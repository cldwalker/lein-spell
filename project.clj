(defproject lein-spell "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :eval-in :leiningen
  ;; because test/fixtures has broken nses that shouldn't be loaded by default
  :test-paths ["test/src"]
  :dependencies [[bultitude "0.2.2"]
                 [org.clojure/clojure "1.5.1"]])
