(ns app.db
  (:require
   [reagent.core :as r]))

(def genres
  (r/atom {}))