# ZCode

[English](README.md) | [简体中文](README.zh-CN.md)

[ZCode](https://zcode.z.ai) Remote Control 的安卓客户端。扫描 ZCode 桌面端弹出的二维码，即可在手机上连接到你的桌面窗口——查看和切换工作区与任务、跟进 Agent 进度、随时下达后续指令。连接本质上是一个网址，所以任意网页也可以正常打开。

> Remote Control 用于把当前 ZCode 桌面窗口临时开放给手机访问；指令由桌面端背后的机器执行，手机只是控制端。详见[官方文档](https://zcode.z.ai/cn/docs/remote-control)。

## 功能

- 📷 **扫码接入**：扫 ZCode 桌面端 Remote Control 弹窗中的二维码完成配置（也支持手动输入）
- 🖥 **全屏沉浸**：隐藏状态栏 / 导航栏，无任何浏览器 UI
- 🔒 **设置持久化**：URL 保存后，之后启动直接进入网页
- ✋ **长按屏幕**随时重新打开设置页修改 URL
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
