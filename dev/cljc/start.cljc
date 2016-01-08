(ns rente.start
  (:require #?(:cljs [figwheel.client :as fw])
            #_[rente.client.app :as app]))

#?(:cljs (enable-console-print!))

;(app/-main)
#?(:cljs (println "Figwheel loaded!"))