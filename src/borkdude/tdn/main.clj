(ns borkdude.tdn.main
  (:require [clojure.stacktrace :as stack]
            [clojure.java.io :as io]
            [clojure.tools.deps.alpha :as deps]
            [clojure.tools.deps.alpha.util.io :refer [printerrln]])
  (:import
    ;; maven-resolver-api
    [org.eclipse.aether RepositorySystem RepositorySystemSession DefaultRepositoryCache DefaultRepositorySystemSession ConfigurationProperties]
    [org.eclipse.aether.artifact Artifact DefaultArtifact]
    [org.eclipse.aether.repository LocalRepository Proxy RemoteRepository RemoteRepository$Builder]
    [org.eclipse.aether.graph Dependency Exclusion]
    [org.eclipse.aether.transfer TransferListener TransferEvent TransferResource]

    ;; maven-resolver-spi
    [org.eclipse.aether.spi.connector RepositoryConnectorFactory]
    [org.eclipse.aether.spi.connector.transport TransporterFactory]
    [org.eclipse.aether.spi.locator ServiceLocator]

    ;; maven-resolver-impl
    [org.eclipse.aether.impl DefaultServiceLocator]

    ;; maven-resolver-connector-basic
    [org.eclipse.aether.connector.basic BasicRepositoryConnectorFactory]

    ;; maven-resolver-transport-file
    [org.eclipse.aether.transport.file FileTransporterFactory]

    ;; maven-resolver-transport-http
    [org.eclipse.aether.transport.http HttpTransporterFactory]

    ;; maven-aether-provider
    [org.apache.maven.repository.internal MavenRepositorySystemUtils]

    ;; maven-resolver-util
    [org.eclipse.aether.util.repository AuthenticationBuilder DefaultProxySelector DefaultMirrorSelector]

    ;; maven-core
    [org.apache.maven.settings DefaultMavenSettingsBuilder Settings Server Mirror]

    ;; maven-settings-builder
    [org.apache.maven.settings.building DefaultSettingsBuilderFactory]

    ;; plexus-utils
    [org.codehaus.plexus.util.xml Xpp3Dom])
  (:gen-class))

(require 'clojure.tools.deps.alpha.extensions
         'clojure.tools.deps.alpha.extensions.deps
         'clojure.tools.deps.alpha.extensions.git
         'clojure.tools.deps.alpha.extensions.local
         'clojure.tools.deps.alpha.extensions.maven
         'clojure.tools.deps.alpha.extensions.pom)

(require '[clojure.edn :as edn]
         '[clojure.tools.deps.alpha.util.maven :as mvn]
         )

(set! *warn-on-reflection* true)

;; avoid null pointer
#_(mvn/make-system)

(defn the-locator-fn []
  (let [^DefaultServiceLocator loc
        (doto (MavenRepositorySystemUtils/newServiceLocator)
          (.addService RepositoryConnectorFactory BasicRepositoryConnectorFactory)
          (.addService TransporterFactory FileTransporterFactory)
          (.addService TransporterFactory HttpTransporterFactory))]
    (try
      (let [c (Class/forName "clojure.tools.deps.alpha.util.S3TransporterFactory")]
        (.addService loc TransporterFactory c))
      (catch ClassNotFoundException _
        (printerrln "Warning: failed to load the S3TransporterFactory class")
        loc))))

(def sentinel (Object.))

(defn make-system
  ^RepositorySystem []
  (prn :locator @clojure.tools.deps.alpha.util.maven/the-locator)
  (.getService ^ServiceLocator @clojure.tools.deps.alpha.util.maven/the-locator RepositorySystem))

(defn make-session
  ^RepositorySystemSession [^RepositorySystem system local-repo]
  (locking sentinel
    (prn :system system)
    (prn :local-repo local-repo))
  (let [system (make-system)
        _ (prn :sys2 system)
        session (MavenRepositorySystemUtils/newSession)
        local-repo-mgr (.newLocalRepositoryManager system session (mvn/make-local-repo local-repo))]
    (.setLocalRepositoryManager session local-repo-mgr)
    (.setTransferListener session mvn/console-listener)
    (.setCache session (DefaultRepositoryCache.))
    (doseq [^Server server (.getServers ^Settings (#'mvn/get-settings))]
      (mvn/add-server-config session server))
    session))

(defn -main [& args]
  (prn :make-system (make-system))
  (try
    (let [l (the-locator-fn)]
      (alter-var-root #'clojure.tools.deps.alpha.util.maven/make-session (constantly make-session))
      (alter-var-root #'clojure.tools.deps.alpha.util.maven/the-locator (constantly (delay (prn :delay!)
                                                                                           (prn :l l)
                                                                                           l))))
    (let [arg (or (first args) "{:deps {org.clojure/clojure {:mvn/version \"1.10.2\"}}}")
          edn-str (if (.exists (io/file arg))
                    (slurp arg)
                    arg)]
      (prn (-> (edn/read-string edn-str)
               (update :mvn/repos (fn [repos]
                                    (or repos
                                        {"central" {:url "https://repo1.maven.org/maven2/"}
                                         "clojars" {:url "https://repo.clojars.org/"}})))
               (deps/resolve-deps nil)
               (deps/make-classpath nil nil))))
    (catch Exception e
      (prn :exception e #_(.getName (class e)))
      (stack/print-stack-trace e))))
