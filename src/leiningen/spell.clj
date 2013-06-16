(ns leiningen.spell
 (:require [clojure.string]
           [clojure.java.shell]
           [clojure.java.io :as io]))

(defn doc-file-for-ns [nsp]
  (spit (str nsp)
        (->> nsp ns-interns vals
             (map (comp :doc meta))
             (clojure.string/join "\n")))
  (str nsp))

(defn file-lines [file]
  (-> file slurp (clojure.string/split #"\n")))

(defn fetch-whitelist
  []
  (-> "whitelist.txt" io/resource file-lines rest))

(defn- count-less-than-six
  "Filters lines that are 4 or 5 char count"
  [lines]
  (->> lines
       (group-by count)
       (filter (fn [[k v]] (#{4 5} k)))
       vals 
       flatten))

(defn- whitelist-with
  "Filters whitelist with given f and prints % and ration affected. Useful for
  comparing possible whitelist filters."
  ([f] (whitelist-with f false))
  ([f verbose]
   (let [lines (fetch-whitelist)
         matching-lines (f lines)
         ratio (/ (count matching-lines) (count lines))]
     (when verbose (prn matching-lines))
     (str (format "%.2f" (float ratio)) " - " ratio))))

(defn typos-for-ns [nsp]
  (when (symbol? nsp)
    (require nsp :reload))
  (let [nsp (if (symbol? nsp) (find-ns nsp) nsp)]
    (->> (doc-file-for-ns nsp)
         (format "cat %s | aspell --ignore=3 list")
         (clojure.java.shell/sh "bash" "-c")
         :out
         (#(clojure.string/split % #"\n"))
         distinct
         sort
         (remove (set (fetch-whitelist)))
         (clojure.string/join "\n"))))

(comment
  (whitelist-with count-less-than-six)
  (whitelist-with (fn [l] (filter #(re-find #"^[A-Z]" %) l)) true)
  (println (typos-for-ns 'pallet.actions)))

(defn spell
  "Given a namespace, finds misspelled words in fn docs and prints them one per line."
  [project & args]
  (println (typos-for-ns (symbol (first args)))))
