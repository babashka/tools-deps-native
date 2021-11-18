(ns borkdude.tdn.main
  (:require
   [borkdude.tdn.cli :as cli]
   [borkdude.tdn.pod :as pod]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.deps.alpha]
   [clojure.tools.deps.alpha.util.dir :as dir]
   [clojure.tools.deps.alpha.util.maven :as mvn])
  (:gen-class))

(defmacro reference-publics []
  (let [publics (-> (the-ns 'clojure.tools.deps.alpha)
                    ns-publics
                    keys)]
    `(def publics# ~(vec (for [sym publics]
                           (symbol "clojure.tools.deps.alpha"
                                   (name sym)))))))

(reference-publics)

;; Stop the build directory from being baked into the binary.
;; This gets initialised in -main.
(alter-var-root #'dir/*the-dir* (constantly nil))

(defn initialise-dir []
  (alter-var-root
   #'dir/*the-dir*
   (constantly (io/file (System/getProperty "user.dir"))))
  (pod/debug :user.dir (System/getProperty "user.dir")))

(defn -main [& args]
  (mvn/make-system)
  (initialise-dir)

  (if (System/getenv "BABASHKA_POD")
    (pod/pod args)
    (cli/cli args)))

(defn init-at-build-time [_]
  (println
   (->> (map ns-name (all-ns))
        (remove #(str/starts-with? % "clojure"))
        (map #(str/split (str %) #"\."))
        (keep butlast)
        (map #(str/join "." %))
        distinct
        (map munge)
        (cons "clojure")
        (str/join ","))))

(comment
  (init-at-build-time nil))
