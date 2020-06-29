# tools-deps-native

This is an experiment to compile tools.deps with GraalVM native-image.

## Build

To compile this project, point `GRAALVM_HOME` at your GraalVM distribution and
then run `script/compile`.

The build requires
[`clojure`](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools)
to be installed.

## Findings

There is a problem with jgit at the moment. The output I got was similar to this one: https://bugs.eclipse.org/bugs/show_bug.cgi?id=546175
Quarkus has a version of jgit that does work with GraalVM, so probably we can borrow their ideas: https://quarkus.io/guides/jgit

Let's move on and skip over this problem for now. 
There's a branch of tools.gitlibs which replaces jgit with shelling out to git.  See
https://github.com/ghadishayban/tools.gitlibs/tree/shell-git.

I installed that library locally by checking out the `shell-git` branch,
appending `shell-git` to the version in `pom.xml` and then installed it using
`mvn -Dmaven.test.skip=true clean install`.

Then I depended on that library in a checkout of tools.deps.alpha, I also
changed that version by appending `shell-git` and also installed that using
`mvn`. That installation is used in the `deps.edn` of this project.

Currently blocked on this issue:

```
Error: com.oracle.graal.pointsto.constraints.UnresolvedElementException: Discovered unresolved type during parsing: org.apache.maven.project.ReactorModelPool. To diagnose the issue you can use the --allow-incomplete-classpath option. The missing type is then reported at run time when it is accessed the first time.
Detailed message:
Trace:
	at parsing clojure.tools.deps.alpha.extensions.pom$model_resolver.invokeStatic(pom.clj:51)
Call path from entry point to clojure.tools.deps.alpha.extensions.pom$model_resolver.invokeStatic(Object):
	at clojure.tools.deps.alpha.extensions.pom$model_resolver.invokeStatic(pom.clj:41)
	at clojure.tools.deps.alpha.extensions.pom$model_resolver.invoke(pom.clj:41)
	at clojure.core.proxy$clojure.lang.APersistentMap$ff19274a.hashCode(Unknown Source)
	at java.util.concurrent.ConcurrentHashMap.get(ConcurrentHashMap.java:936)
	at java.util.Properties.getProperty(Properties.java:1125)
	at com.oracle.svm.core.jdk.SystemPropertiesSupport.getProperty(SystemPropertiesSupport.java:144)
	at com.oracle.svm.core.jdk.Target_java_lang_System.getProperty(JavaLangSubstitutions.java:345)
	at com.oracle.svm.jni.JNIJavaCallWrappers.jniInvoke_VA_LIST:Ljava_lang_System_2_0002egetProperty_00028Ljava_lang_String_2_00029Ljava_lang_String_2(generated:0)
```

I then used `--allow-incomplete-classpath` just to see how far it gets.  I now
get a NPE in `util/maven.clj` on the line `local-repo-mgr
(.newLocalRepositoryManager system session local-repo)` where `system` appears
to be `null`. Might be related to the `--allow-incomplete-classpath`.
