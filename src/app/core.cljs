(ns app.core
  (:require
   [app.db]
   [app.effects]
   [app.events]
   [app.subs]
   [app.views]
   [reagent.dom :as rdom]
   [re-frame.core :as rf]))

(defn render
  []
  (rdom/render [app.views/ui] (.getElementById js/document "app")))

(defn ^:dev/after-load clear-cache-and-render!
  []
  (rf/clear-subscription-cache!)
  (render))

(defn ^:export init
  []
  (rf/dispatch-sync [:initialize])
  (render))
