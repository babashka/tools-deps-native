#!/usr/bin/env bb

(require '[babashka.pods :as pods])
(pods/load-pod "./tools-deps-native")

(require '[clojure.tools.deps.alpha :as deps])
(deps/calc-basis {})
