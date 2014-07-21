(defproject techno-bot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[ring-server "0.3.1"]
                 [ring/ring-json "0.3.1"]
                 [org.clojure/clojure "1.6.0"]
                 [compojure "1.1.8"]
                 [clj-http "0.9.2"]
                 [org.clojure/data.json "0.2.5"]
                 [environ "0.5.0"]]
  :plugins [[lein-ring "0.8.11"]]
  :ring {:handler techno-bot.core/app}
  :main techno-bot.core
  :uberjar-name "technobot.jar"
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
