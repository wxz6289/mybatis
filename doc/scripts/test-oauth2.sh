#!/bin/bash

# OAuth2登录测试脚本

echo "========================================="
echo "OAuth2登录功能测试"
echo "========================================="
echo ""

BASE_URL="https://localhost:8443"

# 检查应用是否运行
echo "1. 检查应用状态..."
if curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/test/public" | grep -q "200"; then
    echo "✓ 应用正在运行"
else
    echo "✗ 应用未运行，请先启动应用"
    echo "  运行: ./mvnw spring-boot:run"
    exit 1
fi

echo ""
echo "2. 测试获取授权URL..."
echo ""

# 测试各个平台的授权URL
for platform in github google wechat qq weibo; do
    echo "测试 $platform 平台..."
    RESPONSE=$(curl -s "$BASE_URL/api/oauth2/authorize/$platform")
    
    if echo "$RESPONSE" | grep -q "\"code\":200"; then
        echo "  ✓ $platform 授权URL获取成功"
        
        # 提取URL（简单方式）
        URL=$(echo "$RESPONSE" | grep -o '"data":"[^"]*"' | cut -d'"' -f4)
        if [ ! -z "$URL" ]; then
            echo "  授权URL: ${URL:0:80}..."
        fi
    else
        echo "  ✗ $platform 授权URL获取失败"
        echo "  响应: $RESPONSE"
    fi
    echo ""
done

echo "========================================="
echo "测试完成！"
echo "========================================="
echo ""
echo "下一步："
echo "1. 在浏览器中访问上述授权URL"
echo "2. 使用对应平台账号授权"
echo "3. 观察回调结果"
echo "4. 查看应用日志了解详细信息"
echo ""
echo "查看日志："
echo "  tail -f logs/application.log"
echo ""
