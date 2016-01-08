(ns rente.db
  (:require [com.stuartsierra.component               :as component]
   #?(:clj  [datomic.api                              :as db       ]
      :cljs [datomic-cljs.api                         :as db       ])
            [datascript.core                          :as mdb      ]))

(defonce conn (atom nil))

(defrecord
  ^{:doc "Ephemeral (in-memory) database. Currently implemented as
          DataScript database. Once the reference to @conn is lost,
          the database is garbage-collected.

          @conn, while also a 'connection', is really an atom with the current DataScript DB value."}
  EphemeralDatabase [conn history history-limit reactive?]
  component/Lifecycle
    (start [this]
      (log/pr :user "Starting Ephemeral database")
      (let [; Maintain DB history.
            history (when (pos? history-limit) (atom []))
            _ (when (pos? history-limit)
                (mdb/listen! conn :history
                  (fn [tx-report] (swap! history c/conj tx-report))))
            conn-f (mdb/create-conn)
            ; Sets up the tx-report listener for a conn
            #?@(:cljs [_ (when reactive? (rx-mdb/posh! conn-f))])
            _ (log/pr :user "Ephemeral database reactivity set up.")]
        (c/assoc this :conn    conn-f
                      :history history)))
    (stop [this]
      (reset! conn nil) ; TODO is this wise?
      this))

(defrecord ^{:doc "Can be one of three things:
                   1) A direct connection to a Datomic database using the Datomic Peer API
                      - This option is for Clojure (e.g. server) only, not ClojureScript
                   2) A direct connection to a Datomic database using the Datomic HTTP API
                      - This option is currently not proven to be secure and is awaiting
                        further developments by the Cognitect team.
                   3) A REST endpoint pair:
                      - One for pushing, e.g. 'POST /db'
                      - One for pulling, e.g. 'GET  /db'
                      - This way the Datomic database is not directly exposed to the client,
                        but rather the server is able to use access control and other
                        security measures when handling queries from the client.
                        This is the (currently) recommended option."}
  BackendDatabase [type name host port rest-port uri conn txr-alias create-if-not-present?]
  component/Lifecycle
    (start [this]
      ; Set all transactor logs to WARN 
      #?(:clj (doseq [^ch.qos.logback.classic.Logger logger
                        (->> (ch.qos.logback.classic.util.ContextSelectorStaticBinder/getSingleton)
                             (.getContextSelector)
                             (.getLoggerContext)
                             (.getLoggerList))]
                (.setLevel logger ch.qos.logback.classic.Level/WARN)))
      (let [uri-f (condp = type
                            :free
                              (str "datomic:" (c/name type)
                                   "://" host ":" port "/" name)
                            :mem
                              (str "datomic:" (c/name type)
                                   "://" name)
                            :http
                              (str "http://" host ":" rest-port "/" txr-alias "/" name)
                            (throw (->ex :illegal-argument
                                         "Database type not supported"
                                         type)))
            connect (fn [] #?(:clj  (db/connect uri-f)
                              :cljs (db/connect host rest-port txr-alias name)))
            conn-f  (try (connect)
                      (catch #?(:clj RuntimeException :cljs js/Error) e
                        (when (and #?(:clj
                                       (-> e .getMessage
                                           (=  (str "Could not find " name " in catalog")))
                                      :cljs "TODO")
                                   create-if-not-present?)
                          (do (log/pr :warn "Creating database...")
                              #?(:clj  (Peer/createDatabase uri-f)
                                 :cljs (go (<? (db/create-database host rest-port txr-alias name)))))
                          (connect))))]
      (reset! conn conn-f)
      (c/assoc this :uri uri-f)))
    (stop [this]
      #?(:clj (db/release @conn))
      (reset! conn nil)
      this))

(defrecord
  ^{:doc "Database-system consisting of an EphemeralDatabase (e.g. DataScript),
          BackendDatabase (e.g. Datomic), and a reconciler which constantly
          pushes diffs from the EphemeralDatabase to the BackendDatabase
          and pulls new data from the BackendDatabase.

          A Datomic subscription model would be really nice for performance
          (ostensibly) to avoid the constant backend polling of the reconciler,
          but unfortunately Datomic does not have this.

          @backend: 
            See BackendDatabase
            
          @reconciler
            Doesn't currently exist

          "}
  Database
  [ephemeral reconciler backend]
  ; TODO code pattern here
  component/Lifecycle
    (start [this]
      (let [ephemeral-f  (when ephemeral  (component/start ephemeral ))
            backend-f    (when backend    (component/start backend   ))
            reconciler-f (when reconciler (component/start reconciler))]
        (c/assoc this
          :ephemeral  ephemeral-f
          :reconciler reconciler-f
          :backend    backend-f)))
    (stop [this]
      (let [reconciler-f (when reconciler (component/stop reconciler))
            ephemeral-f  (when ephemeral  (component/stop ephemeral ))
            backend-f    (when backend    (component/stop backend   ))]
        (c/assoc this
          :ephemeral  ephemeral-f
          :reconciler reconciler-f
          :backend    backend-f))))

(defn ->db
  "Constructor for |Database|."
  {:usage '(->db {:backend {}
                  :reconciler {}
                  :ephemeral {}})}
  [{{:keys [type name host port rest-port txr-alias create-if-not-present?] :as backend}
    :backend
    {:keys [] :as reconciler}
    :reconciler
    {:keys [history-limit] :as ephemeral}
    :ephemeral
    :as config}]
  (log/pr :user (kmap config))
  (when backend
    (err/assert (contains? #{:free :http} type)) ; TODO for now
    (err/assert ((fn-and string? nempty?) name))
    (err/assert ((fn-and string? nempty?) host))
    (err/assert (integer? port))
    (err/assert ((fn-or nil? integer?) port))
    (err/assert ((fn-or nil? string?)  txr-alias))
    (err/assert ((fn-or nil? boolean?) create-if-not-present?)))

  (when ephemeral
    (err/assert ((fn-or nil? integer?) history-limit)))

  (Database.
    (when ephemeral
      (map->EphemeralDatabase
        (c/assoc ephemeral :history-limit (or history-limit 0))))
    reconciler
    (when backend
      (map->BackendDatabase 
        (c/assoc backend :uri  (atom nil)
                         :conn (atom nil))))))