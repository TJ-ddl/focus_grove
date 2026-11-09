# Focus Grove — 维护与推送说明

## 推送前必查

- 绝不提交：`keystore/`（`forest.jks`、`keystore.properties`）、`local.properties`、`app/build/`、`.gradle/`、`.kotlin/`、`dist/`。
- 提交前运行 `git status --short` 核对。出现签名文件或本机路径时先停下报告，不要自行提交。
- APK 只发 GitHub Releases，不进仓库。
- 敏感文件自查：

```bash
git ls-files | grep -E 'keystore/|\.jks$|keystore\.properties|local\.properties|^dist/|\.apk$'
```

无输出才可推送。

## 命名与商标

- 对外名称统一用 **Focus Grove（林地）**。
- README、文档、UI 文案不要出现「Forest」「森林」「专注森林」，也不要写「Forest 风格」这类比较性描述。
- 内部包名 / 类名保留 `com.personal.forest`、`ForestApp`、`ForestTheme` 等，不要为了文案改名（牵动大量文件）；新增用户可见文字用「林地」。
- 插画全部由 `tools/gen_art.py` 生成。不要引入来源不明的图片、字体或代码。

## 构建

- 需要 JDK 21、Android SDK、Gradle 8.9，统一用 `build.sh`：

```bash
bash build.sh :app:testDebugUnitTest
bash build.sh :app:assembleRelease
```

- 产物：`app/build/outputs/apk/release/app-release.apk`
- 签名文件只在本地，务必自行备份；签名丢失只能卸载重装，会丢数据。

## 推送流程

1. `git status --short` 检查变更，确认没有敏感文件
2. 改动涉及逻辑时先跑 `bash build.sh :app:testDebugUnitTest`
3. `git add -A && git commit -m "说明"`
4. `git push`

仓库地址：https://github.com/TJ-ddl/focus_grove

如果仓库在 GitHub 上再次改名，记得同步 `git remote set-url origin <新地址>`。
