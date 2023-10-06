# Rodin B AST

This repository contains a standalone version of the Rodin AST. It is used within the ProB tool.

Almost all files in this repository are taken unmodified from the [rodincore/org.eventb.core.ast](https://sourceforge.net/p/rodin-b-sharp/rodincore/ci/master/tree/org.eventb.core.ast/) project.
Only two files from the original sources have been modified:

* .gitignore
* src/org/eventb/internal/core/ast/ASTPlugin.java

We also added the following files to support the Maven Central build/upload process:

* .gitlab-ci.yml
* README.md
* build.gradle
* gradle
* gradlew
* gradlew.bat
* gradle.properties.enc
* pubring.gpg.enc
* secring.gpg.enc
* settings.gradle
* update_sources.sh

## How to update this project to a new Rodin version

First, remove any local changes in your clone of this repo, to avoid possible conflicts or other issues:

```sh
$ git restore --staged --worktree . # DELETES ALL LOCAL CHANGES!
```

In a different directory, clone the rodincore repo (if you haven't already):

```sh
$ cd /some/work/directory
$ git clone "https://git.code.sf.net/p/rodin-b-sharp/rodincore" rodin-b-sharp-rodincore
$ cd rodin-b-sharp-rodincore
```

Check out the desired Rodin release, for example:

```sh
$ git switch --detach RodinCore/3.3
```

In this repo, run the `update_sources.sh` script and pass the path of the source org.eventb.core.ast project:

```sh
$ cd .../rodin_eventb_ast
$ ./update_sources.sh /some/work/directory/rodin-b-sharp-rodincore/org.eventb.core.ast
```

Update the version number in build.gradle to the appropriate version *with a SNAPSHOT suffix*, for example:

```groovy
project.version = "3.3.0-SNAPSHOT"
```

Add the version change and commit and push everything:

```sh
$ git add build.gradle
$ git commit -m "Update to Rodin 3.3 sources"
$ git push
```

Once you have confirmed that the new snapshot works, you can remove the -SNAPSHOT suffix and make a proper release to Maven Central.
