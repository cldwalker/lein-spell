(defproject lein-spell "0.1.0"
  :description "This library catches spelling mistakes in programming documents and clojure docstrings."
  :url "https://github.com/cldwalker/lein-spell"
  :license {:name "The MIT License"
            :url "https://en.wikipedia.org/wiki/MIT_License"}
  :eval-in :leiningen
  ;; because test/fixtures has broken nses that shouldn't be loaded by default
  :test-paths ["test/src"]
  :dependencies [[bultitude "0.2.2"]]
  :profiles  {:1.5  {:dependencies [[org.clojure/clojure "1.5.1"]]}}
  :aliases {"all" ["with-profile" "dev:dev,1.5"]})
