(ns punch.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [punch.core-test]
   [punch.common-test]))

(enable-console-print!)

(doo-tests 'punch.core-test
           'punch.common-test)
