(ns rente.run
  (:gen-class)
  (:require [clojure.tools.logging 	    :as log       	  	   ]
            [com.stuartsierra.component :as component 	  	   ]
            [rente.config 		   	    :as config    	  	   ]
            [rente.system 		   	    :as sys :refer [system]]
            [rente.utils.resources      :as res]))

(defn ^:export -main []
  @sys/create-system
  ;(res/reload! (:rente.system/system @res/systems))
  
  ;#?(:clj  (reset! db/conn* (-> @sys/sys-map :db/db :backend   :conn))
  ;   :cljs (reset! db/conn* (-> @sys/sys-map :db/db :ephemeral :conn)))
  (sys/initialize-db!)
  ;(posh.core/posh! @db/conn*)

  #_(rx/render [todo/todo-app]
    (js/document.getElementById "app")))