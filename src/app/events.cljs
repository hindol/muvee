(ns app.events
  (:require
   [app.db :as db]
   [re-frame.core :as rf]))

(def ^:dynamic *account-id*
  "52ec97cd760ee333df011636")

(def ^:dynamic *v3-access-token*
  "d2a4f6f3e0746e65dd641b1a5b40580c")

(def ^:dynamic *v4-access-token*
  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYmYiOjE1ODUzNzA1MDAsInN1YiI6IjUyZWM5N2NkNzYwZWUzMzNkZjAxMTYzNiIsImp0aSI6IjE5MzQ2OTMiLCJhdWQiOiJkMmE0ZjZmM2UwNzQ2ZTY1ZGQ2NDFiMWE1YjQwNTgwYyIsInNjb3BlcyI6WyJhcGlfcmVhZCIsImFwaV93cml0ZSJdLCJ2ZXJzaW9uIjoxfQ.wGwxKwsRHARd1fxB4Yndu-ct0KmBTj07y4h3w1rtLWo")

(rf/reg-event-fx
 :initialize
 (fn [_ _]
   {:db         db/app-db-init
    :dispatch-n [[:fetch-genres] [:fetch-favourite-movies]]}))

(rf/reg-event-fx
 :fetch-genres
 (fn [_ _]
   {:http {:method     :get
           :url        "https://api.themoviedb.org/3/genre/movie/list"
           :opts       {:with-credentials? false
                        :query-params      {:api_key *v3-access-token*}}
           :on-success [:genres-fetched]}}))

(rf/reg-event-fx
 :fetch-favourite-movies
 (fn [_ [_ {:keys [page]}]]
   {:http {:method     :get
           :url        (str "https://api.themoviedb.org/4/account/"
                            *account-id*
                            "/movie/favorites")
           :opts       {:with-credentials? false
                        :query-params      {:page    page
                                            :sort_by "release_date.desc"}
                        :oauth-token       *v4-access-token*}
           :on-success [:favourite-movies-fetched]}}))

(rf/reg-event-fx
 :fetch-director
 (fn [_ [_ movie-id]]
   {:fetch-director {:movie-id movie-id}}))

(rf/reg-event-db
 :genres-fetched
 (fn [db [_ {:keys [genres]}]]
   (assoc db :genres (zipmap (map :id genres) genres))))

(rf/reg-event-fx
 :favourite-movies-fetched
 (fn [{:keys [db]} [_ {:keys [page total_pages results]}]]
   {:db         (update db :favourite-movies merge (zipmap (map :id results) results))
    :dispatch-n (cond-> (for [m results]
                      [:fetch-director (:id m)])
                  (< page total_pages) (conj [:fetch-favourite-movies {:page (inc page)}]))}))

(rf/reg-event-db
 :director-fetched
 (fn [db [_ movie-id director]]
   (assoc-in db [:favourite-movies movie-id :director] director)))

(rf/reg-event-db
 :show-only-genre
 (fn [db [_ genre-id]]
   (update-in db [:show-only :genre-ids] conj genre-id)))

(rf/reg-event-db
 :reset-show-only
 (fn [db _]
   (assoc db :show-only (:show-only db/app-db-init))))