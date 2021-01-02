(ns borkdude.tdn.main
  (:require [clojure.tools.deps.alpha :as deps])
  (:gen-class))

(require '[clojure.tools.deps.alpha.extensions :as ext]) ;; somehow requiring this namespace as a side effect helps...
(require '[clojure.edn :as edn]
         '[clojure.tools.deps.alpha.util.maven :as mvn])

(mvn/make-system)

(defn -main [& args]
  (prn :mvn/system (mvn/make-system))
  (prn (ext/coord-paths 'clj-kondo/clj-kondo {:mvn/version "2020.12.12"} :mvn {:mvn/repos clojure.tools.deps.alpha.util.maven/standard-repos}))
  (prn :fst (first args))
  (prn :deps (edn/read-string (first args)))
  (prn (-> (edn/read-string (first args))
           (deps/resolve-deps nil)
           (deps/make-classpath nil nil)))
  (println)
  #_(prn (-> {:deps '{github-borkdude/babashka.curl
                    {:git/url "https://github.com/borkdude/babashka.curl"
                     :sha "2dc7f53271de3c2edc3e1474a000b9dfa7324eaf"}}
            :mvn/repos {"central" {:url "https://repo1.maven.org/maven2/"}
                        "clojars" {:url "https://repo.clojars.org/"}}}
           (deps/resolve-deps nil)
           (deps/make-classpath nil nil))))
