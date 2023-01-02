(ns borkdude.tdn.pod
  (:refer-clojure :exclude [read-string])
  (:require
   [bencode.core :as bencode]
   [borkdude.tdn.bbuild]
   [clojure.tools.deps]
   [clojure.tools.deps.util.dir]
   [clojure.tools.deps.util.maven]
   [clojure.tools.deps.util.session]
   [clojure.walk :as walk]
   [cognitect.transit :as transit])
  (:import
   [java.io PushbackInputStream]))

(set! *warn-on-reflection* true)

(def debug? false)

(defn debug [& strs]
  (when debug?
    (binding [*out* *err*]
      (apply println strs))))

;;; bencode

(def stdin (PushbackInputStream. System/in))

(defn read-bencode []
  (try (bencode/read-bencode stdin)
       (catch java.io.EOFException _)))

(defn write-bencode [v]
  (debug :write-bencode v)
  (bencode/write-bencode System/out v)
  (.flush System/out))

(defn read-string [^bytes v]
  (String. v))

(defn read-keyword [v]
  (some-> v read-string keyword))

(defn read-symbol [v]
  (some-> v read-string symbol))

(defn write-map [m]
  (write-bencode
   (walk/postwalk
    (fn ident-to-string [v] (if (ident? v) (name v) v))
    m)))

;;; payload
(def jiofile-key (str ::file))

(def jiofile-read-handler
  (transit/read-handler (fn [^String s] (java.io.File. s))))

(def jiofile-write-handler
  (transit/write-handler jiofile-key str))

(defn reg-transit-handlers
  []
  (format "
(require '[babashka.pods :as --pod])

(--pod/add-transit-read-handler!
    \"%s\"
    (fn [s] (java.io.File. s)))

(--pod/add-transit-write-handler!
  #{java.io.File}
  \"%s\"
  str)
"
          jiofile-key jiofile-key))

(def transit-read-handlers
  (delay
    (transit/read-handler-map
     {jiofile-key jiofile-read-handler})))

(def transit-write-handlers
  (delay
    (transit/write-handler-map
     {java.io.File jiofile-write-handler})))

(defn read-transit [^String v]
  (transit/read
   (transit/reader
    (java.io.ByteArrayInputStream. (.getBytes v "utf-8"))
    :json
    {:handlers @transit-read-handlers})))

(defn write-transit [v]
  (let [baos (java.io.ByteArrayOutputStream.)]
    (transit/write
     (transit/writer
      baos
      :json
      {:handlers @transit-write-handlers}) v)
    (.toString baos "utf-8")))

(defn read-payload [v]
  (some-> v read-string read-transit))

(defn write-payload [v]
  (some-> v write-transit))

;;; Implementation

(defn ns-public-syms
  "Return unqualified syms for all non-macro, function vars in ns"
  [ns-sym]
  (->> (the-ns ns-sym)
       ns-publics
       (filter #(and (not (:macro (meta (second %))))
                     (:arglists (meta (second %)))))
       (mapv key)))

(defn ns-public-fq-syms [ns-sym]
  (->> (ns-public-syms ns-sym)
       (mapv #(symbol (name ns-sym) (name %)))))

(defn wrapped-sym [sym]
  (symbol (namespace sym) (str "-pod-" (name sym))))

(defn unwrapped-sym [sym]
  (assert (> (count (name sym)) 5) (name sym))
  (symbol (namespace sym) (subs (name sym) 5)))

(def standard-repos-form
  `(~'def ~'standard-repos ~clojure.tools.deps.util.maven/standard-repos))

(def session-cache-form
  '(def session (java.util.concurrent.ConcurrentHashMap.)))

(def dir-var-form
  '(def ^:dynamic *the-dir*
     (clojure.java.io/file (System/getProperty "user.dir"))))

(def with-dir-form-str
  (str
   "(defmacro with-dir [^java.io.File dir & body] "
   " `(binding [clojure.tools.deps.util.dir/*the-dir* ~dir] ~@body))"))

(defn client-invoke-with-dir-form
  [f-sym]
  `(~'defn ~f-sym [& ~'args]
    (~(wrapped-sym f-sym)
     clojure.tools.deps.util.dir/*the-dir*
     ~'args)))

(defn pod-apply-with-dir-form [s args-sym]
  (assert (symbol? s) (str s))
  (assert (symbol? args-sym) (str args-sym)) ; no need to let bind
  `(binding [clojure.tools.deps.util.dir/*the-dir*
             (clojure.tools.deps.util.dir/canonicalize (first ~args-sym))]
     (debug :dir (clojure.tools.deps.util.dir/canonicalize (first ~args-sym)))
     (apply ~s (second ~args-sym))))

(defn dispatch* [sym args]
  (let [args-sym (gensym "args")
        invoke-with-dir-var-syms
        (reduce
         into []
         [(ns-public-fq-syms 'borkdude.tdn.bbuild)
          (ns-public-fq-syms 'clojure.tools.deps)
          (ns-public-fq-syms 'clojure.tools.deps.util.dir)
          (ns-public-fq-syms 'clojure.tools.deps.util.io)
          (ns-public-fq-syms 'clojure.tools.deps.util.maven)])]
    `(let [~args-sym ~args
           sym#      ~sym]
       (case sym#
         ~@(reduce
            (fn [forms s]
              (into forms
                    [(wrapped-sym s) (pod-apply-with-dir-form s args-sym)]))
            []
            invoke-with-dir-var-syms)
         (throw (ex-info (str "Unknown function: " sym#) {}))))))

(defmacro dispatch [sym args]
  (dispatch* sym args))

(defn invoke [msg]
  (let [var-sym (some->> (get msg "var") read-symbol)
        _       (debug :invoke :var var-sym)
        args    (some-> (get msg "args") read-payload)]
    (debug :invoke :args args)
    (dispatch var-sym args)))

;;; pod protocol

(defn pr-form [form]
  (binding [*print-meta* true]
    (pr-str (vary-meta form dissoc :line :column))))

(defn var-map
  [sym]
  {:name sym})

(defn code-map
  [sym form]
  {:name sym
   :code form})

(defn var-maps [syms]
  (->> syms
       sort
       (mapv var-map)))

(defn wrapped-var-maps [syms]
  (reduce
   (fn [res s]
     (into res
           [(var-map (wrapped-sym s))
            (code-map s (pr-form
                         (client-invoke-with-dir-form s)))]))
   []
   syms))

(defn public-var-maps [ns-sym]
  (-> ns-sym
      ns-public-syms
      var-maps))

(defn public-wrapped-var-maps [ns-sym]
  (-> ns-sym
      ns-public-syms
      wrapped-var-maps))

(def description
  {:format     :transit+json
   :namespaces [{:name "borkdude.tdn.pod"
                 :vars [{:name '-reg-transit-handlers
                         :code (reg-transit-handlers)} ]}
                ;; NOTE: order is important here.  We need to define *the-dir*
                ;; before anything that refers to it
                {:name 'clojure.tools.deps.util.dir
                 :vars (into
                        [{:name "*the-dir*"
                          :code (pr-form dir-var-form)}
                         {:name "with-dir"
                          :code with-dir-form-str}]
                        (public-wrapped-var-maps
                         'clojure.tools.deps.util.dir))}
                {:name 'clojure.tools.deps
                 :vars (public-wrapped-var-maps 'clojure.tools.deps)}
                {:name 'clojure.tools.deps.util.io
                 :vars (public-wrapped-var-maps
                        'clojure.tools.deps.util.io)}
                {:name 'clojure.tools.deps.util.maven
                 :vars (into
                        [{:name "standard-repos"
                          :code (pr-form standard-repos-form)}]
                        (public-wrapped-var-maps
                         'clojure.tools.deps.util.maven))}
                {:name 'clojure.tools.deps.util.session
                 :vars (into
                        [
                         #_{:name "session"
                            :code (pr-form session-cache-form)}]
                        (public-wrapped-var-maps
                         'clojure.tools.deps.util.session))}
                {:name 'borkdude.tdn.bbuild
                 :vars (public-wrapped-var-maps 'borkdude.tdn.bbuild)}]
   :opts       {:shutdown {}}})

(defn error-map
  [message data id]
  {"ex-message" message
   "ex-data"    (write-payload data)
   "id"         id
   "status"     ["done" "error"]})

(defmacro with-message [[id] & body]
  `(let [id# ~id]
     (try
       (let [val#     (do ~@body)
             transit# (write-transit val#)]
         (debug :transit transit#)
         (write-bencode
          {"value"  transit#
           "id"     id#
           "status" ["done"]}))
       (catch Exception e#
         (debug e#)
         (write-bencode
          (error-map
           (or (ex-message e#) "")
           (assoc (ex-data e#) :type (str (class e#)))
           id#))))))

(defn unknown-op
  [op id]
  (write-bencode
   {"ex-message" "Unknown op"
    "ex-data"    (pr-str {:op op})
    "id"         id
    "status"     ["done" "error"]}))

(defn pod [& _args]
  (loop []
    (when-let [msg (read-bencode)]
      (debug :msg msg)
      (let [op (some-> (get msg "op") read-keyword)
            id (or (some-> (get msg "id") read-string) "unknown")]
        (debug :op op :id id)
        (debug :user.home (System/getProperty "user.home"))
        (debug :user.dir (System/getProperty "user.dir"))
        (case op
          :describe (write-map description)
          :invoke   (with-message [id]
                      (invoke msg))
          :shutdown (System/exit 0)
          (unknown-op op id))
        (recur)))))
