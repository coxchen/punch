(ns punch.common-test
  #? (:cljs (:require-macros [cljs.test :refer (is deftest testing)]))
  (:require [punch.common :as sut]
            #?(:clj [clojure.test :refer :all]
               :cljs [cljs.test])))

(deftest example-passing-test-cljc
  (is (= 1 1)))
