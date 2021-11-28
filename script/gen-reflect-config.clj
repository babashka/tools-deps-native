#!/usr/bin/env bb
;; Build GraalVM configuration using the native-image-agent.

;; Runs a command to determine required config that can not be determined
;; statically.

(require '[babashka.tasks :refer [shell]]
         '[cheshire.core :as cheshire]
         '[clojure.string :as str]
         '[graalvm :refer [extra-env
                           native-bin]])

;; (def trace-agent "-agentlib:native-image-agent")

;; (defn trace [args]
;;   (let [cmd (into
;;              ["-Scp" (first *command-line-args*) "-M:compile"]
;;              args)
;;         trace-agent (str trace-agent "=trace-output=trace-file.json")]
;;     (apply shell {:extra-env (assoc extra-env
;;                                     "JAVA_TOOL_OPTIONS" trace-agent)}
;;            "bb clojure" cmd)))

;; (defn trace->config [config-dir]
;;   (-> (shell
;;        (native-bin "native-image-configure")
;;        "generate"
;;        "--trace-input=trace-file.json"
;;        (str "--output-dir=" config-dir)
;;        )))

;; (println "Tracing")

;; (trace ["deps" "{}"])
;; (trace ["create-basis"
;;         ;; NOTE adding an s3 dep here would enable s3 support?
;;         "{:deps {org.clojure/clojure {:mvn/version \"1.10.3\"}}}"])

;; (trace->config ".")

(def trace-json (cheshire/parse-string (slurp "trace-file.json") true))

;; [Z = boolean
;; [B = byte
;; [S = short
;; [I = int
;; [J = long
;; [F = float
;; [D = double
;; [C = char
;; [L = any non-primitives(Object)

(defn normalize-array-name [n]
  ({"[F" "float[]"
    "[B" "byte[]"
    "[Z" "boolean[]"
    "[C" "char[]"
    "[D" "double[]"
    "[I" "int[]"
    "[J" "long[]"
    "[S" "short[]"} n n))

(def ignored (atom #{}))
(def unignored (atom #{}))

(defn ignore [{:keys [:tracer :caller_class :function :args] :as _m}]
  (when (= "reflect" tracer)
    (when-let [arg (first args)]
      (let [arg (normalize-array-name arg)]
        (if (and caller_class
                 (or (= "clojure.lang.RT" caller_class)
                     (= "clojure.genclass__init" caller_class)
                     (and (str/starts-with? caller_class "clojure.core$fn")
                          (= "java.sql.Timestamp" arg)))
                 (= "forName" function))
          (swap! ignored conj arg)
          (when (= "clojure.lang.RT" caller_class)
            ;; unignore other reflective calls in clojure.lang.RT
            (swap! unignored conj arg)))))))

(run! ignore trace-json)

;; (prn @ignored)
;; (prn @unignored)

(defn process-1 [{:keys [:name] :as m}]
  (when-not (or (and (= 1 (count m))
                     (contains? @ignored name)
                     (not (contains? @unignored name)))
                (str/includes? name "$eval"))
    ;; fix bug(?) in automated generated config
    (if (= "java.lang.reflect.Method" name)
      (assoc m :name "java.lang.reflect.AccessibleObject")
      m)))


(def reflect-config (cheshire/parse-string (slurp "reflect-config.json") true))

(def config-json (cheshire/parse-string (slurp "reflect-config.json") true))

(def cleaned (keep process-1 config-json))

(spit "reflect-config-cleaned.json" (cheshire/generate-string cleaned {:pretty true}))

(println "Done tracing")
