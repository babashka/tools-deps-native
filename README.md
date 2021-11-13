# tools-deps-native

This is an [experiment](#issues) to compile tools.deps with GraalVM native-image.

- [x] resolve mvn deps
- [x] download mvn deps
- [x] resolve git deps
- [x] download git deps

## Status

This project is currently _very experimental_ and any functionality or API may
still change.

## Why

Not needing a JVM for dependency resolution and downloading can speed up things
(you don't pay the startup time). I might want to use this for [babashka.deps](https://book.babashka.org/#babashkadeps),
the namespace in babashka that downloads deps from clojars that can be used in
scripts, but it might also have other use cases.

The official Clojure CLI has classpath caching. This tool is about as fast as
that _without any_ caching.

## Install

A pre-release binaries for macOS and linux are available
[here](https://github.com/borkdude/tools-deps-native-experiment/releases/tag/0.0.1-SNAPSHOT).

You can also visit the CircleCI builds for more up to date binaries and download
them from the artifacts.

To build from source yourself: see [build](#build).

## Run

Run the CLI to get information about usage.

## Build

To compile this project, point `GRAALVM_HOME` at your GraalVM distribution and
then run `script/compile`. Using GraalVM 21.3.0 Java 11 CE is recommended.

The build requires
[`clojure`](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools)
to be installed.

After a successful build, there is a `tools-deps-native` binary which you can pass a deps.edn map.
