#!/usr/bin/env bb

(require '[babashka.fs :as fs]
         '[babashka.tasks :refer [shell clojure]]
         '[clojure.string :as str]
         '[graalvm :refer [native-bin
                           extra-env]])

(shell "java" "-version")

(def app_name "tools-deps-native")
(def app_ns "borkdude.tdn.main")

(shell (native-bin "gu") "install" "native-image")

(fs/delete-tree "classes")
(fs/delete-tree "tools-deps-native")
(fs/create-dir "classes")

(clojure {:extra-env extra-env}
         "-M:compile-main")

(def init-at-build-time
  (str/trim (:out (clojure {:extra-env extra-env
                            :out :string}
                           "-X borkdude.tdn.main/init-at-build-time"))))

(println "Init at build time:")
(println init-at-build-time)

(def classpath (str (str/trim (with-out-str (clojure {:extra-env extra-env} "-Spath")))
                    fs/path-separator "classes"))

(shell "bb script/gen-reflect-config.clj" classpath)

(println "Compiling")

(prn app_ns)

(def args ["-cp" classpath
           "-J-Xmx5g" (str "-H:Name=" app_name)
           "-H:+ReportExceptionStackTraces"
           "-H:ReflectionConfigurationFiles=reflect-config.json,reflect-config-manual.json"
           "-H:ResourceConfigurationFiles=resources.json"
           "-H:+JNI"
           "-H:Log=registerResource:"
           "-H:EnableURLProtocols=http,https,jar"
           "--enable-all-security-services"
           "-J-Dclojure.spec.skip-macros=true"
           "-J-Dclojure.compiler.direct-linking=true"
           (str "--initialize-at-build-time=" init-at-build-time ",cognitect,aws,org.slf4j")
           "--report-unsupported-elements-at-runtime"
           "--verbose"
           "--no-fallback"
           "--no-server"
           "--allow-incomplete-classpath"])

(spit "native-image-args.txt" (str/join " " args))

(prn (cons (native-bin "native-image")
           args))
(shell {:extra-env extra-env}
       (native-bin "native-image")
       app_ns "@native-image-args.txt")

nil
