# DocScan - 智能文档扫描

一款开源的 Android 文档扫描应用，支持自动边缘检测、透视校正、多种图像滤镜和 PDF 生成。

## 功能特性

- **相机扫描** - 使用 CameraX 实现实时预览和高画质拍照
- **自动边缘检测** - 基于 OpenCV 的文档边缘检测与透视校正
- **多种滤镜** - 原图、灰度、黑白、锐化、自动增强（CLAHE）
- **PDF 生成** - 将多页扫描内容生成标准 A4 尺寸 PDF
- **文档管理** - 查看历史文档、删除、分享 PDF
- **闪光灯控制** - 拍照时支持闪光灯开关
- **多页扫描** - 支持连续扫描多页合并为一个文档

## 技术栈

| 技术 | 用途 |
|------|------|
| Kotlin | 开发语言 |
| CameraX | 相机预览与拍照 |
| OpenCV | 图像处理（边缘检测、透视变换、滤镜） |
| AndroidX PDF | PDF 文档生成 |
| Material Design 3 | UI 组件与主题 |
| Coroutines | 异步处理 |
| Gson | 数据持久化 |

## 系统要求

- Android 7.0 (API 24) 及以上
- 相机硬件

## 构建与运行

### 环境准备

1. 安装 [Android Studio](https://developer.android.com/studio)
2. 确保已安装 Android SDK，compileSdk 34
3. NDK（OpenCV 需要）

### 构建步骤

```bash
# 克隆项目
git clone https://github.com/your-username/DocScan.git
cd DocScan

# 使用 Gradle 构建
./gradlew assembleDebug

# 或直接在 Android Studio 中打开项目并运行
```

## 项目结构

```
app/src/main/java/com/docscan/app/
├── DocScanApp.kt              # Application 入口，初始化 OpenCV
├── camera/
│   ├── CameraManager.kt       # 相机管理（CameraX 封装）
│   └── ScannerActivity.kt     # 扫描界面（拍照、闪光灯、多页）
├── model/
│   └── ScanDocument.kt        # 数据模型（文档、页面、滤镜）
├── processing/
│   ├── ImageProcessor.kt      # 图像处理（OpenCV 滤镜、边缘检测、透视变换）
│   └── PdfGenerator.kt        # PDF 生成
├── ui/
│   ├── MainActivity.kt        # 主界面（文档列表）
│   ├── DocumentAdapter.kt     # 文档列表适配器
│   ├── PreviewActivity.kt     # 预览与滤镜选择
│   ├── PdfViewerActivity.kt   # 文档详情与页面浏览
│   └── PageAdapter.kt         # 页面列表适配器
└── utils/
    └── DocumentStorage.kt     # 本地存储管理
```

## 截图

<p>
  <img src="screenshots/main.png" width="200" alt="主页">
  <img src="screenshots/scanner.png" width="200" alt="扫描">
  <img src="screenshots/preview.png" width="200" alt="预览">
  <img src="screenshots/detail.png" width="200" alt="详情">
</p>

## License

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！