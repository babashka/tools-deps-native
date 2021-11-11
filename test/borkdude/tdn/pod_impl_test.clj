(ns borkdude.tdn.pod-impl-test
  (:require
   [borkdude.tdn.pod :as pod]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(deftest wrapped-sym-test
  (doseq [sym ['abc 'abc/def]]
    (testing (str "with sym " sym)
      (let [wrapped (pod/wrapped-sym sym)]
        (is (= (namespace sym) (namespace wrapped)))
        (is (= sym (pod/unwrapped-sym wrapped)))
        (is (str/starts-with? (name wrapped) "-pod-"))))))
