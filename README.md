# tools-deps-native

This is an [experiment](#issues) to compile tools.deps with GraalVM native-image.

- [x] resolve mvn deps
- [x] download mvn deps
- [x] resolve git deps
- [x] download git deps

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

As of now the binary accepts a `deps.edn` literal map or file and returns the classpath as a string.

``` shell
$ ./tools-deps-native '{:deps {babashka/fs {:mvn/version "0.0.1"}}}'
Downloading: babashka/fs/0.0.1/fs-0.0.1.pom from clojars
Downloading: babashka/fs/0.0.1/fs-0.0.1.jar from clojars
"/Users/borkdude/.m2/repository/babashka/fs/0.0.1/fs-0.0.1.jar:/Users/borkdude/.m2/repository/org/clojure/clojure/1.9.0/clojure-1.9.0.jar:/Users/borkdude/.m2/repository/org/clojure/core.specs.alpha/0.1.24/core.specs.alpha-0.1.24.jar:/Users/borkdude/.m2/repository/org/clojure/spec.alpha/0.1.143/spec.alpha-0.1.143.jar"
```

``` shell
$ ./tools-deps-native '{:deps {babashka/fs {:git/url "https://github.com/babashka/fs.git" :sha "bc4bd8efe29e9000c941877b02584555f0874988"}}}'
Cloning: https://github.com/babashka/fs.git
Checking out: https://github.com/babashka/fs.git at bc4bd8efe29e9000c941877b02584555f0874988
"/Users/borkdude/.gitlibs/libs/babashka/fs/bc4bd8efe29e9000c941877b02584555f0874988/src:/Users/borkdude/.m2/repository/org/clojure/clojure/1.10.1/clojure-1.10.1.jar:/Users/borkdude/.m2/repository/org/clojure/core.specs.alpha/0.2.44/core.specs.alpha-0.2.44.jar:/Users/borkdude/.m2/repository/org/clojure/spec.alpha/0.2.176/spec.alpha-0.2.176.jar"
```

## Build

To compile this project, point `GRAALVM_HOME` at your GraalVM distribution and
then run `script/compile`. Using GraalVM 21.3.0 Java 11 CE is recommended.

The build requires
[`clojure`](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools)
to be installed.

After a successful build, there is a `tools-deps-native` binary which you can pass a deps.edn map.
