#!/usr/bin/env bb

(require '[babashka.fs :as fs]
         '[babashka.tasks :refer [shell clojure]]
         '[clojure.string :as str]
         '[graalvm :refer [native-bin
                           extra-env
                           windows?]])

(def app_name "tools-deps-native")
(def app_ns "borkdude.tdn.main")

(fs/delete-tree "classes")
(fs/delete-tree "tools-deps-native")
(fs/create-dir "classes")

(println "java version")
(clojure "-M" "-e" "(System/getProperty \"java.version\")")

(System/exit 1)

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

;; uncomment to gen reflect config
#_(shell "bb script/gen-reflect-config.clj" classpath)

(println "Compiling")

(prn app_ns)

(def args
  (cond-> ["-cp" classpath
           "-J-Xmx5g" (str "-H:Name=" app_name)
           "-H:+ReportExceptionStackTraces"
           "-H:ReflectionConfigurationFiles=reflect-config-cleaned.json,reflect-config-manual.json"
           "-H:ResourceConfigurationFiles=resources.json"
           "-H:+JNI"
           "-H:Log=registerResource:"
           "-H:EnableURLProtocols=http,https,jar"
           "--enable-all-security-services"
           "-J-Dclojure.spec.skip-macros=true"
           "-J-Dclojure.compiler.direct-linking=true"
           (str "--initialize-at-build-time=" init-at-build-time ",cognitect,aws,org.slf4j")
           "--initialize-at-build-time=org.eclipse.aether.transport.http.HttpTransporterFactory"
           "--initialize-at-build-time=org.eclipse.aether.transport.http.Nexus2ChecksumExtractor"
           "--initialize-at-build-time=org.eclipse.aether.transport.http.XChecksumChecksumExtractor"
           "--initialize-at-build-time=org.eclipse.aether.util.version.GenericVersionScheme"
           "--report-unsupported-elements-at-runtime"
           "--verbose"
           "--no-fallback"
           "--no-server"
           "--allow-incomplete-classpath"
           "-march=compatibility"
           "-O1"]
    (= "true" (System/getenv "BABASHKA_MUSL"))
    (conj "--static"
          "--libc=musl"
          ;; see https://github.com/oracle/graal/issues/3398
          "-H:CCompilerOption=-Wl,-z,stack-size=2097152")))

(spit "native-image-args.txt" (str/join " " args))

(prn :install-dir (System/getenv "VSINSTALLDIR"))

(shell {:extra-env extra-env}
       (native-bin "native-image")
       app_ns "@native-image-args.txt")

nil
