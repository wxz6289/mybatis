#!/bin/bash

# Android 浏览器应用打包脚本
# 作者: GitHub Copilot
# 日期: $(date)

echo "==========================================="
echo "       Android 浏览器应用打包工具"
echo "==========================================="

# 检查 Android SDK 环境
check_android_sdk() {
    if [ -z "$ANDROID_HOME" ]; then
        echo "错误: 未设置 ANDROID_HOME 环境变量"
        echo "请设置 ANDROID_HOME 指向你的 Android SDK 目录"
        echo "例如: export ANDROID_HOME=/Users/你的用户名/Library/Android/sdk"
        exit 1
    fi

    if [ ! -d "$ANDROID_HOME" ]; then
        echo "错误: ANDROID_HOME 目录不存在: $ANDROID_HOME"
        exit 1
    fi

    echo "✓ Android SDK 路径: $ANDROID_HOME"
}

# 检查 Java 环境
check_java() {
    if ! command -v java &> /dev/null; then
        echo "错误: 未找到 Java"
        echo "请安装 Java 8 或更高版本"
        exit 1
    fi

    java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    echo "✓ Java 版本: $java_version"
}

# 清理旧的构建文件
clean_build() {
    echo "清理旧的构建文件..."
    if [ -d "app/build" ]; then
        rm -rf app/build
        echo "✓ 已清理 app/build 目录"
    fi

    if [ -d "build" ]; then
        rm -rf build
        echo "✓ 已清理根目录 build 目录"
    fi
}

# 构建 APK
build_apk() {
    echo "开始构建 APK..."

    # 给 gradlew 添加执行权限
    chmod +x gradlew

    # 构建 debug APK
    echo "构建 Debug APK..."
    ./gradlew assembleDebug

    if [ $? -eq 0 ]; then
        echo "✓ Debug APK 构建成功"
        debug_apk_path="app/build/outputs/apk/debug/app-debug.apk"
        if [ -f "$debug_apk_path" ]; then
            echo "✓ Debug APK 位置: $debug_apk_path"
            apk_size=$(du -h "$debug_apk_path" | cut -f1)
            echo "✓ APK 大小: $apk_size"
        fi
    else
        echo "✗ Debug APK 构建失败"
        exit 1
    fi

    echo ""
    echo "是否构建 Release APK? (y/n)"
    read -r answer
    if [ "$answer" = "y" ] || [ "$answer" = "Y" ]; then
        echo "构建 Release APK..."
        ./gradlew assembleRelease

        if [ $? -eq 0 ]; then
            echo "✓ Release APK 构建成功"
            release_apk_path="app/build/outputs/apk/release/app-release-unsigned.apk"
            if [ -f "$release_apk_path" ]; then
                echo "✓ Release APK 位置: $release_apk_path"
                apk_size=$(du -h "$release_apk_path" | cut -f1)
                echo "✓ APK 大小: $apk_size"
                echo "注意: Release APK 未签名，需要签名后才能安装到设备"
            fi
        else
            echo "✗ Release APK 构建失败"
        fi
    fi
}

# 安装 APK 到设备
install_apk() {
    echo ""
    echo "是否安装 APK 到连接的设备? (y/n)"
    read -r answer
    if [ "$answer" = "y" ] || [ "$answer" = "Y" ]; then
        debug_apk_path="app/build/outputs/apk/debug/app-debug.apk"
        if [ -f "$debug_apk_path" ]; then
            echo "检查连接的设备..."
            adb devices
            echo "开始安装..."
            adb install -r "$debug_apk_path"
            if [ $? -eq 0 ]; then
                echo "✓ APK 安装成功"
                echo "应用包名: com.example.androidbrowser"
            else
                echo "✗ APK 安装失败"
            fi
        else
            echo "✗ 未找到 Debug APK 文件"
        fi
    fi
}

# 显示项目信息
show_project_info() {
    echo ""
    echo "==========================================="
    echo "           项目信息"
    echo "==========================================="
    echo "应用名称: 内置浏览器"
    echo "包名: com.example.androidbrowser"
    echo "目标 URL: https://192.168.31.3:8448"
    echo "最小 SDK: 21 (Android 5.0)"
    echo "目标 SDK: 33 (Android 13)"
    echo "版本: 1.0"
    echo "==========================================="
}

# 主函数
main() {
    # 检查是否在项目根目录
    if [ ! -f "build.gradle" ] || [ ! -f "settings.gradle" ]; then
        echo "错误: 请在 Android 项目根目录下运行此脚本"
        exit 1
    fi

    show_project_info
    check_android_sdk
    check_java
    clean_build
    build_apk
    install_apk

    echo ""
    echo "==========================================="
    echo "           打包完成!"
    echo "==========================================="
}

# 运行主函数
main