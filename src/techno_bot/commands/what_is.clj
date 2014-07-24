(ns techno-bot.commands.what_is.clj
  (:require [clojure.string :as string]))


(def techno-wiki-url "http://en.wikipedia.org/wiki/Techno")

(defn parse [user-input]
  (string/trim (last (string/split user-input #"^is"))))
