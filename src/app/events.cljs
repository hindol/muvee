(ns app.events
  (:require
   [app.db :as db]
   [re-frame.core :as rf]))

(rf/reg-event-fx
 :initialize
 (fn [_ _]
   {:db         db/app-db-init
    :dispatch-n [[:fetch-genres] [:fetch-favourite-movies]]}))

(rf/reg-event-fx
 :fetch-genres
 (fn [_ _]
   {:fetch-genres nil}))

(rf/reg-event-fx
 :fetch-favourite-movies
 (fn [_ [_ {:keys [page]}]]
   {:fetch-favourite-movies {:page (or page 1)}}))

(rf/reg-event-fx
 :fetch-director
 (fn [_ [_ movie-id]]
   {:fetch-director {:movie-id movie-id}}))

(rf/reg-event-db
 :genres-fetched
 (fn [db [_ genres]]
   (assoc db :genres (zipmap (map :id genres) genres))))

(rf/reg-event-fx
 :favourite-movies-fetched
 (fn [{:keys [db]} [_ page]]
   {:db (update db :favourite-movies merge (zipmap (map :id page) page))
    :dispatch-n (for [m page]
                  [:fetch-director (:id m)])}))

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