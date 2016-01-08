(ns rente.config
  (:require [environ.core :refer [env]]))

(def initial-config
  {;:port (Integer/parseInt (or (env :port) "8080"))
   :log/config
    {:levels #{:debug}}
   :db/config
    {:db
      {:backend
         {:type                   :free
          :name                   "test"
          :host                   "localhost"
          :port                   4334
          :create-if-not-present? true}
       :ephemeral {:reactive? true}}}})
