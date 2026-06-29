# androidPlate — 车牌识别 Android 应用

## 项目概述

一个基于 Android 原生开发的车牌识别应用，使用手机摄像头实时捕捉车牌图像，通过集成的车牌识别 SDK 识别车牌号码并展示结果。

| 项目         | 内容                              |
| ------------ | --------------------------------- |
| **包名**     | `com.zkc.androidplate`            |
| **技术栈**   | Kotlin + Jetpack Compose + CameraX |
| **最低 SDK** | API 24 (Android 7.0)              |
| **目标 SDK** | API 36 (Android 16)               |
| **编译 SDK** | API 36                            |
| **Gradle**   | 9.4.1                             |
| **AGP**      | 9.2.1                             |
| **Kotlin**   | 2.2.10                            |
| **Compose**  | BOM 2026.02.01                    |
| **版本号**   | `1.0.0` (versionCode 100)        |

---

## 项目结构

```
f:\TEMP\androidPlate/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/zkc/androidplate/
│   │   │   │   ├── MainActivity.kt          # 主界面 + 摄像头 + 识别逻辑
│   │   │   │   └── ui/theme/
│   │   │   │       ├── Color.kt             # 主题色定义
│   │   │   │       ├── Theme.kt             # Material3 主题配置
│   │   │   │       └── Type.kt              # 字体排版定义
│   │   │   ├── res/                          # 资源文件（图标、字符串、主题、manifest）
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle.kts                  # 模块构建配置
├── gradle/
│   ├── wrapper/
│   │   ├── gradle-wrapper.jar
│   │   └── gradle-wrapper.properties
│   ├── libs.versions.toml                    # 版本目录（统一管理依赖版本）
│   └── gradle-daemon-jvm.properties
├── androidplate.jks                          # 应用签名文件
├── build.gradle.kts                          # 顶层构建配置
├── settings.gradle.kts                       # 项目设置
├── gradle.properties                         # Gradle 属性配置
├── gradlew / gradlew.bat                     # Gradle Wrapper 脚本
├── .gitignore
└── PROJECT_DOC.md                            # 本文件
```

---

## 技术架构

### UI 层

- 纯 **Jetpack Compose** 构建，无 XML 布局
- 使用 **Material 3** 组件（Card、Text、Button）
- 深色主题 (`#121212` 背景)，简洁风格
- 支持 Android 12+ 动态取色 (Dynamic Color)

### 摄像头层

- 使用 **CameraX** (1.3.4) 库
  - `Preview` — 实时预览画面
  - `ImageAnalysis` — 帧分析（分辨率 640x480，`STRATEGY_KEEP_ONLY_LATEST`）
  - `CameraSelector.DEFAULT_BACK_CAMERA` — 后置摄像头
- 通过 `PreviewView` (AndroidView) 嵌入 Compose 界面
- 帧回调执行在单线程 Executor 上

### 图像处理管线

```
ImageProxy (YUV_420_888)
    │
    ▼
yuv420888ToNv21() → 手动转为 NV21 字节数组
    │
    ▼
imageProxyToBitmap() → YuvImage → JPEG → BitmapFactory → Bitmap
    │
    ▼
rotateBitmap() → 根据摄像头旋转角度矫正方向（仅用于预览显示）
    │
    ▼
PlateRecognizer.recognize() → 返回 List<PlateResult>（含车牌号、置信度、类型、坐标）
```

### 车牌识别

- 使用 **`com.github.linjunbin0101:plate-sdk:1.0.1`**（JitPack）
- 关键类：`com.zkc.plate.PlateRecognizer`
  - `PlateRecognizer.init(context, config)` — 初始化（加载模型），支持 `PlateConfig` 配置
  - `PlateRecognizer.getInstance()` — 获取单例
  - `.recognize(bitmap)` — 识别车牌，返回 `List<PlateResult>`
    - `PlateResult.number` — 车牌号（如 "粤A12345"）
    - `PlateResult.confidence` — 置信度（0.0 ~ 1.0）
    - `PlateResult.type` — 车牌类型编码
    - `PlateResult.x1, y1, x2, y2` — 车牌矩形框坐标
- SDK 默认启用旋转重试（`enableRotationRetry = true`），传入任意方向 bitmap 均可识别
- 模型文件由 SDK 内部管理加载

---

## 界面流程

```
启动应用
    │
    ▼
检查摄像头权限
    ├── 未授权 → 请求权限 → 授权 → 初始化 PlateRecognizer
    └── 已授权 → 初始化 PlateRecognizer
    │
    ▼
CameraPreview + 绿色车牌引导框
    │
    ▼
点击"拍照识别"按钮
    │
    ▼
ImageAnalysis 捕获下一帧
    │
    ▼
转为 Bitmap → 矫正方向 → PlateRecognizer.recognize()
    │
    ├── 识别成功 → 显示车牌号（绿色卡片）
    └── 识别失败 → 显示"未识别到车牌"（灰色卡片）
    │
    ▼
点击"重新拍照" → 回到实时预览
```

---

## 当前构建状态

当前代码状况：

- ☑ Gradle 配置完整（版本目录 + 签名 + JVM 参数）
- ☑ 所有依赖声明正确（未见未解析引用）
- ☑ 代码语法正确、逻辑完整
- ☑ 签名文件已存在，签名密码与包名统一为 `androidplate`
- ⬜ 尚未执行实际构建验证

---

## 注意事项

1. **首次构建**：由于依赖了 JitPack 上的 `plate-sdk`，需要确保网络可访问 JitPack
2. **权限**：应用需要 `CAMERA` 权限（必需）以及 `READ/WRITE_EXTERNAL_STORAGE`（仅 Android 12 以下）
3. **签名**：`androidplate.jks` 密码 `androidplate`，debug 和 release 均使用同一签名