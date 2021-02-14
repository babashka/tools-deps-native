# tools-deps-native

This is an experiment to compile tools.deps with GraalVM native-image.

- [x] resolve mvn deps
- [x] download mvn deps
- [x] resolve git deps
- [x] download git deps

## Run

``` shell
$ tools-deps-native '{:deps {babashka/fs {:git/url "https://github.com/babashka/fs.git" :sha "bc4bd8efe29e9000c941877b02584555f0874988"}}}'
Cloning: https://github.com/babashka/fs.git
Checking out: https://github.com/babashka/fs.git at bc4bd8efe29e9000c941877b02584555f0874988
"/Users/borkdude/.gitlibs/libs/babashka/fs/bc4bd8efe29e9000c941877b02584555f0874988/src:/Users/borkdude/.m2/repository/org/clojure/clojure/1.10.1/clojure-1.10.1.jar:/Users/borkdude/.m2/repository/org/clojure/core.specs.alpha/0.2.44/core.specs.alpha-0.2.44.jar:/Users/borkdude/.m2/repository/org/clojure/spec.alpha/0.2.176/spec.alpha-0.2.176.jar"
```

## Build

To compile this project, point `GRAALVM_HOME` at your GraalVM distribution and
then run `script/compile`.

The build requires
[`clojure`](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools)
to be installed.

After a successful build, there is a `tools-deps-native` binary which you can pass a deps.edn map.
