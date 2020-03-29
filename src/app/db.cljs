(ns app.db
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs-http.client :as http]
   [cljs.core.async :refer [<!]]
   [re-frame.core :as rf]))

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
      (rf/dispatch [:genres-fetched (map (juxt :id :name)
                                         (get-in response [:body :genres]))]))))

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
       (rf/dispatch [:favourite-movies-fetched (map (juxt :id identity)
                                                    (get-in response [:body :results]))])
       (when (< page (get-in response [:body :total_pages]))
         (fetch-favourites (inc page)))))))

(defn fetch-director
  [movie-id]
  (go
    (let [response (<! (http/get (str "https://api.themoviedb.org/3/movie/"
                                      movie-id
                                      "/credits")
                                 {:with-credentials? false
                                  :query-params      {:api_key *v3-access-token*}}))
          crew     (get-in response [:body :crew])
          director (first (filter #(-> % :job (= "Director")) crew))]
      (rf/dispatch [:director-fetched movie-id director]))))

(rf/reg-event-db
 :initialize
 (fn [_ _]
   {:favourite-movies {}
    :genres           {}
    :show-only        {:genre-ids    #{}
                       :director-ids #{}}}))

(rf/reg-event-db
 :fetch-genres
 (fn [db _]
   (fetch-genres)
   db))

(rf/reg-event-db
 :fetch-favourite-movies
 (fn [db _]
   (fetch-favourites)
   db))

(rf/reg-event-db
 :fetch-director
 (fn [db [_ movie-id]]
   (fetch-director movie-id)
   db))

(rf/reg-event-db
 :genres-fetched
 (fn [db [_ genres]]
   (update db :genres into genres)))

(rf/reg-event-db
 :favourite-movies-fetched
 (fn [db [_ page]]
   (update db :favourite-movies into page)))

(rf/reg-event-db
 :director-fetched
 (fn [db [_ movie-id director]]
   (assoc-in db [:favourite-movies movie-id :director] director)))

(rf/reg-sub
 :genres
 (fn [db _]
   (:genres db)))

(rf/reg-sub
 :favourite-movies
 (fn [db _]
   (:favourite-movies db)))

(rf/reg-sub
 :director
 (fn [db [_ movie-id]]
   (get-in db [:favourite-movies movie-id :director])))
