#!/usr/bin/env bash
# Build helper: uses the locally installed JDK 21, Android SDK and Gradle.
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

# 本机用户名含撇号（t'd'g）。Gradle 把测试 worker 的 classpath 写进 Java @argfile，
# argfile 里撇号被当作引号吃掉，凡在真实路径下的 classpath 条目全部失效，
# 单元测试必然 ClassNotFoundException。junction 也救不了（Gradle 会解析回真实路径），
# 所以测试改 robocopy 一份到无撇号目录再跑；assemble 等任务不受影响，原样执行。
for arg in "$@"; do
    case "$arg" in
        *test*|*Test*)
            MIRROR='C:\forest_head'
            echo "==> 单元测试在无撇号镜像目录运行: $MIRROR"
            # // 前缀：阻止 Git Bash 把 robocopy 的 /flag 改写成 Unix 路径
            robocopy "$(cygpath -w "$PROJECT_DIR")" "$MIRROR" //MIR //XD .git build .gradle .kotlin keystore dist //NFL //NDL //NJH //NJS //NP || true
            exec "$HOME/tools/gradle-8.9/bin/gradle" --project-dir "$MIRROR" "$@"
            ;;
    esac
done
exec "$HOME/tools/gradle-8.9/bin/gradle" --project-dir "$PROJECT_DIR" "$@"
