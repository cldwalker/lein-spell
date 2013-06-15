(ns leiningen.spell
 (:require [clojure.string]))

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
  (-> "datomic-whitelist" file-lines rest))

(defn typos-for-ns [nsp]
  (when (symbol? nsp)
    (require nsp :reload))
  (let [nsp (if (symbol? nsp) (find-ns nsp) nsp)]
    (->> (clojure.java.shell/sh
           "bash"
           "-c"
           (format "cat %s | aspell --ignore=3 list --home-dir=. --personal=whitelist | sort | uniq"
                   (doc-file-for-ns nsp)))
         :out
         (#(clojure.string/split % #"\n"))
         (remove (set (fetch-whitelist)))
         (clojure.string/join "\n"))))


(comment
  (defn with-char-<6
    [lines]
    (->> lines
         (group-by count)
         (filter (fn [[k v]] (#{4 5} k)))
         vals 
         flatten))

  (defn try-fn
    ([f] (try-fn f false))
    ([f verbose]
     (let [lines (concat
                   (->> "whitelist" file-lines rest) 
                   (->> "datomic-whitelist" file-lines rest))
           matching-lines (f lines)
           ratio (/ (count matching-lines) (count lines))]
       (when verbose (prn matching-lines))
       (str (format "%.2f" (float ratio)) " - " ratio))))

  (try-fn with-char-<6)
  (try-fn (fn [l] (filter #(re-find #"^[A-Z]" %) l)) true)

  (slurp "pallet.actions")
  (require 'pallet.actions :reload)
  (println (typos-for-ns 'pallet.core.api))
  )

(defn spell
  "Given a namespace, finds misspelled words in fn docs and prints them one per line."
  [project & args]
  (println (typos-for-ns (symbol (first args)))))
