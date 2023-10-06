#!/bin/sh

set -e -u

if [ "$#" -ne 1 ]
then
	echo "$0: expected exactly 1 argument, not $#" >&2
	echo "usage: $0 RODINCORE_AST_PATH" >&2
	exit 2
fi

original_path="$1"
own_sources_path="$(dirname "$0")"

if [ -z "${own_sources_path}" ]
then
	echo "$0: failed to determine destination path"
	exit 1
fi

git rm -r \
	"${own_sources_path}/build-gen.xml" \
	"${own_sources_path}/build.properties" \
	"${own_sources_path}/customBuildCallbacks.xml" \
	"${own_sources_path}/epl-v10.html" \
	"${own_sources_path}/META-INF" \
	"${own_sources_path}/notice.html" \
	"${own_sources_path}/plugin.properties" \
	"${own_sources_path}/src" \
	"${own_sources_path}/tom" \
	"${own_sources_path}/tools"

cp -R \
	"${original_path}/build-gen.xml" \
	"${original_path}/build.properties" \
	"${original_path}/customBuildCallbacks.xml" \
	"${original_path}/epl-v10.html" \
	"${original_path}/META-INF" \
	"${original_path}/notice.html" \
	"${original_path}/plugin.properties" \
	"${original_path}/src" \
	"${original_path}/tom" \
	"${original_path}/tools" \
	"${own_sources_path}"

# We replaced this file - keep our version instead of the original.
git restore --staged --worktree "${own_sources_path}/src/org/eventb/internal/core/ast/ASTPlugin.java"

git add \
	"${own_sources_path}/build-gen.xml" \
	"${own_sources_path}/build.properties" \
	"${own_sources_path}/customBuildCallbacks.xml" \
	"${own_sources_path}/epl-v10.html" \
	"${own_sources_path}/META-INF" \
	"${own_sources_path}/notice.html" \
	"${own_sources_path}/plugin.properties" \
	"${own_sources_path}/src" \
	"${own_sources_path}/tom" \
	"${own_sources_path}/tools"

echo "Successfully updated with sources from ${original_path}"
echo "Remember to update the version number in build.gradle!"
