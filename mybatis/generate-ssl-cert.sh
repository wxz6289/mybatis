#!/bin/bash

# 生成自签名SSL证书脚本
# 用于开发环境测试HTTPS

echo "========================================="
echo "生成自签名SSL证书"
echo "========================================="
echo ""

# 检查keytool是否可用
if ! command -v keytool &> /dev/null; then
    echo "错误: 未找到keytool命令，请确保已安装JDK"
    exit 1
fi

# 创建证书目录
CERT_DIR="src/main/resources/certificates"
mkdir -p "$CERT_DIR"

echo "1. 生成PKCS12格式的证书..."
keytool -genkeypair \
  -alias mybatis-learn \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore "$CERT_DIR/mybatis-learn.p12" \
  -validity 3650 \
  -storepass changeit \
  -keypass changeit \
  -dname "CN=localhost, OU=Development, O=MyBatis Learn, L=Beijing, ST=Beijing, C=CN" \
  -ext "SAN=DNS:localhost,IP:127.0.0.1"

if [ $? -eq 0 ]; then
    echo "✓ 证书生成成功: $CERT_DIR/mybatis-learn.p12"
    echo ""
    echo "证书信息:"
    echo "  - 别名: mybatis-learn"
    echo "  - 类型: PKCS12"
    echo "  - 有效期: 3650天 (10年)"
    echo "  - 密码: changeit"
    echo "  - 域名: localhost"
    echo ""
    echo "2. 查看证书详情..."
    keytool -list -v \
      -keystore "$CERT_DIR/mybatis-learn.p12" \
      -storepass changeit \
      -storetype PKCS12
    echo ""
    echo "========================================="
    echo "证书生成完成！"
    echo "========================================="
    echo ""
    echo "下一步："
    echo "1. 在 application.yaml 中配置HTTPS"
    echo "2. 或者运行: ./mvnw spring-boot:run"
    echo ""
else
    echo "✗ 证书生成失败"
    exit 1
fi
