(ns rente.app
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [rente.ws :as ws]))

(defrecord App [ws-connection]
  component/Lifec ycle
  (st art [component]
    (log/debug "Application logi c started")
    compon ent)
  (stop  [component]
    (log/debug "Application logi c stopped")
    component))

(defn new-app []
  (map->App {}))
