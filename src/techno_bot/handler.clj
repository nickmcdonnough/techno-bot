(ns techno-bot.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.params :refer (wrap-params)]
            [ring.adapter.jetty :as jetty]
            [clj-http.client :as client]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [environ.core :as environ]))

(def slack-post-url (environ/env :slack-api-url))
(def youtube-search-base "http://gdata.youtube.com/feeds/api/videos?alt=json&q=")

(defn youtube-query-url [search-terms]
  (let [query (string/replace search-terms #"\s" "%20")]
    (str youtube-search-base query)))

(defn get-youtube-data [args]
  (let [response (json/read-json ((client/get (youtube-query-url args)) :body))]
    (assoc {} :url (-> response :feed :entry first :link first :href)
           :title (-> response :feed :entry first :title :$t))))

(defn build-bot-message [user url link-text]
  (let [link (str "<" url "|" link-text ">.")]
  (str "Here you go " user ": " link)))

(defn post-to-slack [message]
  (client/post slack-post-url
               {:body (json/write-str {:channel "#techno-enthusiasts"
                                       :username "techno-bot"
                                       :icon_emoji ":de:"
                                       :text message})}))

(defn youtube [user args]
  (let [yt-response (get-youtube-data args)]
    (build-bot-message
      user
      (yt-response :url)
      (yt-response :title))))

(def user-exec {"youtube" #(post-to-slack (youtube %1 %2))})

(defn exec-user-command [mymap]
  (let [text (mymap :text)
        user (mymap :user)
        string-vec (string/split text #"\s")
        command (nth string-vec 1)
        search-terms (nthrest string-vec 2)]
    ((user-exec command) user search-terms)))

(defn process-incoming-webhook [username text]
  (-> (assoc {} :user username :text text)
      exec-user-command))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (POST "/mks/" [user_name text] (process-incoming-webhook user_name text))
  (route/resources "/")
  (route/resources "/mks/")
  (route/not-found "Not Found"))

(def app (wrap-params app-routes))

(defn -main [] (jetty/run-jetty (wrap-params app-routes) {:port 5000}))
