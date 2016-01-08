(ns rente.run
  (:gen-class)
  (:require [clojure.tools.logging 	    :as log       	  	   ]
            [com.stuartsierra.component :as component 	  	   ]
            [rente.config 		   	    :as config    	  	   ]
            [rente.system 		   	    :as sys :refer [system]]
            [rente.utils.resources      :as res]))

#_(defn -main [& args]
  (let [config (config/get-config)]
    (component/start (system config))
    (log/info "rente started")))

(defn ^:export -main []
  @sys/create-system
  (res/reload! (:rente.system/system @res/systems))
  
  (reset! db/conn* (-> @sys-map :db/db :ephemeral :conn))
  (initialize-db!)
  (posh.core/posh! @db/conn*)
  (todo/init!)

  (rx/render [todo/todo-app]
    (js/document.getElementById "app")))