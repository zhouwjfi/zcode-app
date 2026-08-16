# ZCode

[English](README.md) | [简体中文](README.zh-CN.md)

把任意网站变成全屏 Android App 的 WebView 壳（WebView wrapper）。网站**不需要支持 PWA**。

## 功能

- 📷 **扫码配置**：首次启动扫二维码即可设置 URL（也可手动输入）
- 🖥 **全屏沉浸**：隐藏状态栏 / 导航栏，无任何浏览器 UI
- 🔒 **设置持久化**：URL 保存后，之后启动直接进入网页
- ✋ **长按屏幕**随时重新打开设置页修改 URL
- 🔙 返回键 = 网页后退
- ✨ 品牌启动屏（每次冷启动一次，含入场动画）
- Cookie / localStorage / JS 全支持；`tel:`、`mailto:` 等外部链接交给系统
- 允许 HTTP 明文，内网地址可用（可在 Manifest 关闭 `usesCleartextTraffic`）

## 构建

```bash
gradle wrapper && ./gradlew assembleDebug
```

或用 Android Studio 直接打开本目录。

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`

## 安装

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 自定义

| 想改什么 | 位置 |
|---|---|
| 应用名 | `app/src/main/AndroidManifest.xml` 的 `android:label` |
| 包名 | `app/build.gradle.kts` 的 `applicationId` |
| 启动屏文案/邮箱 | `app/src/main/res/layout/activity_main.xml`（splashScreen 部分） |
| 主题色 | `res/values/themes.xml`、`res/drawable/bg_btn_*.xml` |
| 图标 | 替换 `res/mipmap-*/ic_launcher.png`（1024×1024 原图重新缩放） |

## License

MIT
