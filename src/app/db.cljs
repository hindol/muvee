(ns app.db)

(def app-db-init
  {:favourite-movies {}
   :genres           {}
   :directors        {}
   :show-only        {:genre-ids    #{}
                      :director-ids #{}}})
