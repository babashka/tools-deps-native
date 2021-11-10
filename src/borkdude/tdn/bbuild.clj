(ns borkdude.tdn.bbuild
  "Helpers for tools.bbuild"
  (:import
   [org.eclipse.aether.artifact DefaultArtifact]
   [org.eclipse.aether.installation InstallRequest]))


(defn default-artifact
  [group-id artifact-id classifier ftype path]
  (DefaultArtifact. group-id artifact-id classifier ftype path))

(defn install-request []
  (InstallRequest.))
