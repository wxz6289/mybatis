# Android 内置浏览器应用

一个简单的 Android 应用，内置浏览器功能，启动时自动访问指定的网址。

## 应用信息

- **应用名称**: 内置浏览器
- **包名**: com.example.androidbrowser
- **目标URL**: https://192.168.31.3:8448
- **最小 Android 版本**: 5.0 (API 21)
- **目标 Android 版本**: 13 (API 33)

## 功能特性

1. **内置浏览器**: 使用 Android WebView 组件
2. **自动导航**: 启动时自动访问指定网址
3. **进度显示**: 页面加载时显示进度条
4. **返回导航**: 支持返回键浏览历史
5. **全屏体验**: 无状态栏的沉浸式体验
6. **网络权限**: 自动请求网络访问权限

## 项目结构

```
AndroidBrowser/
├── app/
│   ├── build.gradle                    # 应用级构建配置
│   ├── proguard-rules.pro             # ProGuard 混淆规则
│   └── src/main/
│       ├── AndroidManifest.xml        # 应用清单文件
│       ├── java/com/example/androidbrowser/
│       │   └── MainActivity.java      # 主活动类
│       └── res/
│           ├── layout/
│           │   └── activity_main.xml  # 主界面布局
│           ├── values/
│           │   ├── strings.xml        # 字符串资源
│           │   └── themes.xml         # 主题样式
│           └── xml/
│               ├── backup_rules.xml   # 备份规则
│               └── data_extraction_rules.xml # 数据提取规则
├── gradle/wrapper/                     # Gradle Wrapper
├── build.gradle                        # 项目级构建配置
├── settings.gradle                     # Gradle 设置
├── gradle.properties                   # Gradle 属性
├── gradlew                            # Gradle Wrapper 脚本 (Unix)
├── build.sh                          # 打包脚本
└── README.md                          # 项目说明
```

## 构建和安装

### 前提条件

1. **Android SDK**: 确保已安装 Android SDK
2. **Java**: 需要 Java 8 或更高版本
3. **环境变量**: 设置 `ANDROID_HOME` 环境变量

```bash
export ANDROID_HOME=/path/to/your/android/sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

### 使用打包脚本构建

1. 进入项目目录：
```bash
cd AndroidBrowser
```

2. 给打包脚本添加执行权限：
```bash
chmod +x build.sh
```

3. 运行打包脚本：
```bash
./build.sh
```

脚本会自动：
- 检查环境依赖
- 清理旧的构建文件
- 构建 Debug APK
- 可选择构建 Release APK
- 可选择安装到连接的设备

### 手动构建

1. 构建 Debug APK：
```bash
./gradlew assembleDebug
```

2. 构建 Release APK：
```bash
./gradlew assembleRelease
```

3. 安装到设备：
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 配置说明

### 修改目标URL

在 `MainActivity.java` 中修改 `TARGET_URL` 常量：

```java
private static final String TARGET_URL = "https://your-target-url.com";
```

### 修改应用名称

在 `app/src/main/res/values/strings.xml` 中修改：

```xml
<string name="app_name">你的应用名称</string>
```

### 修改包名

1. 重命名 Java 包目录
2. 修改 `AndroidManifest.xml` 中的 package 属性
3. 修改 `app/build.gradle` 中的 applicationId

## 权限说明

应用请求以下权限：

- `INTERNET`: 访问网络
- `ACCESS_NETWORK_STATE`: 检查网络状态

## 兼容性

- **最低版本**: Android 5.0 (API 21)
- **目标版本**: Android 13 (API 33)
- **架构支持**: ARMv7, ARM64, x86, x86_64

## 常见问题

### 1. 网址无法访问

确保：
- 设备连接到正确的网络
- 目标服务器正在运行
- 防火墙设置允许访问

### 2. HTTPS 证书问题

如果目标网址使用自签名证书，可能需要在应用中添加证书信任代码。

### 3. 构建失败

检查：
- Android SDK 是否正确安装
- ANDROID_HOME 环境变量是否设置
- Java 版本是否兼容

## 开发者信息

- **开发工具**: Android Studio / VS Code
- **构建工具**: Gradle 7.5
- **编程语言**: Java
- **界面框架**: Android Native