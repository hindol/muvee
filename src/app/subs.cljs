(ns app.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :genres
 (fn
   ([db [_ genre-id]]
    (if genre-id
      (get-in db [:genres genre-id])
      (:genres db)))))

(rf/reg-sub
 :favourite-movies
 (fn [db _]
   (:favourite-movies db)))

(rf/reg-sub
 :favourite-movies-filtered
 :<- [:favourite-movies]
 :<- [:show-only]
 (fn [[favourite-movies filters] _]
   (sort-by :release_date >
            (cond->> (vals favourite-movies)
              (seq filters) (filterv #(every? (set (:genre_ids %))
                                              (:genre-ids filters)))))))

(rf/reg-sub
 :director
 (fn [db [_ movie-id]]
   (get-in db [:favourite-movies movie-id :director])))

(rf/reg-sub
 :show-only
 (fn [db _]
   (:show-only db)))