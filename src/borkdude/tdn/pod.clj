(ns borkdude.tdn.pod
  (:refer-clojure :exclude [read-string])
  (:require
   [clojure.edn :as edn]
   [clojure.tools.deps.alpha]
   [clojure.walk :as walk]
   [bencode.core :as bencode])
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

;;; Implementation

(defn ns-public-vars [ns-sym]
  (->> (the-ns ns-sym)
       ns-publics
       keys
       (mapv #(symbol (name ns-sym) (name %)))))

(defmacro dispatch-publics [sym args]
  (let [publics  (reduce
                  into []
                  [(ns-public-vars 'clojure.tools.deps.alpha)
                   (ns-public-vars 'clojure.tools.deps.alpha.util.dir)
                   (ns-public-vars 'clojure.tools.deps.alpha.util.maven)])
        args-sym (gensym "args")]
    `(let [~args-sym ~args
           sym#      ~sym]
       (case sym#
         ~@(reduce
            (fn [forms s]
              (into forms
                    [s `(apply ~s ~args-sym)]))
            []
            publics)
         (throw (ex-info (str "Unknown function: " sym#) {}))))))

(defn invoke [msg]
  (let [v    (some->> (get msg "var")
                      read-string
                      symbol
                                        ;(symbol "clojure.tools.deps.alpha")
                      #_resolve)
        args (some-> (get msg "args") read-string edn/read-string)]
    (debug :invoke :var v :args args)
    (dispatch-publics v args)))

(defn public-var-maps [ns-sym]
  (->> (ns-publics (the-ns ns-sym))
       keys
       sort
       (mapv (partial hash-map :name))))

;;; pod protocol

(def description
  {:format     :edn
   :namespaces [{:name 'clojure.tools.deps.alpha
                 :vars (public-var-maps 'clojure.tools.deps.alpha)}
                {:name 'clojure.tools.deps.alpha.util.dir
                 :vars (public-var-maps 'clojure.tools.deps.alpha.util.dir)}
                {:name 'clojure.tools.deps.alpha.util.maven
                 :vars (public-var-maps 'clojure.tools.deps.alpha.util.maven)}]
   :opts       {:shutdown {}}})

(defn error-map
  [message data id]
  {"ex-message" message
   "ex-data"    (pr-str data)
   "id"         id
   "status"     ["done" "error"]})

(defmacro with-message [[id] & body]
  `(let [id# ~id]
     (try
       (write-bencode
        {"value"  (pr-str ~@body)
         "id"     id#
         "status" ["done"]})
       (catch Exception e#
         (debug (str e#))
         (write-bencode
          (error-map
           (ex-message e#)
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
        (case op
          :describe (write-map description)
          :invoke   (with-message [id]
                      (invoke msg))
          :shutdown (System/exit 0)
          (unknown-op op id))
        (recur)))))
