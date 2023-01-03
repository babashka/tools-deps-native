(ns borkdude.tdn.cli
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.tools.deps :as deps]))

(def default-repos
  {"central" {:url "https://repo1.maven.org/maven2/"}
   "clojars" {:url "https://repo.clojars.org/"}})

(defn usage []
  (println
   (str "tools-deps-native ["
        "help | "
        "deps | "
        "create-basis | "
        "root-deps | "
        "slurp-deps | "
        "user-deps-path"
        "] ")))

(defn help [[_ f]]
  (println)
  (if f
    (case f
      "help" (println "help [cmd]\n\nShow help.")
      "deps" (println "deps [path|deps-edn-map]

Load the given path to a deps.edn file, defaults to ./deps.edn.
Output a classpath EDN map with :classpath-roots and :classpath keys.")
      "create-basis" (println "create-basis [path]

Output a basis from a set of deps sources and a set of aliases. By default,
use root, user, and project deps and no argmaps (essentially the same classpath
you get by default from the Clojure CLI).

Each dep source value can be :standard, a string path, a deps edn map, or nil.
Sources are merged in the order - :root, :user, :project, :extra.

Aliases refer to argmaps in the merged deps that will be supplied to the basis
subprocesses (tool, resolve-deps, make-classpath-map).")
      "root-deps" (println "root-deps

Read and output the root deps.edn resource from the classpath at the path
clojure/tools/deps/deps.edn")
      "slurp-deps" (println "slurp-deps [path]

Read a single deps.edn file from disk and canonicalize symbols.
Outputs a deps map. If the file doesn't exist, returns nil.

Defaults to ./deps.edn")
      "user-deps-path" (println "user-deps-path

Use the same logic as clj to output the location of the user deps.edn.
Note that it's possible no file may exist at this location."))

    (println "tools.deps.edn ...

          help  show this help message
          deps  output a claspath map from a deps map
  create-basis  output a basis from a set of deps sources and a set of aliases
     root-deps  output the root deps.edn
    slurp-deps  read, canonicalize and output a deps.edn file
user-deps-path  output the path to the users deps.edn file

Use tools-deps.edn help <cmd> to get more specific help"))
  (println))

(defn slurp-deps
  ([path]
   (let [file (io/file path)]
     (when (.exists file)
       (deps/slurp-deps file))))
  ([] (deps/slurp-deps (io/file "deps.edn"))))

(defn create-basis [args]
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
        deps (cond
               (and arg (.exists (io/file arg)))
               (slurp-deps arg)

               arg
               (edn/read-string arg)

               :else
               (slurp-deps))]
    (if (map? deps)
      (prn (as-> deps x
             (update x :mvn/repos (fn [repos]
                                    (or repos default-repos)))
             (deps/resolve-deps x nil)
             (deps/make-classpath-map deps x nil)))
      (binding [*out* *err*]
        (throw
         (ex-info (str"Invalid argument " deps ", expected a map") {}))))))

(defn cli [args]
  (if-let [arg (some-> args first keyword)]
    (try
      (case arg
        :deps           (deps (rest args))
        :create-basis   (create-basis (rest args))
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
