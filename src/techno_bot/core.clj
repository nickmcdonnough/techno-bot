(ns techno-bot.core
  (:require [techno-bot.commands.youtube :as yt]
            [techno-bot.commands.weather :as weather]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [clj-http.client :as client]
            [ring.middleware.params :refer (wrap-params)]
            [ring.adapter.jetty :as jetty]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [environ.core :as environ]))

(def slack-post-url (environ/env :slack-api-url))

(defn build-bot-message [ytmap]
  (str "Here you go " (:user ytmap) ": " "<" (:url ytmap) "|" (:title ytmap) ">."))

(defn post-to-slack [message]
  (client/post slack-post-url
               {:body (json/write-str {:channel "#techno-enthusiasts"
                                       :username "technobot"
                                       :icon_emoji ":de:"
                                       :text message})}))

(def user-exec {"youtube" #(->> (assoc (yt/get-youtube-data %2) :user %)
                                (build-bot-message)
                                (post-to-slack))
                "weather" #(post-to-slack (weather/austin) #_%&)})
                ;"what" #(->> (assoc (what/parse %2) :user %))})

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
  (GET "/" [] "technobot")
  (POST "/mks/" [user_name text] (process-incoming-webhook user_name text))
  (route/resources "/")
  (route/resources "/mks/")
  (route/not-found "Not Found"))

(def app (wrap-params app-routes))

(defn -main [] (jetty/run-jetty (wrap-params app-routes) {:port 5000}))

