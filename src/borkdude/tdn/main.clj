(ns borkdude.tdn.main
  (:require [clojure.tools.deps.alpha :as deps])
  (:gen-class))

(defn -main [& _args]
  (prn (-> {:deps '{github-borkdude/babashka.curl
                    {:git/url "https://github.com/borkdude/babashka.curl"
                     :sha "2dc7f53271de3c2edc3e1474a000b9dfa7324eaf"}}
            :mvn/repos {"central" {:url "https://repo1.maven.org/maven2/"}
                        "clojars" {:url "https://repo.clojars.org/"}}}
           (deps/resolve-deps nil)
           (deps/make-classpath nil nil))))
