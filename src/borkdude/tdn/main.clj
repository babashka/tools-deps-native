(ns borkdude.tdn.main
  (:require [clojure.tools.deps.alpha :as deps])
  (:gen-class))

(require '[clojure.tools.deps.alpha.extensions :as ext])
(require '[clojure.tools.deps.alpha.util.maven :as mvn])

(defn -main [& _args]
  (prn :mvn/system (mvn/make-system))
  (prn (ext/coord-paths 'clj-kondo/clj-kondo {:mvn/version "2020.12.12"} :mvn {:mvn/repos clojure.tools.deps.alpha.util.maven/standard-repos}))
  (prn (-> {:deps '{github-borkdude/babashka.curl
                    {:git/url "https://github.com/borkdude/babashka.curl"
                     :sha "2dc7f53271de3c2edc3e1474a000b9dfa7324eaf"}}
            :mvn/repos {"central" {:url "https://repo1.maven.org/maven2/"}
                        "clojars" {:url "https://repo.clojars.org/"}}}
           (deps/resolve-deps nil)
           (deps/make-classpath nil nil))))
