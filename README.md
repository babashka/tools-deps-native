# tools-deps-native

This is an experiment to compile tools.deps with GraalVM native-image.

## Build

To compile this project, point `GRAALVM_HOME` at your GraalVM distribution and
then run `script/compile`.

The build requires
[`clojure`](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools)
to be installed.

Currently the build stalls after a while:

```
[tools-deps-native:66284]    classlist:   1,975.99 ms,  1.19 GB
[tools-deps-native:66284]        (cap):   1,513.17 ms,  1.19 GB
[tools-deps-native:66284]        setup:   2,626.32 ms,  1.19 GB
^C
```

Welcome to help.
