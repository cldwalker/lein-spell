(ns leiningen.spell-test
  (:require [leiningen.spell :refer :all]
            [clojure.java.shell :as sh]
            [clojure.test :refer :all]))

(defn- has-correct-file-output?
  [output & files]
  (is (= output
         (->
           (apply sh/sh "lein" "spell"
                  (map #(str "test/fixtures/" %) files))
           :out))))

(defn- has-correct-warning?
  [regex file]
  (is (re-find regex
               (:err (sh/sh "lein" "spell" (str "test/fixtures/" file))))))

(deftest spell-with-file-arguments
  (testing "misspelled words from doc file"
    (has-correct-file-output? "miskate\n" "misspelled.txt"))
  (testing "no misspelled words from doc file"
    (has-correct-file-output? "\n" "perfect.txt"))
  (testing "misspelled words from clojure file"
    (has-correct-file-output? "actally\ncorects\nitselve\n" "misspelled.clj"))
  (testing "no misspelled words from clojure file"
    (has-correct-file-output? "\n" "perfect.clj"))
  (testing "misspelled words from doc and clojure files - good and bad"
    (has-correct-file-output? "actally\ncorects\nitselve\nmiskate\n"
                              "misspelled.txt" "perfect.txt" "misspelled.clj" "perfect.clj"))
  (testing "warning for no namespace in clj file"
    (has-correct-warning? #"^No namespace found" "namespaceless.clj"))
  (testing "warning for failed require of a clj file"
    (has-correct-warning? #"Failed on namespace test.fixtures.require-failed-whoops with exception" "require_failed.clj")))

;; stub b/namespaces with fixtures.*

;; unit tests
;; whitelist, core name, local whitelist, ns name, ignorable
