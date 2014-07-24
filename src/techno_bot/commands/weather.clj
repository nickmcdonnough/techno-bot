(ns techno-bot.commands.weather
  (:require [clojure.string :as string]
            [clojure.data.json :as json]
            [clj-http.client :as client]
            [environ.core :as environ]))

(def api-key (environ/env :wug-api-key))
(def city-lookup-url "http://autocomplete.wunderground.com/aq?query=")

(defn conditions-url [city statecode]
  (str "http://api.wunderground.com/api/" api-key "/conditions/q/" statecode "/" city ".json"))

(defn get-austin-weather []
  (let [response ((client/get (conditions-url "Austin" "TX")) :body)
        current-weather ((json/read-json response) :current_observation)]
    (assoc {} :temperature (:temperature_string current-weather)
              :wind-type (:wind_string current-weather)
              :wind-direction (:wind_dir current-weather)
              :wind-speed (:wind_mph current-weather)
              :humidity (:relative_humidity current-weather))))

(defn austin-weather-string [weather-map]
  (str "The current temperature in Austin is " (:temperature weather-map) " "
       "with " (:humidity weather-map) " humidity. "
       (if (not= (:wind-type weather-map) "Calm")
         (str "There is a " (:wind-type weather-map) " wind blowing at " (:wind-speed weather-map)
              " from the " (:wind-direction weather-map) "."))))

(defn austin []
  (austin-weather-string (get-austin-weather)))
