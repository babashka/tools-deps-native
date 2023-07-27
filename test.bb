#!/usr/bin/env bb

(require '[babashka.pods :as pods])
(pods/load-pod ["clojure" "-M" "-m" "borkdude.tdn.main"]
               #_"./tools-deps-native")

(require '[clojure.tools.deps :as deps])
(prn (deps/calc-basis {:project "test/test-deps.edn" :aliases [:env/dev]}))
