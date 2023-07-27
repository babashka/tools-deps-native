(ns borkdude.tdn.pod-test
  (:require
   [clojure.java.io :as io]
   [babashka.pods :as pods]
   [clojure.test :refer [deftest is testing]]))

(def pod-spec (if (= "native" (System/getenv "POD_TEST_ENV"))
                (do
                  (println "using native pod")
                  "./tools-deps-native")
                ["clojure" "-Mrun"]))

(deftest pod-lifecycle-test
  (testing "pod can be loaded and unloaded"
    (let [pod-id (pods/load-pod pod-spec)]
      (is pod-id)
      (pods/unload-pod pod-id))))

(deftest calc-basis-test
  (testing "pod can be invoke calc-basis"
    (let [pod-id (pods/load-pod pod-spec)]
      (try
        (let [res (pods/invoke
                   pod-id
                   'clojure.tools.deps/-pod-calc-basis
                   [(io/file ".") [{}]])]
          (is (= {:libs {}, :classpath-roots [], :classpath {}}
                 res )))
        (finally
          (pods/unload-pod pod-id)))))
  (testing "more complex deps.edn"
    (let [pod-id (pods/load-pod pod-spec)]
      (try
        (let [res (pods/invoke
                   pod-id
                   'clojure.tools.deps/-pod-calc-basis
                   [(io/file "test/test-deps.edn") [{}]])]
          (is (= {:libs {}, :classpath-roots [], :classpath {}}
                 res )))
        (finally
          (pods/unload-pod pod-id))))))
