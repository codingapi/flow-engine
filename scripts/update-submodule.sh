#!/usr/bin/env bash

# 更新 flow-frontend 子模块到最新 main 分支

echo "正在更新 flow-frontend 子模块..."

# 进入子模块目录
cd ../flow-frontend

# 切换到 main 分支并拉取最新代码
echo "切换到 main 分支并拉取最新代码..."
git checkout main
git pull origin main

# 返回项目根目录
cd ../

# 更新子模块引用
echo "更新子模块引用..."
git add flow-frontend
git commit -m "Update flow-frontend to latest main"

echo ""
echo "✅ flow-frontend 已更新到最新 main 分支"
echo "请运行 'git push' 推送更改到远程仓库"
