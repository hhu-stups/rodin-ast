# Rodin B AST

[![Build Status](https://travis-ci.org/hhu-stups/rodin-ast.svg?branch=master)](https://travis-ci.org/hhu-stups/rodin-ast)

This repository contains a standalone version of the Rodin AST. It is used within the ProB tool.

No original file is changed, but we added:
* README.md
* build.gradle
* .travis.yml
* gradle
* gradlew
* gradlew.bat
* gradle.properties.enc
* pubring.gpg.enc
* secring.gpg.enc
* settings.gradle

Note to future Jens:
After updating the sources the ```gradle patch``` task must be run. This modifies the sources. **Do not run the task multiple times!**
