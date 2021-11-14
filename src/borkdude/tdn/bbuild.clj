(ns borkdude.tdn.bbuild
  "Helpers for tools.bbuild"
  (:require [clojure.java.io :as jio]
            [clojure.tools.deps.alpha.util.maven :as mvn])
  (:import
   [org.eclipse.aether.artifact DefaultArtifact]
   [org.eclipse.aether.installation InstallRequest]))

(set! *warn-on-reflection* true)

(defn- resolve-path
  "Adapted from tools build api"
  ^java.io.File [project-root path]
  (let [path-file (jio/file path)]
    (if (.isAbsolute path-file)
      ;; absolute, ignore root
      path-file
      ;; relative to *project-root*
      (jio/file project-root path-file))))

(defn install
  "Adapted from tools build install task"
  [{:keys [project-root basis lib classifier version jar-file class-dir]}]
  (let [{:mvn/keys [local-repo]} basis
        group-id (namespace lib)
        artifact-id (name lib)
        jar-file-file (resolve-path project-root jar-file)
        pom-dir (jio/file (resolve-path project-root class-dir) "META-INF" "maven" group-id artifact-id)
        pom (jio/file pom-dir "pom.xml")
        system (mvn/make-system)
        session (mvn/make-session system (or local-repo @mvn/cached-local-repo))
        jar-artifact (.setFile (DefaultArtifact. group-id artifact-id classifier "jar" version) jar-file-file)
        artifacts (cond-> [jar-artifact]
                    (and pom-dir (.exists pom)) (conj (.setFile (DefaultArtifact. group-id artifact-id classifier "pom" version) pom)))
        install-request (.setArtifacts (InstallRequest.) artifacts)]
    (.install system session install-request)
    nil))
