(ns leiningen.spell-test
  (:require [leiningen.spell :refer :all]
            [clojure.java.shell :as sh]
            [clojure.test :refer :all]))

(defn- has-correct-file-output?
  [output file]
  (is (= output
         (->
           (sh/sh "lein" "spell" (str "test/fixtures/" file))
           :out))))

(deftest spell-with-file-arguments
  (testing "misspelled words from doc file"
    (has-correct-file-output? "miskate\n" "misspelled.txt"))
  (testing "no misspelled words from doc file"
    (has-correct-file-output? "\n" "perfect.txt"))
  (testing "misspelled words from clojure file"
    (has-correct-file-output? "actally\ncorects\nitselve\n" "misspelled.clj"))
  (testing "no misspelled words from clojure file"
    (has-correct-file-output? "\n" "perfect.clj")))
