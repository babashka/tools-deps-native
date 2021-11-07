(ns borkdude.tdn.main
  (:require
   [borkdude.tdn.cli :as cli]
   [borkdude.tdn.pod :as pod]
   [clojure.string :as str]
   [clojure.tools.deps.alpha])
  (:gen-class))

(defmacro reference-publics []
  (let [publics (-> (the-ns 'clojure.tools.deps.alpha)
                    ns-publics
                    keys)]
    `(def publics# ~(vec (for [sym publics]
                           (symbol "clojure.tools.deps.alpha"
                                   (name sym)))))))

(reference-publics)

#_(require '[clojure.tools.deps.alpha.extensions :as ext]) ;; somehow requiring this namespace as a side effect helps...
(require '[clojure.edn :as edn]
         '[clojure.tools.deps.alpha.util.maven :as mvn])

;; avoid null pointer
#_(mvn/make-system)


(defn -main [& args]
  ;; TODO use BABASHKA_POD to detect when running as POD or CLI
  (mvn/make-system)
  (if (System/getenv "BABASHKA_POD")
    (pod/pod args)
    (cli/cli args)))

(defn init-at-build-time [_]
  (mvn/make-system)
  (println
   (->> (map ns-name (all-ns))
        (remove #(clojure.string/starts-with? % "clojure"))
        (map #(clojure.string/split (str %) #"\."))
        (keep butlast)
        (map #(clojure.string/join "." %))
        distinct
        (map munge)
        (cons "clojure")
        (str/join ","))))

(comment
  (init-at-build-time nil))
