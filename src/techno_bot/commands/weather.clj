(ns techno-bot.commands.weather
  (:require [clojure.string :as string]
            [clojure.data.json :as json]
            [clj-http.client :as client]
            [environ.core :as environ]))

(def api-key (environ/env :wug-api-key))
(def city-lookup-url "http://autocomplete.wunderground.com/aq?query=")

(defn conditions-url [city statecode]
  (str "http://api.wunderground.com/api/" api-key "/conditions/q/" statecode "/" city ".json"))

(defn make-api-call [city state]
  (let [response ((client/get (conditions-url city state)) :body)
        {:keys [current_observation]} (json/read-json response)
        {:keys [temperature_string relative_humidity wind_string wind_mph wind_dir]} current_observation]
    {:temperature temperature_string
     :humidity relative_humidity
     :wind-type wind_string
     :wind-speed wind_mph
     :wind-direction wind_dir}))

(defn build-weather-string [city {:keys [temperature humidity wind-type
                                         wind-speed wind-direction]}]
  (str "The current temperature in " city " is " temperature " "
       "with " humidity " humidity. "
       (when (not= wind-type "Calm")
         (str "The wind is coming " (string/replace wind-type "F" "f")))))

(defn get-current-conditions [city state]
  (build-weather-string city (make-api-call city state)))
