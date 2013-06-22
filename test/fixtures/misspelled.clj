(ns fixtures.misspelled)

(def ^{:doc "This corects for typos."} correction-factor)

(defn- misspell*
  "This fn should actally catch all typos."
  [])

(defn misspell
  "This fn should catch typos especially on itselve."
  []
  (misspell*))
