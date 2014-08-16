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

(defn build-bot-message [{:keys [user url title]}]
  (str "Here you go " user ": " "<" url "|" title ">."))

(defn post-to-slack [message]
  (client/post slack-post-url
               {:body (json/write-str {:channel "#techno-enthusiasts"
                                       :username "technobot"
                                       :icon_emoji ":de:"
                                       :text message})}))

(def user-exec {"youtube" (fn [user search-terms]
                            (-> search-terms
                                yt/get-youtube-data
                                (assoc :user user)
                                build-bot-message
                                post-to-slack))
                "weather" (fn [& _] (post-to-slack (weather/austin)))})
                ;"what" #(-> %2 what/parse (assoc :user %1))})

(defn exec-user-command [{:keys [user text]}]
  (let [[_ command & search-terms] (string/split text #"\s")]
    ((user-exec command) user search-terms)))

(defn process-incoming-webhook [username text]
  (exec-user-command {:user username :text text}))

(defroutes app-routes
  (GET "/" [] "technobot")
  (POST "/mks/" [user_name text] (process-incoming-webhook user_name text))
  (route/resources "/")
  (route/resources "/mks/")
  (route/not-found "Not Found"))

(def app (wrap-params app-routes))

(defn -main [] (jetty/run-jetty (wrap-params #'app-routes) {:port 5000}))
