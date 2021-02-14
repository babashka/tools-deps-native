(ns borkdude.tdn.main
  (:require [clojure.java.io :as io]
            [clojure.tools.deps.alpha :as deps])
  (:gen-class))

#_(require '[clojure.tools.deps.alpha.extensions :as ext]) ;; somehow requiring this namespace as a side effect helps...
(require '[clojure.edn :as edn]
         '[clojure.tools.deps.alpha.util.maven :as mvn])

;; avoid null pointer
(mvn/make-system)

(defn -main [& args]
  (let [arg (first args)
        edn-str (if (.exists (io/file arg))
                  (slurp arg)
                  arg)]
    (prn (-> (edn/read-string edn-str)
             (update :mvn/repos (fn [repos]
                                  (or repos
                                      {"central" {:url "https://repo1.maven.org/maven2/"}
                                       "clojars" {:url "https://repo.clojars.org/"}})))
             (deps/resolve-deps nil)
             (deps/make-classpath nil nil)))))
