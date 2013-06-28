(ns leiningen.spell
 (:require [clojure.string :as string]
           [clojure.set]
           [clojure.java.shell]
           [leiningen.core.eval :as eval]
           [bultitude.core :as b]
           [clojure.java.io :as io])
  (:import [java.io File]))

(def ^:dynamic ^{:doc "When enabled, typos have file and line info."} *file-line-mode* false)

(defn- doc-file-for-ns [nsp]
  (let [file (File/createTempFile (str nsp) ".txt")]
    (spit file
          (->> nsp ns-interns vals
               (map (comp :doc meta))
               (string/join "\n")))
    file))

(defn- file-lines [file]
  (-> file slurp (string/split #"\n")))

(defn fetch-whitelist
  "Returns a list of allowed words - mostly CS, programming and clojure words."
  []
  (-> "whitelist.txt" io/resource file-lines rest))

(defn- ns-intern-names
  [nsp]
  (->> nsp find-ns ns-interns vals (map (comp name :name meta))))

(defn- core-names
  []
  (ns-intern-names 'clojure.core))

(defn fetch-local-whitelist
  "Fetches local whitelist from .lein-spell if it exists"
  []
  (if (.exists (io/file ".lein-spell"))
    (file-lines ".lein-spell")
    []))

(defn fetch-whitelist-pluralized
  "Fetches plurals of all whitelist words"
  []
  (map
    #(str % "s")
    (concat (core-names)
            (fetch-whitelist)
            (fetch-local-whitelist))))

(def memoized-fetch-whitelist (memoize (comp set fetch-whitelist)))
(def memoized-core-names (memoize (comp set core-names)))
(def memoized-fetch-local-whitelist (memoize (comp set fetch-local-whitelist)))
(def memoized-fetch-whitelist-pluralized (memoize (comp set fetch-whitelist-pluralized)))

(defn- count-less-than-six
  "Filters lines that are 4 or 5 char count"
  [lines]
  (->> lines
       (group-by count)
       (filter (fn [[k v]] (#{4 5} k)))
       vals 
       flatten))

(defn- whitelist-with
  "Filters white-list with given f and prints % and ration affected. Useful for
  comparing possible white-list filters."
  ([f] (whitelist-with f false))
  ([f verbose]
   (let [lines (fetch-whitelist)
         matching-lines (f lines)
         ratio (/ (count matching-lines) (count lines))]
     (when verbose (prn matching-lines))
     (str (format "%.2f" (float ratio)) " - " ratio))))

(defn ignorable?
  "Returns truish value if misspelled word can be ignored."
  [word]
  (or (re-find #"[A-Z]" word)
      (re-find #"('d|'s)$" word)))

(defn remove-whitelisted
  "Removes words that are whitelisted."
  [nsp lines]
  (clojure.set/difference (set lines)
                          (memoized-fetch-whitelist)
                          (memoized-core-names)
                          (memoized-fetch-local-whitelist)
                          (set (when nsp (ns-intern-names nsp)))
                          (memoized-fetch-whitelist-pluralized)
                          #{""}))

(defn- ->file-line-maps
  "Creates file-line maps based on typos and current nsp/file scope."
  [typos nsp file]
  ;; smarter source-file lookup when not only looking at src namespaces
  (let [source-file (if nsp (str "src/" (b/path-for (str nsp))) file)
        lines (file-lines source-file)]
    (mapcat (fn [typo]
              (->> lines
                   (map-indexed (fn [i e] {:line (inc i) :text e :typo typo :file source-file}))
                   (filter #(.contains (:text %) typo))))
            typos)))

(defn typos-for-file
  "Given a file and optional ns, returns a list of misspelled words."
  ([file]
   (typos-for-file file nil))
  ([file nsp]
   (let [typos (->> file
                    (format "cat %s | aspell --ignore=3 list")
                    (clojure.java.shell/sh "bash" "-c")
                    :out
                    (#(string/split % #"\n"))
                    distinct
                    sort
                    (remove-whitelisted nsp)
                    (remove ignorable?))]
     (if *file-line-mode*
       (->file-line-maps typos nsp file)
       typos))))

(defn typos-for-ns
  "Given a namespace or namespace symbol, returns a list of misspelled words."
  [nsp]
  (when (symbol? nsp)
    (require nsp :reload))
  (-> (if (symbol? nsp) (find-ns nsp) nsp)
      doc-file-for-ns
      (typos-for-file nsp)))

(defn- warn
  [& msg]
  (binding [*out* *err*]
    (apply println msg)))

(defn- safe-typos-for-ns [nsp]
  (try (typos-for-ns nsp)
       (catch Exception e
         (warn (format "Failed on namespace %s with exception %s" nsp e)))))

(defn typos-for-all-ns
  "Returns a list of misspelled words for all namespaces under src/."
  []
  (->> (b/namespaces-on-classpath :classpath "src")
       (map (fn [nsp]
              [nsp (safe-typos-for-ns nsp)]))
       (filter #(seq (second %)))
       (mapcat second)))

(defn- ns-for-file
  [file]
  (or (b/ns-form-for-file file)
      (warn "No namespace found for" file)))

(defn typos-for-files
  "Returns a list of misspelled words for given files."
  [files]
  (->> files
       (map #(if (.endsWith % ".clj")
               (some-> % ns-for-file safe-typos-for-ns)
               (typos-for-file %)))
       (remove nil?)
       (mapcat identity)
       distinct))

(defn typos-for-ns-and-doc-files
  "Returns a list of misspelled words for all namespaces under src/ and *.{md,mdown,mkd,markdown,txt} doc files."
  []
  (let [doc-files (->> "." io/file file-seq (map str)
                       (filter #(re-find #"\.(md|markdown|mdown|mkd|txt)$" %)))]
    (-> (typos-for-files doc-files)
        (into (typos-for-all-ns))
        distinct)))

(comment
  (whitelist-with count-less-than-six)
  (whitelist-with (fn [l] (filter #(re-find #"^[A-Z]" %) l)) true)
  (time (typos-for-all-ns))
  (println (string/join "\n" (typos-for-ns 'pallet.api))))

(defn- print-lines
  [lines]
  (let [formatted-lines (if *file-line-mode*
                          (->> lines
                               (sort-by :typo)
                               (map #(format "%s:%s:%s" (:file %) (:line %) (:typo %))))
                          (sort lines))]
    (->> formatted-lines
         (string/join "\n")
         println)))

(defn spell*
  "Handles actual processing of spell"
  [args]
  (let [[opts args] (split-with #{"-n" "--file-line"} args)]
    (binding [*file-line-mode* (boolean (seq opts))]
      (if (seq args)
        (print-lines (typos-for-files args))
        (print-lines (typos-for-ns-and-doc-files))))))

(defn ^:no-project-needed spell
  "Finds misspelled words in given files and prints them one per line. If a clojure file, only the
  fn docs are searched. If no args given, searches **/*.{md,txt} files and clojure files under src/.

  Options:
  * -n, --file-line : Outputs in grep -nH format i.e. file:line:text for use with vim's grepprg."
  [project & args]
  (eval/eval-in-project project (spell* args)))
