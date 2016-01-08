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
      ; Maintain DB history.
      (let [history (when (pos? history-limit) (atom []))
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

(defrecord ^{:doc "Datomic"}
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