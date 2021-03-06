(ns techno-bot.commands.boteval
  (:require [clojure.string :as string]))

(defn is-safe? [exp]
  (string/blank?
    (re-matches #".*Runtime.*|.*shell.*|.*System.*|.*while true.*" exp)))

(defn evaluate [exp]
  (if (is-safe? exp)
    (load-string exp)
    nil))

