(ns techno-bot.core
  (:require [techno-bot.commands.youtube :as yt]
            [techno-bot.commands.weather :as weather]
            [techno-bot.commands.boteval :as beval]
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

(defmulti do-command (fn [command _ _] command))

(defmethod do-command :default [command user _]
  (post-to-slack (str "Sorry, " user ", I don't know about " command ".")))

(defmethod do-command "youtube" [_ user search-terms]
  (-> search-terms
      yt/get-youtube-data
      (assoc :user user)
      build-bot-message
      post-to-slack))

(defmethod do-command "weather" [_ user city]
  (if (= city "sf")
    (post-to-slack (weather/get-current-conditions "San Fransisco" "CA"))
    (post-to-slack (weather/get-current-conditions "Austin" "TX"))))

(defmethod do-command "eval" [_ user exp]
  (let [result (beval/evaluate (string/join " " exp))]
    (if result
      (post-to-slack (str user ", your expression evaluated to: " (print-str result)))
      (post-to-slack (str user ", I could not evaluate that expression.")))))

(defn exec-user-command [{:keys [user text]}]
  (let [[_ command & search-terms] (string/split text #"\s")]
    (do-command command user search-terms)))

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
