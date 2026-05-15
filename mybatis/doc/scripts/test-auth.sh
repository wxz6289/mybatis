#!/bin/bash

# 用户认证系统测试脚本
# 使用前请确保应用已启动: ./mvnw spring-boot:run

BASE_URL="http://localhost:8080"

echo "========================================="
echo "用户认证系统测试"
echo "========================================="
echo ""

# 1. 测试公开接口
echo "1. 测试公开接口..."
curl -s ${BASE_URL}/api/test/public | jq .
echo ""
echo ""

# 2. 注册用户
echo "2. 注册用户..."
REGISTER_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "123456",
    "email": "test@example.com",
    "phone": "13800138000",
    "name": "测试用户",
    "age": 25,
    "deptId": 1
  }')

echo "$REGISTER_RESPONSE" | jq .
echo ""

# 提取Token
TOKEN=$(echo "$REGISTER_RESPONSE" | jq -r '.data.token')
echo "Token: $TOKEN"
echo ""
echo ""

# 3. 登录
echo "3. 用户登录..."
LOGIN_RESPONSE=$(curl -s -X POST ${BASE_URL}/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "123456"
  }')

echo "$LOGIN_RESPONSE" | jq .
echo ""

# 提取Token
TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.token')
echo "Token: $TOKEN"
echo ""
echo ""

# 4. 获取当前用户信息
echo "4. 获取当前用户信息..."
curl -s ${BASE_URL}/api/auth/me \
  -H "Authorization: Bearer ${TOKEN}" | jq .
echo ""
echo ""

# 5. 访问受保护的接口
echo "5. 访问受保护的接口..."
curl -s ${BASE_URL}/api/test/protected \
  -H "Authorization: Bearer ${TOKEN}" | jq .
echo ""
echo ""

# 6. 访问受保护接口（无Token，应该失败）
echo "6. 访问受保护接口（无Token，应该返回401）..."
curl -s ${BASE_URL}/api/test/protected | jq .
echo ""
echo ""

# 7. 退出登录
echo "7. 退出登录..."
curl -s -X POST ${BASE_URL}/api/auth/logout \
  -H "Authorization: Bearer ${TOKEN}" | jq .
echo ""
echo ""

echo "========================================="
echo "测试完成！"
echo "========================================="
