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
then run `script/compile`. Using GraalVM 21.0.0 Java 11 CE is recommended.

The build requires
[`clojure`](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools)
to be installed.

After a successful build, there is a `tools-deps-native` binary which you can pass a deps.edn map.

### Shell branch

The `master` branch, using the jgit dependency, has some [issues](#issues). The
`shell` branch avoids this by shelling out to git instead. It works like this:

There's a branch of tools.gitlibs which replaces jgit with shelling out to git.  See
https://github.com/clojure/tools.gitlibs/tree/shell

I installed that library locally by checking out the `shell` branch,
appending `shell` to the version in `pom.xml` and then installed it using
`mvn -Dmaven.test.skip=true clean install`.

After that you should be able to run `script/compile` and get the `tools-deps-native` binary as usual.

## Issues

This project should be considered experimental due to the following issues, which sometimes (indetermincally) occur. Any help triaging these issues is welcome.

### GraalVM build gets stuck after the line

  `[tools-deps-native:20554] setup: 2,391.24 ms, 0.93 GB` and just sits there
  without any output or CPU activity. Re-running the compile script often works.

### Started thread in image heap

```
Error: Detected a started Thread in the image heap. Threads running in the image generator are no longer running at image runtime.  To see how this object got instantiated use --trace-object-instantiation=java.lang.Thread. The object was probably created by a class initializer and is reachable from a static field. You can request class initialization at image runtime by using the option --initialize-at-run-time=<class-name>. Or you can write your own initialization methods and call them explicitly from your main entry point.
Detailed message:
Trace: Object was reached by
	reading field java.util.concurrent.locks.ReentrantReadWriteLock$Sync.firstReader of
		constant java.util.concurrent.locks.ReentrantReadWriteLock$NonfairSync@3e1d2943 reached by
	reading field java.util.concurrent.locks.ReentrantReadWriteLock.sync of
		constant java.util.concurrent.locks.ReentrantReadWriteLock@135fac58 reached by
	reading field clojure.lang.MultiFn.rw of
		constant clojure.lang.MultiFn@48e1d176 reached by
	reading field clojure.lang.Var.root of
		constant clojure.lang.Var@518f3dbc reached by
	scanning method clojure.core$print_throwable$print_via__7487.invoke(core_print.clj:510)
Call path from entry point to clojure.core$print_throwable$print_via__7487.invoke(Object):
	at clojure.core$print_throwable$print_via__7487.invoke(core_print.clj:509)
	at clojure.tools.logging.proxy$java.io.ByteArrayOutputStream$ff19274a.flush(Unknown Source)
	at java.io.PrintStream.flush(PrintStream.java:417)
	at com.oracle.svm.jni.functions.JNIFunctions.ExceptionDescribe(JNIFunctions.java:763)
	at com.oracle.svm.core.code.IsolateEnterStub.JNIFunctions_ExceptionDescribe_b5412f7570bccae90b000bc37855f00408b2ad73(generated:0)

com.oracle.svm.core.util.UserError$UserException: Detected a started Thread in the image heap. Threads running in the image generator are no longer running at image runtime.  To see how this object got instantiated use --trace-object-instantiation=java.lang.Thread. The object was probably created by a class initializer and is reachable from a static field. You can request class initialization at image runtime by using the option --initialize-at-run-time=<class-name>. Or you can write your own initialization methods and call them explicitly from your main entry point.
Detailed message:
Trace: Object was reached by
	reading field java.util.concurrent.locks.ReentrantReadWriteLock$Sync.firstReader of
		constant java.util.concurrent.locks.ReentrantReadWriteLock$NonfairSync@3e1d2943 reached by
	reading field java.util.concurrent.locks.ReentrantReadWriteLock.sync of
		constant java.util.concurrent.locks.ReentrantReadWriteLock@135fac58 reached by
	reading field clojure.lang.MultiFn.rw of
		constant clojure.lang.MultiFn@48e1d176 reached by
	reading field clojure.lang.Var.root of
		constant clojure.lang.Var@518f3dbc reached by
	scanning method clojure.core$print_throwable$print_via__7487.invoke(core_print.clj:510)
Call path from entry point to clojure.core$print_throwable$print_via__7487.invoke(Object):
	at clojure.core$print_throwable$print_via__7487.invoke(core_print.clj:509)
	at clojure.tools.logging.proxy$java.io.ByteArrayOutputStream$ff19274a.flush(Unknown Source)
	at java.io.PrintStream.flush(PrintStream.java:417)
	at com.oracle.svm.jni.functions.JNIFunctions.ExceptionDescribe(JNIFunctions.java:763)
	at com.oracle.svm.core.code.IsolateEnterStub.JNIFunctions_ExceptionDescribe_b5412f7570bccae90b000bc37855f00408b2ad73(generated:0)

	at com.oracle.svm.core.util.UserError.abort(UserError.java:82)
	at com.oracle.svm.hosted.FallbackFeature.reportAsFallback(FallbackFeature.java:233)
	at com.oracle.svm.hosted.NativeImageGenerator.runPointsToAnalysis(NativeImageGenerator.java:773)
	at com.oracle.svm.hosted.NativeImageGenerator.doRun(NativeImageGenerator.java:563)
	at com.oracle.svm.hosted.NativeImageGenerator.lambda$run$0(NativeImageGenerator.java:476)
	at java.base/java.util.concurrent.ForkJoinTask$AdaptedRunnableAction.exec(ForkJoinTask.java:1407)
	at java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:290)
	at java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1020)
	at java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1656)
	at java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1594)
	at java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:183)
Caused by: com.oracle.graal.pointsto.constraints.UnsupportedFeatureException: Detected a started Thread in the image heap. Threads running in the image generator are no longer running at image runtime.  To see how this object got instantiated use --trace-object-instantiation=java.lang.Thread. The object was probably created by a class initializer and is reachable from a static field. You can request class initialization at image runtime by using the option --initialize-at-run-time=<class-name>. Or you can write your own initialization methods and call them explicitly from your main entry point.
Detailed message:
Trace: Object was reached by
	reading field java.util.concurrent.locks.ReentrantReadWriteLock$Sync.firstReader of
		constant java.util.concurrent.locks.ReentrantReadWriteLock$NonfairSync@3e1d2943 reached by
	reading field java.util.concurrent.locks.ReentrantReadWriteLock.sync of
		constant java.util.concurrent.locks.ReentrantReadWriteLock@135fac58 reached by
	reading field clojure.lang.MultiFn.rw of
		constant clojure.lang.MultiFn@48e1d176 reached by
	reading field clojure.lang.Var.root of
		constant clojure.lang.Var@518f3dbc reached by
	scanning method clojure.core$print_throwable$print_via__7487.invoke(core_print.clj:510)
Call path from entry point to clojure.core$print_throwable$print_via__7487.invoke(Object):
	at clojure.core$print_throwable$print_via__7487.invoke(core_print.clj:509)
	at clojure.tools.logging.proxy$java.io.ByteArrayOutputStream$ff19274a.flush(Unknown Source)
	at java.io.PrintStream.flush(PrintStream.java:417)
	at com.oracle.svm.jni.functions.JNIFunctions.ExceptionDescribe(JNIFunctions.java:763)
	at com.oracle.svm.core.code.IsolateEnterStub.JNIFunctions_ExceptionDescribe_b5412f7570bccae90b000bc37855f00408b2ad73(generated:0)

	at com.oracle.graal.pointsto.constraints.UnsupportedFeatures.report(UnsupportedFeatures.java:126)
	at com.oracle.svm.hosted.NativeImageGenerator.runPointsToAnalysis(NativeImageGenerator.java:770)
	... 8 more
Error: Image build request failed with exit status 1
com.oracle.svm.driver.NativeImage$NativeImageError: Image build request failed with exit status 1
	at com.oracle.svm.driver.NativeImage.showError(NativeImage.java:1676)
	at com.oracle.svm.driver.NativeImage.build(NativeImage.java:1426)
	at com.oracle.svm.driver.NativeImage.performBuild(NativeImage.java:1387)
	at com.oracle.svm.driver.NativeImage.main(NativeImage.java:1374)
	at com.oracle.svm.driver.NativeImage$JDK9Plus.main(NativeImage.java:1858)
```
