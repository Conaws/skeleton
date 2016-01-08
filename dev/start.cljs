(ns rente.start
  (:require [figwheel.client :as fw]
            [rente.client.app :as app]))

(enable-console-print!)

(app/-main)
