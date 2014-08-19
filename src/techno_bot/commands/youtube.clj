(ns techno-bot.commands.youtube
  (:require [clojure.string :as string]
            [clojure.data.json :as json]
            [clj-http.client :as client]))

(def youtube-search-base "http://gdata.youtube.com/feeds/api/videos?alt=json&q=")

(defn youtube-query-url [search-terms]
  (let [query (string/replace search-terms #"\s" "%20")]
    (str youtube-search-base query)))

(defn get-youtube-data [args]
  (let [response (json/read-json ((client/get (youtube-query-url args)) :body))]
    {:url (-> response :feed :entry first :link first :href)
     :title (-> response :feed :entry first :title :$t)}))
