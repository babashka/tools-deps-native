#!/usr/bin/env bb
;; Build GraalVM configuration using the native-image-agent.

;; Runs a command to determine required config that can not be determined
;; statically.

(require '[babashka.process :refer [process]])

(def trace-agent "-agentlib:native-image-agent")

(defn trace [args]
  (let [cmd         (into
                     ["clojure" "-Scp" (first *command-line-args*) "-M:compile"]
                     args)
        trace-agent (str trace-agent "=trace-output=trace-file.json")]
    @(process
      cmd
      {:inherit   false
       :extra-env {"JAVA_TOOL_OPTIONS" trace-agent}})))

(defn trace->config [config-dir]
  @(process
    ["native-image-configure"
     "generate"
     "--trace-input=trace-file.json"
     (str "--output-dir=" config-dir)]))


(trace ["deps" "{}"])
(trace ["create-basis"
        ;; NOTE adding an s3 dep here would enable s3 support?
        "{:deps {org.clojure/clojure {:mvn/version \"1.10.3\"}}}"])
(trace->config ".")
