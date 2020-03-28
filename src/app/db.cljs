(ns app.db
  (:require
   [reagent.core :as r]))

(def genres
  (r/atom {}))

(def favourites
  (r/atom []))

(def filters
  (r/atom #{}))