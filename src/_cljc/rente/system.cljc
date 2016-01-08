(ns rente.system
  (:require [com.stuartsierra.component :as component]
            [rente.ws                   :as ws       ]
            [rente.server               :as server   ]
            [rente.config               :as config   ]
            #_[rente.utils.log          :as log      ]))

(def create-system
  (delay
    (identity ;res/register-system!
      ;::system
      config/initial-config
      (fn [config-0]
        (let [{{:keys [db]} :db/config} config-0]
          (component/system-map
            ;:log/config
            ;  (log/->log-initializer (:log/config config-0))
            :ws-connection
              (ws/new-ws-connection)
            :http-server
              (component/using
                (server/new-http-server (:port config-0))
                [:ws-connection])
            :db/db
              (db/->db db)))))))

(defn initialize-db! []
  ;(log/pr :debug "Initializing database...")
  #_(db/add-schemas! (db/block->schemas schemas)))