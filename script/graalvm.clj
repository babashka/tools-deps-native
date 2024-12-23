(ns graalvm
  (:require [babashka.fs :as fs]
            [clojure.string :as str]))

(def graalvm-home (System/getenv "GRAALVM_HOME"))

(when-not graalvm-home
  (println "Please set GRAALVM_HOME")
  (System/exit 1))

(def native-image-dir (str (fs/file graalvm-home "bin")))

(def windows? (str/starts-with? (System/getProperty "os.name")
                                "Windows"))

(def extra-env {"JAVA_HOME" graalvm-home
                "PATH" (str/join fs/path-separator
                                 [(fs/file graalvm-home "bin")
                                  ;; (fs/file graalvm-home "bin" "lib" "svm" "macros")
                                  (System/getenv "PATH")])})

(defn native-bin [prog]
  (let [prog (str (fs/file native-image-dir prog))]
    (if windows? (str prog ".cmd") prog)))
