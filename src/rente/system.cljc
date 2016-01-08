(ns rente.system
  (:require [com.stuartsierra.component :as component]
            [rente.ws                   :as ws       ]
            [rente.server               :as server   ]
            [rente.app                  :as app      ]
            #_[rente.utils.long         :as log      ]))

(def create-system
  (delay
    (identity ;res/register-system!
      ;::system
      ;initial-config
      (fn [config-0]
        (let [{{:keys [threadpool]} :async/config
               {:keys [db]}         :db/config} config-0]
          (component/system-map
            ;:log/config
            ;  (log/->log-initializer (:log/config config-0))
            :db/db
              (component/using
                (db/->db db)
                [:async/threadpool])))))))

(defn initialize-db! []
  ;(log/pr :debug "Initializing database...")
  #_(db/add-schemas! (db/block->schemas schemas)))

#_(defn system [config]
  (component/system-map
   :ws-connection
   (ws/new-ws-connection)
   :http-server
   (component/using (server/new-http-server (:port config)) [:ws-connection])
   :app
   (component/using (app/new-app) [:ws-connection])))


