(ns techno-bot.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.json :refer :all]
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

; use thread first here for json -> client -> youtube-query-url
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

(defn hello-world-hook [hook]
  (println (hook "key"))
  (println (type hook)))

(def user-exec {"youtube" (fn [user args]
                            (let [yt-response (get-youtube-data args)
                                  url (yt-response :url)
                                  title (yt-response :title)
                                  message (build-bot-message user url title)]
                              (post-to-slack message)))})

(defn exec-user-command [mymap]
  (let [text (mymap "text")
        user (mymap "user_name")
        string-vec (string/split text #"\+")
        command (nth string-vec 1)
        search-terms (nthrest string-vec 2)]
    ((user-exec command) user search-terms)))
; add user back to top user-exec call

(defn parse-user-text [hook]
  (exec-user-command (apply hash-map (flatten (map #(clojure.string/split % #"=") (clojure.string/split hook #"&"))))))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (POST "/mks/" {params :body} (parse-user-text (slurp params)))
  (POST "/hello-world/" {params :body} (hello-world-hook params))
  (route/resources "/")
  (route/resources "/mks/")
  (route/not-found "Not Found"))

(def app
  (-> (handler/site app-routes)
      wrap-json-body))

(defn -main [] (jetty/run-jetty app-routes {:port 5000}))
