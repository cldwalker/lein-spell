(ns leiningen.spell
 (:require [leiningen.core.eval :as eval]))

(defn ^:no-project-needed spell
  "Finds misspelled words in given files and prints them one per line. If a clojure file, only the
fn docs are searched. If no args given, searches **/*.{md,txt} files and clojure files under src/.

Options:
* -n, --file-line : Outputs in grep -nH format i.e. file:line:text for use with vim's grep."
  [project & args]
  (eval/eval-in-project
    (update-in project [:dependencies]
               conj ['lein-spell "0.1.0"])
    `(leiningen.spell.core/spell (list ~@args) (list ~@(:source-paths project)))
    '(require 'leiningen.spell.core)))
