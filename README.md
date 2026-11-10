# Focus Grove（林地）

一个仅在本人手机上自用的本地专注计时应用（vivo S50 / OriginOS）。纯本地运行，不申请联网权限，数据只保存在应用私有目录。

界面、文案和插画均为原创，插画由 `tools/gen_art.py` 程序生成，可复现。

## 功能

- 专注计时 10–120 分钟，小树随进度呈现三个阶段（幼苗 / 小树 / 成树）
- 熄屏不计为离开；切换到其他应用则本次专注失败；可选「严格模式」与专注白名单，借助使用情况权限做更严格的判定
- 9 个树种、金币与解锁、按日期日历回看的林地、枯树铲除（失败记录仍保留）
- 统计（7 天、标签、连续天数）、标签、8 个成就、桌面小组件（今日专注 + 快速开始）
- 数据以 JSON 本地存储，支持导出 / 导入备份

## 目录

```
app/src/main/java/
  MainActivity.kt            入口
  data/                      模型、存储、经济规则、成就、树种
  session/                   前台服务、计时与离开判定
  ui/                        Compose 界面（种树 / 林地 / 统计 / 我的）
  widget/                    桌面小组件
app/src/main/assets/species.json   树种定义（加树 = 加一条 + 3 张图）
app/src/main/res/drawable-nodpi/   树插画 tree_<id>_<1|2|3>.webp
tools/gen_art.py             重绘插画（Pillow，可复现）
```

签名文件和本机配置只在本地保留，不提交到仓库。

## 构建

需要 JDK 21、Android SDK、Gradle 8.9。`build.sh` 默认使用本机已安装的 JDK / SDK / Gradle，路径可在脚本顶部按需调整。

```bash
bash build.sh :app:testDebugUnitTest
bash build.sh :app:assembleRelease
```

产物：`app/build/outputs/apk/release/app-release.apk`

重绘插画：

```bash
python tools/gen_art.py
```

## 安装

1. 把 APK 拷到手机（数据线 / 微信文件传输均可）
2. 点击安装，允许「安装未知来源应用」
3. 首次打开授予通知权限
4. 到 OriginOS 设置里给「林地」：允许自启动、耗电不限制（否则前台服务可能被清）
5. 严格模式：设置 → 应用 → 特殊权限 → 使用情况访问 → 允许「林地」

以后更新：保持同一签名覆盖安装，数据不会丢。签名文件请自行备份，丢失后只能卸载重装，会丢数据。

## 数据备份

「我的」→ 设置 → 导出数据，会生成一个 JSON。换机或重装后用导入恢复。数据文件位于应用私有目录 `files/data.json`（含 `.bak` 备份）。
