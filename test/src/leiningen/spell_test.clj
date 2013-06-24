(ns leiningen.spell-test
  (:require [leiningen.spell :as spell]
            [clojure.java.shell :as sh]
            [clojure.java.io :as io]
            [clojure.string]
            [bultitude.core :as b]
            [clojure.test :refer :all])
 (:import [java.io File]))

;; put is inside of predicate otherwise clojure.test wouldn't report actual value
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

(deftest spell-with-no-args
  (is (=
       "actally\ncorects\nitselve\n"
       (with-out-str
         (with-redefs [b/namespaces-on-classpath
                       (constantly '(test.fixtures.misspelled test.fixtures.perfect))
                       file-seq (constantly '())]
           (spell/spell* '()))))))

(defn- identifies-correct-typos?
  [expected input & [nsp]]
  (is (= expected
         (spell/typos-for-file
           (doto
             (File/createTempFile (first expected) ".txt")
             (#(spit % input)))
           nsp))))


(deftest typos-for-file-test
  (testing "whitelist of whitelist.txt works"
    (identifies-correct-typos? '("arested") "This is arested autoscaling development"))
  (testing "whitelist of clojure core names"
    (identifies-correct-typos? '("xpand") "To really xpand a macro, use macroexpand."))
  (testing "whitelist of current ns names"
    (def cowabunga)
    (identifies-correct-typos? '("woah") "This ns could have names like woah and cowabunga." 'leiningen.spell-test))
  (testing "whitelist of any words with caps"
    (identifies-correct-typos? '("ridiculusly") "I Loled ridiculusly hard on your HTTPS joke."))
  (testing "whitelist of apostrophe words"
    (identifies-correct-typos? '("critterz")
                               "Them critterz have mess'd with the wrong fellah's."))
  (testing "whitelist of pluralized words"
    (identifies-correct-typos? '("tigerrs")
                               "Lions, tigerrs, defstructs and backends oh my."))
  (testing "whitelist of .lein-spell"
    (spit ".lein-spell" "defenestrate\n")
    (require 'leiningen.spell :reload) ;; reload memoized whitelist
    (identifies-correct-typos? '("anoher")
                               "If he rhymes anoher clojar with clojure I swear I'm going to defenestrate him.")
    (.delete (io/file ".lein-spell"))))
