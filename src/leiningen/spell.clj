(ns leiningen.spell
 (:require [clojure.string :as string]
           [clojure.set]
           [clojure.java.shell]
           [bultitude.core :as b]
           [clojure.java.io :as io])
  (:import [java.io File]))

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

(defn- core-fns
  []
  (->> 'clojure.core find-ns ns-interns vals (map (comp name :name meta)))) 

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
    (concat (core-fns)
            (fetch-whitelist)
            (fetch-local-whitelist))))

(def memoized-fetch-whitelist (memoize (comp set fetch-whitelist)))
(def memoized-core-fns (memoize (comp set core-fns)))
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
  [lines]
  (clojure.set/difference (set lines)
                          (memoized-fetch-whitelist)
                          (memoized-core-fns)
                          (memoized-fetch-local-whitelist)
                          (memoized-fetch-whitelist-pluralized)
                          #{""}))

(defn typos-for-file
  "Given a file, returns a list of misspelled words."
  [file]
  (->> file
       (format "cat %s | aspell --ignore=3 list")
       (clojure.java.shell/sh "bash" "-c")
       :out
       (#(string/split % #"\n"))
       distinct
       sort
       remove-whitelisted
       (remove ignorable?)))

(defn typos-for-ns
  "Given a namespace or namespace symbol, returns a list of misspelled words."
  [nsp]
  (when (symbol? nsp)
    (require nsp :reload))
  (-> (if (symbol? nsp) (find-ns nsp) nsp)
      doc-file-for-ns
      typos-for-file))

(defn typos-for-all-ns
  "Returns a list of misspelled words for all namespaces under src/."
  []
  (->> (b/namespaces-on-classpath :prefix "pallet")
       #_(b/namespaces-on-classpath :classpath "src")
       (map (fn [nsp]
              [nsp
               (try (typos-for-ns nsp)
                    (catch Exception e
                      (binding [*out* *err*]
                       (println (format "Failed on namespace %s with exception %s" nsp e)))))]))
       (filter #(seq (second %)))
       (mapcat second)
       distinct
       sort))

(comment
  (whitelist-with count-less-than-six)
  (whitelist-with (fn [l] (filter #(re-find #"^[A-Z]" %) l)) true)
  (time (typos-for-all-ns))
  (println (string/join "\n" (typos-for-ns 'pallet.api))))

(defn spell
  "Finds misspelled words in fn docs and prints them one per line. If given an arg,
  only does that namespace. Otherwise does all namespaces under src/."
  [project & args]
  (if (first args)
    (println (string/join "\n" (typos-for-ns (symbol (first args)))))
    (println (string/join "\n" (typos-for-all-ns)))))
