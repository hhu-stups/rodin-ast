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
