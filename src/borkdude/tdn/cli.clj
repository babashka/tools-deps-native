(ns borkdude.tdn.cli
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [clojure.tools.deps.alpha :as deps]))

(def default-repos
  {"central" {:url "https://repo1.maven.org/maven2/"}
   "clojars" {:url "https://repo.clojars.org/"}})

(defn usage []
  (println "tools.deps.edn [ (deps [path])]")
  nil)

(defn help [args]
  (println "tools.deps.edn [ (deps [path])]")
  nil)

(defn slurp-deps
  ([path]
   (let [file (io/file path)]
     (when (.exists file)
       (deps/slurp-deps file))))
  ([] (deps/slurp-deps (io/file "deps.edn"))))

(defn create-basis [args]
  (prn :creat-basis args)
  (let [arg  (first args)
        deps (edn/read-string arg)]
    (if (map? deps)
      (prn (-> deps
               (update :mvn/repos (fn [repos]
                                    (or repos default-repos)))
               deps/create-basis))
      (binding [*out* *err*]
        (throw
         (ex-info (str"Invalid argument " deps ", expeccted a map") {}))))))

(defn deps [args]
  (let [arg  (first args)
        deps (or
              (slurp-deps arg)
              (edn/read-string arg))]
    (prn :deps deps)
    (if (map? deps)
      (prn (as-> deps x
             (update x :mvn/repos (fn [repos]
                                    (or repos default-repos)))
             (deps/resolve-deps x nil)
             (deps/make-classpath-map deps x nil)))
      (binding [*out* *err*]
        (throw
         (ex-info (str"Invalid argument " deps ", expeccted a map") {}))))))

(defn cli [args]
  (if-let [arg (some-> args first keyword)]
    (try
      (case arg
        :deps           (deps (rest args))
        :create-basis   (println (create-basis (rest args)))
        :root-deps      (println (deps/root-deps))
        :slurp-deps     (println (apply slurp-deps (rest args)))
        :user-deps-path (println (deps/user-deps-path))
        :help           (help args)
        (usage))
      (catch clojure.lang.ExceptionInfo e
        (binding [*out* *err*]
          (println (.getMessage e)))
        (System/exit 1)))
    (help args)))
