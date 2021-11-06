(ns borkdude.tdn.main
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.deps.alpha :as deps])
  (:gen-class))

#_(require '[clojure.tools.deps.alpha.extensions :as ext]) ;; somehow requiring this namespace as a side effect helps...
(require '[clojure.edn :as edn]
         '[clojure.tools.deps.alpha.util.maven :as mvn])

;; avoid null pointer
#_(mvn/make-system)

(def default-repos
  {"central" {:url "https://repo1.maven.org/maven2/"}
   "clojars" {:url "https://repo.clojars.org/"}})

(defn -main [& args]
  (mvn/make-system)
  (let [arg     (first args)
        edn-str (if (.exists (io/file arg))
                  (slurp arg)
                  arg)]
    (prn (-> (edn/read-string edn-str)
             (update :mvn/repos (fn [repos]
                                  (or repos default-repos)))
             (deps/resolve-deps nil)
             (deps/make-classpath nil nil)))))

(defn init-at-build-time [_]
  (mvn/make-system)
  (println
   (->> (map ns-name (all-ns))
        (remove #(clojure.string/starts-with? % "clojure"))
        (map #(clojure.string/split (str %) #"\."))
        (keep butlast)
        (map #(clojure.string/join "." %))
        distinct
        (map munge)
        (cons "clojure")
        (str/join ","))))
