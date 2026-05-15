#!/bin/bash

# HTTPS快速配置脚本
# 用于快速启用HTTPS

echo "========================================="
echo "HTTPS快速配置"
echo "========================================="
echo ""

# 检查证书是否存在
CERT_FILE="src/main/resources/certificates/mybatis-learn.p12"

if [ ! -f "$CERT_FILE" ]; then
    echo "1. 证书文件不存在，正在生成..."
    echo ""
    
    # 运行证书生成脚本
    if [ -f "generate-ssl-cert.sh" ]; then
        chmod +x generate-ssl-cert.sh
        ./generate-ssl-cert.sh
        
        if [ $? -ne 0 ]; then
            echo "✗ 证书生成失败"
            exit 1
        fi
    else
        echo "✗ 未找到 generate-ssl-cert.sh 脚本"
        exit 1
    fi
else
    echo "✓ 证书文件已存在: $CERT_FILE"
fi

echo ""
echo "2. 配置HTTPS..."
echo ""

# 询问是否启用HTTPS
read -p "是否立即启用HTTPS? (y/n): " ENABLE_SSL

if [ "$ENABLE_SSL" = "y" ] || [ "$ENABLE_SSL" = "Y" ]; then
    # 设置环境变量
    export SSL_ENABLED=true
    
    echo "✓ HTTPS已启用"
    echo ""
    echo "配置信息:"
    echo "  - HTTPS端口: 8443"
    echo "  - HTTP端口: 8080 (自动重定向)"
    echo "  - 证书路径: $CERT_FILE"
    echo ""
    echo "访问地址:"
    echo "  - https://localhost:8443"
    echo "  - http://localhost:8080 (将重定向到HTTPS)"
    echo ""
else
    echo "ℹ HTTPS未启用，保持HTTP模式"
    echo ""
    echo "要启用HTTPS，请运行:"
    echo "  export SSL_ENABLED=true"
    echo "  ./mvnw spring-boot:run"
    echo ""
fi

echo "========================================="
echo "配置完成！"
echo "========================================="
echo ""
echo "下一步："
echo "1. 启动应用: ./mvnw spring-boot:run"
echo "2. 查看文档: cat HTTPS_GUIDE.md"
echo ""
