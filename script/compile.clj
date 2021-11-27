#!/usr/bin/env bb

(require '[babashka.fs :as fs]
         '[babashka.tasks :refer [shell clojure]]
         '[clojure.string :as str]
         '[graalvm :refer [native-image-dir
                           native-bin
                           extra-env]])

(def app_name "tools-deps-native")
(def app_ns "borkdude.tdn.main")

(run! prn (map str (fs/list-dir native-image-dir)))
(prn (native-bin "gu"))
(shell (format "%s install native-image"
               (native-bin "gu")))

(fs/delete-tree "classes")
(fs/delete-tree "tools-deps-native")
(fs/create-dir "classes")

(clojure {:extra-env extra-env}
         "-M:compile-main")

(def init-at-build-time (:out (clojure {:extra-env extra-env
                                        :out :string}
                                       "-X borkdude.tdn.main/init-at-build-time")))

(println "Init at build time:")
(println init-at-build-time)

(def classpath (str (str/trim (with-out-str (clojure {:extra-env extra-env} "-Spath")))
                    fs/path-separator "classes"))

(shell "script/gen-reflect-config.clj" classpath)

(println "Compiling")

(prn classpath)

(def args ["-cp" classpath
           (str "-H:Name=" app_name)
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
           "--allow-incomplete-classpath"
           "--trace-object-instantiation=java.lang.Thread"
           "-J-Xmx5g"
           app_ns])

(apply shell {:extra-env extra-env}
       (cons (native-bin "native-image")
             args))

nil
