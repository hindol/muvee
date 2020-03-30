(ns app.effects
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

(rf/reg-fx
 :fetch-genres
 (fn [_]
   (go
     (let [response (<! (http/get "https://api.themoviedb.org/3/genre/movie/list"
                                  {:with-credentials? false
                                   :query-params      {:api_key *v3-access-token*}}))]
       (rf/dispatch [:genres-fetched (get-in response [:body :genres])])))))

(rf/reg-fx
 :fetch-favourite-movies
 (fn [{:keys [page]}]
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
         (rf/dispatch [:fetch-favourite-movies {:page (inc page)}]))))))

(rf/reg-fx
 :fetch-director
 (fn [{:keys [movie-id]}]
   (go
     (let [response (<! (http/get (str "https://api.themoviedb.org/3/movie/"
                                       movie-id
                                       "/credits")
                                  {:with-credentials? false
                                   :query-params      {:api_key *v3-access-token*}}))
           crew     (get-in response [:body :crew])
           director (first (filter #(-> % :job (= "Director")) crew))]
       (rf/dispatch [:director-fetched movie-id director])))))