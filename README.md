# tools-deps-native

This is an experiment to compile tools.deps with GraalVM native-image.

## Build

To compile this project, point `GRAALVM_HOME` at your GraalVM distribution and
then run `script/compile`.

The build requires
[`clojure`](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools)
to be installed.

After a successful build, there is a `tools-deps-native` binary which you can pass a deps.edn map.

### Shell-git branch

The `master` branch take a long time to compile when using the jgit
dependency. The `shell-git` branch avoids this as follows:

There's a branch of tools.gitlibs which replaces jgit with shelling out to git.  See
https://github.com/ghadishayban/tools.gitlibs/tree/shell-git.

I installed that library locally by checking out the `shell-git` branch,
appending `shell-git` to the version in `pom.xml` and then installed it using
`mvn -Dmaven.test.skip=true clean install`.

Then I depended on that library in a checkout of tools.deps.alpha, I also
changed that version by appending `shell-git` and also installed that using `mvn`.

That installation is used in the `deps.edn` of this project.
