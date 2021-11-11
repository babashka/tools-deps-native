(ns borkdude.tdn.pod-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [babashka.pods.jvm :as pods]))


(def pod-spec (if (= "native" (System/getenv "POD_TEST_ENV"))
                "./tools-deps-native"
                ["clojure" "-Mrun"]))

(alter-var-root
 #'borkdude.tdn.pod/client-pod-namespace
 (constantly 'babashka.pods.jvm))

(deftest pod-lifecycle-test
  (testing "pod can be loaded and unloaded"
    (let [pod-id (pods/load-pod pod-spec)]
      (is pod-id)
      (pods/unload-pod pod-id))))

(deftest calc-basis-test
  (testing "pod can be invoke calc-bais"
    (let [pod-id (pods/load-pod pod-spec)]
      (try
        (let [res (pods/invoke
                   pod-id
                   'clojure.tools.deps.alpha/-pod-calc-basis "." [{}])]
          (is (= {:libs {}, :classpath-roots [], :classpath {}}
                 res )))
        (finally
          (pods/unload-pod pod-id))))))
