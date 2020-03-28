(ns app.main
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [app.db :as db]
   [cljs-http.client :as http]
   [cljs.core.async :refer [<!]]
   [reagent.dom :as rdom])
  (:import
   [goog.date UtcDateTime]))

(def ^:dynamic *account-id*
  "52ec97cd760ee333df011636")

(def ^:dynamic *v3-access-token*
  "d2a4f6f3e0746e65dd641b1a5b40580c")

(def ^:dynamic *v4-access-token*
  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYmYiOjE1ODUzNzA1MDAsInN1YiI6IjUyZWM5N2NkNzYwZWUzMzNkZjAxMTYzNiIsImp0aSI6IjE5MzQ2OTMiLCJhdWQiOiJkMmE0ZjZmM2UwNzQ2ZTY1ZGQ2NDFiMWE1YjQwNTgwYyIsInNjb3BlcyI6WyJhcGlfcmVhZCIsImFwaV93cml0ZSJdLCJ2ZXJzaW9uIjoxfQ.wGwxKwsRHARd1fxB4Yndu-ct0KmBTj07y4h3w1rtLWo")

(defn fetch-genres
  []
  (go
    (let [response (<! (http/get "https://api.themoviedb.org/3/genre/movie/list"
                                 {:with-credentials? false
                                  :query-params      {:api_key *v3-access-token*}}))]
      (swap! db/genres into (map (juxt :id :name)
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
       (swap! db/favourites into (get-in response [:body :results]))
       (when (< page (get-in response [:body :total_pages]))
         (fetch-favourites (inc page)))))))

(defn poster
  [props movie]
  (let [poster-url (str "https://image.tmdb.org/t/p/w200"
                        (:poster_path movie))]
    [:img.img-fluid.img-thumbnail
     (merge {:src poster-url
             :alt "Poster"}
            {:style {:object-fit       "contain"
                     :max-height       "200px"
                     :background-image (str "url(\"" poster-url "\")")
                     :background-size  "cover"}}
            props)]))

(defn title
  [movie]
  [:h4 (:title movie)
   (when (not= (:original_language movie) "en")
     [:small.text-muted.ml-2 (str "(" (:original_title movie) ")")])])

(defn year
  [movie]
  [:span.text-muted.mr-2
   (.getYear (UtcDateTime/fromIsoString (:release_date movie)))])

(defn genre
  [{:keys [genre-id filters]}]
  [:a {:class    (when (contains? filters genre-id) "text-muted")
       :href     "#"
       :on-click #(swap! db/filters conj genre-id)}
   (get @db/genres genre-id)])

(defn genre-list
  [{:keys [genre-ids filters]}]
  (when-not (empty? @db/genres)
    [:span.ml-2
     (doall
      (interpose ", " (for [g genre-ids]
                        ^{:key g}
                        [genre {:genre-id g
                                :filters  filters}])))
     (when-not (empty? filters)
       [:a.ml-2.text-muted
        {:href     "#"
         :on-click #(reset! db/filters #{})}
        "↻"])]))

(defn overview
  [movie]
  [:p (:overview movie)])

(defn movie-card
  [{:keys [movie filters]}]
  [:div.row.mt-2.mr-2.mb-2.ml-2
   [poster {:class "col-4 col-sm-3"} movie]
   [:div.col
    [:div.row
     [:div.col
      [title movie]]]
    [:div.row
     [:div.col
      [:p [year movie] "•" [genre-list {:genre-ids (:genre_ids movie)
                                        :filters   filters}]]]]
    [:div.row
     [:div.col
      [overview movie]]]]])

(defn movie-list
  [{:keys [movies filters]}]
  [:div.row
   [:div.col
    (doall
     (for [m     movies
           :when (or (empty? filters)
                     (every? (set (:genre_ids m)) filters))]
       ^{:key (:id m)}
       [movie-card {:movie   m
                    :filters filters}]))]])

(defn app
  []
  [:div.container-lg.pt-2.pb-2
   [movie-list {:movies  @db/favourites
                :filters @db/filters}]])

(defn ^:dev/after-load start
  []
  (rdom/render [app] (.getElementById js/document "app"))
  (when (empty? @db/favourites)
    (fetch-favourites))
  (when (empty? @db/genres)
    (fetch-genres)))

(defn ^:export init
  []
  (start))
