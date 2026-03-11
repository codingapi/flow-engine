#!/usr/bin/env bash

# 获取脚本所在目录（项目根目录）
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 进入项目根目录
cd "$PROJECT_DIR"

# 更新 submodule 到最新提交（如果未初始化则先初始化）
git submodule update --init --remote --merge flow-frontend