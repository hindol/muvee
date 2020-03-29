(ns app.main
  (:require
   [app.db :as db]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [re-frame.core :as rf])
  (:import
   [goog.date UtcDateTime]))

(defn info
  [text]
  [:div.alert.alert-info {:role "alert"} text])

(defn poster
  [movie]
  (when (:poster_path movie)
    (let [poster-url (str "https://image.tmdb.org/t/p/w200"
                          (:poster_path movie))]
      [:img.img-fluid.img-thumbnail
       {:src   poster-url
        :alt   "Poster"
        :style {:object-fit "contain"}}])))

(defn title
  [movie]
  [:h4 (:title movie)
   (when (not= (:original_language movie) "en")
     [:small.text-muted.ml-2 (str "(" (:original_title movie) ")")])])

(defn year
  [movie]
  [:span.text-muted.mr-2
   (.getYear (UtcDateTime/fromIsoString (:release_date movie)))])

(defn director
  [movie]
  (let [director (rf/subscribe [:director (:id movie)])]
    (rf/dispatch [:fetch-director (:id movie)])
    (fn []
      (when @director
        [:span.text-muted.ml-2.mr-2 (:name @director)]))))

(defn genre
  [{:keys [genre-id filters on-genre-filter]}]
  [:a {:class    (when (contains? filters genre-id) "text-muted")
       :href     "#"
       :on-click #(on-genre-filter genre-id)}
   (get @(rf/subscribe [:genres]) genre-id)])

(defn genre-list
  [{:keys [genre-ids filters on-genre-filter on-reset-filter]}]
  (let [genres (rf/subscribe [:genres])]
    (when-not (empty? @genres)
      [:span.ml-2
       (doall
        (interpose ", " (for [g genre-ids]
                          ^{:key g}
                          [genre {:genre-id        g
                                  :filters         filters
                                  :on-genre-filter on-genre-filter}])))
       (when-not (empty? filters)
         [:a.ml-2.text-muted
          {:href     "#"
           :on-click on-reset-filter}
          "↻"])])))

(defn overview
  [movie]
  [:p (:overview movie)])

(defn movie-card
  [{:keys [movie filters on-genre-filter on-reset-filter]}]
  [:div.row.mt-2.mr-2.mb-2.ml-2
   [:div.col-4.col-sm-3
    [poster movie]]
   [:div.col
    [:div.row
     [:div.col
      [title movie]]]
    [:div.row
     [:div.col
      [:p
       [year movie] "•"
       [director movie] "•"
       [genre-list {:genre-ids       (:genre_ids movie)
                    :filters         filters
                    :on-genre-filter on-genre-filter
                    :on-reset-filter on-reset-filter}]]]]
    [:div.row
     [:div.col
      [overview movie]]]]])

(defn movie-jumbotron
  [props]
  [:div.jumbotron
   [movie-card props]])

(defn movie-list
  []
  (let [favourites (rf/subscribe [:favourite-movies])
        filters    (r/atom #{})]
    (rf/dispatch [:fetch-favourite-movies])
    (fn []
      (when (seq @favourites)
        (let [r (rand-nth (keys @favourites))]
          [:div.row
           [:div.col
            (doall
             (cons ^{:key r} [movie-jumbotron {:movie           (@favourites r)
                                               :filters         @filters
                                               :on-genre-filter #(swap! filters conj %)
                                               :on-reset-filter #(reset! filters #{})}]
                   (for [m     (vals (dissoc @favourites r))
                         :when (or (empty? @filters)
                                   (every? (set (:genre_ids m)) @filters))]
                     ^{:key (:id m)}
                     [movie-card {:movie           m
                                  :filters         @filters
                                  :on-genre-filter #(swap! filters conj %)
                                  :on-reset-filter #(reset! filters #{})}])))]])))))

(defn app
  []
  [:div.container-lg.pt-2.pb-2
   [info "Click on a genre to restrict to that genre. Click on multiple genres to filter further."]
   [movie-list]])

(defn render
  []
  (rdom/render [app] (.getElementById js/document "app")))

(defn ^:dev/after-load clear-cache-and-render!
  []
  (rf/clear-subscription-cache!)
  (render))

(defn ^:export init
  []
  (rf/dispatch-sync [:initialize])
  (rf/dispatch [:fetch-genres])
  (render))
