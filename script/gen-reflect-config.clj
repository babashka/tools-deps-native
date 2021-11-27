#!/usr/bin/env bb
;; Build GraalVM configuration using the native-image-agent.

;; Runs a command to determine required config that can not be determined
;; statically.

(require '[babashka.tasks :refer [shell clojure]]
         '[graalvm :refer [extra-env
                           native-bin]])

(def trace-agent "-agentlib:native-image-agent")

(defn trace [args]
  (let [cmd (into
             ["-Scp" (first *command-line-args*) "-M:compile"]
             args)
        trace-agent (str trace-agent "=trace-output=trace-file.json")]
    (apply shell {:extra-env (assoc extra-env
                                    "JAVA_TOOL_OPTIONS" trace-agent)}
           "bb clojure" cmd)))

(defn trace->config [config-dir]
  (-> (shell
       (native-bin "native-image-configure")
       "generate"
       "--trace-input=trace-file.json"
       (str "--output-dir=" config-dir)
       )))

(println "Tracing")

(trace ["deps" "{}"])
(trace ["create-basis"
        ;; NOTE adding an s3 dep here would enable s3 support?
        "{:deps {org.clojure/clojure {:mvn/version \"1.10.3\"}}}"])

(trace->config ".")

(println "Done tracing")
