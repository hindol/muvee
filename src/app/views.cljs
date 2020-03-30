(ns app.views
  (:require
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
  [genre-id]
  (let [filters (rf/subscribe [:show-only])]
    [:a {:class    (when (contains? (:genre-ids @filters) genre-id) "text-muted")
         :href     "#"
         :on-click #(rf/dispatch [:show-only-genre genre-id])}
     (get @(rf/subscribe [:genres genre-id]) :name)]))

(defn genre-list
  [genre-ids]
  (let [genres  (rf/subscribe [:genres])
        filters (rf/subscribe [:show-only])]
    (when-not (empty? @genres)
      [:span.ml-2
       (doall
        (interpose ", " (for [g genre-ids]
                          ^{:key g}
                          [genre g])))
       (when-not (every? empty? ((juxt :genre-ids :director-ids) @filters))
         [:a.ml-2
          {:href     "#"
           :on-click #(rf/dispatch [:reset-show-only])}
          "↻"])])))

(defn overview
  [movie]
  [:p (:overview movie)])

(defn movie-card
  [{:keys [class]} movie]
  [:div {:class class}
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
        [genre-list (:genre_ids movie)]]]]
     [:div.row
      [:div.col
       [overview movie]]]]]])

(defn movie-jumbotron
  [movie]
  [movie-card {:class ["col" "jumbotron"]} movie])

(defn movie-list
  []
  (let [favourite-movies (rf/subscribe [:favourite-movies-filtered])]
    (when (seq @favourite-movies)
      (let [spotlight (rand-nth @favourite-movies)]
        [:div.row
         [:div.col
          [:div.row
           [movie-jumbotron spotlight]]
          [:div.row
           (for [m (remove #{spotlight} @favourite-movies)]
             ^{:key (:id m)}
             [movie-card {:class "col-lg-6"} m])]]]))))

(defn ui
  []
  [:div.container-lg.pt-2.pb-2
   [:div.row
    [:div.col
     [info "Click on a genre to restrict to that genre. Click on multiple genres to filter further."]]]
   [movie-list]])