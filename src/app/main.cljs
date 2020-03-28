(ns app.main
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs-http.client :as http]
   [cljs.core.async :refer [<!]]
   [clojure.string :as str]
   [reagent.core :as r]
   [reagent.dom :as rdom]))

(def ^:dynamic *account-id*
  "52ec97cd760ee333df011636")

(def ^:dynamic *v3-access-token*
  "d2a4f6f3e0746e65dd641b1a5b40580c")

(def ^:dynamic *v4-access-token*
  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYmYiOjE1ODUzNzA1MDAsInN1YiI6IjUyZWM5N2NkNzYwZWUzMzNkZjAxMTYzNiIsImp0aSI6IjE5MzQ2OTMiLCJhdWQiOiJkMmE0ZjZmM2UwNzQ2ZTY1ZGQ2NDFiMWE1YjQwNTgwYyIsInNjb3BlcyI6WyJhcGlfcmVhZCIsImFwaV93cml0ZSJdLCJ2ZXJzaW9uIjoxfQ.wGwxKwsRHARd1fxB4Yndu-ct0KmBTj07y4h3w1rtLWo")

(def genres
  (r/atom {}))

(def favourites
  (r/atom []))

(defn fetch-genres
  []
  (go
    (let [response (<! (http/get "https://api.themoviedb.org/3/genre/movie/list"
                                 {:with-credentials? false
                                  :query-params      {:api_key *v3-access-token*}}))]
      (swap! genres into (map (juxt :id :name)
                              (get-in response [:body :genres]))))))

(defn fetch-favourites
  ([] (fetch-favourites 1))
  ([page]
   (go
     (let [response (<! (http/get (str "https://api.themoviedb.org/4/account/"
                                       *account-id*
                                       "/movie/favorites")
                                  {:with-credentials? false
                                   :query-params      {:page    page
                                                       :sort_by "release_date.desc"}
                                   :oauth-token       *v4-access-token*}))]
       (swap! favourites into (get-in response [:body :results]))
       (when (< page (get-in response [:body :total_pages]))
         (fetch-favourites (inc page)))))))

(defn poster
  [props m]
  [:img.img-fluid.img-thumbnail
   (merge {:src (str "https://image.tmdb.org/t/p/w200"
                     (:poster_path m))
           :alt "Poster"}
          {:style {:object-fit "contain"
                   :width      "100%"
                   :height     "200px"}}
          props)])

(defn title
  [m]
  [:h4 (:title m)
   (when (not= (:original_language m) "en")
     [:small.text-muted.ml-2 (str "(" (:original_title m) ")")])])

(defn genre
  [m]
  (when-not (empty? @genres)
    [:p.text-muted (str/join ", " (map @genres (:genre_ids m)))]))

(defn overview
  [m]
  [:p (:overview m)])

(defn movie
  [m]
  [:div.row.mt-2.mr-2.mb-2.ml-2
   [poster {:class "col-3"} m]
   [:div.col
    [:div.row
     [:div.col
      [title m]]]
    [:div.row
     [:div.col
      [genre m]]]
    [:div.row
     [:div.col
      [overview m]]]]])

(defn movies
  []
  [:div.row
   [:div.col
    (for [f @favourites]
      ^{:key (:id f)}
      [movie f])]])

(defn app
  []
  [:div.container-lg.pt-2.pb-2
   [movies]])

(defn ^:dev/after-load start
  []
  (rdom/render [app] (.getElementById js/document "app"))
  (fetch-favourites)
  (fetch-genres))

(defn ^:export init
  []
  (start))
