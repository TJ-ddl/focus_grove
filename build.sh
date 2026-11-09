#!/usr/bin/env bash
# Build helper: uses the locally installed JDK 21, Android SDK and Gradle.
# The user home path contains an apostrophe, which breaks Gradle's test-worker
# classpath, so builds go through a junction without the apostrophe.
set -e
export JAVA_HOME="/c/Program Files/Microsoft/jdk-21.0.6.7-hotspot"
export ANDROID_HOME="$HOME/android-sdk"
if [ -d /c/gradlehome ]; then
    export GRADLE_USER_HOME='C:\gradlehome'
fi
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -d /c/forestproj ]; then
    PROJECT_DIR='C:\forestproj'
fi
exec "$HOME/tools/gradle-8.9/bin/gradle" --project-dir "$PROJECT_DIR" "$@"
