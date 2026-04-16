# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在本项目中工作时提供指导。

## 项目概述

Flow Engine 是一个企业级工作流引擎，基于 Java 17 和 Spring Boot 3.5.9 构建（当前版本 0.0.18）。

### 核心模块

| 模块 | 说明 |
|------|------|
| `flow-engine-framework` | 核心工作流引擎框架（节点/动作/策略/脚本/服务） |
| `flow-engine-starter` | Spring Boot 自动配置入口 |
| `flow-engine-starter-api` | REST API 层 |
| `flow-engine-starter-infra` | 持久化层（JPA 实体、仓储实现） |
| `flow-engine-starter-query` | 查询层 |
| `flow-engine-example` | 示例应用 |

### 框架层包结构

| 包路径 | 职责 |
|--------|------|
| `com.codingapi.flow.node` | 19 种流程节点类型 |
| `com.codingapi.flow.action` | 8 种动作类型 |
| `com.codingapi.flow.strategy` | 节点策略和工作流策略 |
| `com.codingapi.flow.script` | Groovy 脚本运行时 |
| `com.codingapi.flow.repository` | 仓储接口定义 |
| `com.codingapi.flow.builder` | 建造者模式实现 |
| `com.codingapi.flow.manager` | 业务管理器 |
| `com.codingapi.flow.service` | 业务服务层 |

## 常用命令

### 构建与测试

```bash
# 构建整个项目
./mvnw clean install

# 运行所有测试
./mvnw test

# 运行特定模块的测试
./mvnw test -pl flow-engine-framework

# 运行特定的测试类
./mvnw test -Dtest=ScriptRuntimeContextTest

# 运行示例应用
cd flow-engine-example && mvn spring-boot:run
```

## 开发规范

### 设计先行

- **开发任何新功能前，必须先在 `docs/architecture/` 下完成架构设计文档，经用户确认后再编码**
- 架构文档包含：系统架构、API 接口、核心组件方案、模块职责划分
- 任务清单见 `docs/todo/`
- 各阶段详细可执行的开发计划见 `docs/plan/`

### 禁止自动提交

- **未经用户明确要求，任何情况下不得执行 `git commit` / `git push` / `git merge` 操作**
- 所有代码变更需经用户审核后再提交

### 基本规范

- **与用户沟通及编写文档时，所有内容必须使用中文表述**
- **在每次修改代码以后，要执行本地化的编译验证确保代码没有错误**
- Java 代码遵循 Spring Boot 规范
- 设计涉及流程或 UML 图形的解决方案时，使用纯文本线框图表示
- 在设计计划方案或执行方案过程中，对于代码的设计规划与调整修改要遵循本项目的代码风格和架构设计规则
- 设计的计划要保存到本地的 `docs/` 目录下：
  - `docs/plan/` - 存放详细可执行开发计划，文件命名格式为 `yyyy-mm-dd-标题.md`
  - `docs/architecture/` - 存放架构设计文档
  - `docs/todo/` - 存放阶段任务清单

### Git 工作流

**代码提交路径：**

```
feature/{task-name}  →  PR  →  dev  →  （用户审核）  →  main
```

- **禁止直接向 main 分支提交**，main 分支由用户手动合并管理
- **dev 分支为集成分支**，所有 PR 的目标分支均为 dev
- **所有代码开发必须在 git worktree 中进行**，不在主工作区直接改代码

### 测试规范

本项目后端采用 JUnit 5 进行单元测试。

**测试文件位置：**

- 测试文件放在 `src/test/java/` 目录下，与源代码同级的测试目录中
- 测试文件命名：`*Test.java`

**测试编写流程：**

1. **Red（红灯）**：先编写测试用例，明确预期行为
2. **Green（绿灯）**：编写最小代码使测试通过
3. **Refactor（重构）**：优化代码结构，运行全部测试确认不回归

**测试用例设计要求：**

- 每个测试用例需明确：测试目标、前置条件、执行步骤、期望断言
- 优先测试核心业务逻辑（Service、Manager、Converter、Utils 等）
- 测试应该有确定性结果，避免依赖外部状态

**测试命令：**

```bash
# 运行所有测试
./mvnw test

# 运行特定模块的测试
./mvnw test -pl flow-engine-framework

# 运行特定的测试类
./mvnw test -Dtest=ScriptRuntimeContextTest

# 运行特定包的测试（使用 Maven surefire）
./mvnw test -pl flow-engine-framework -Dtest=**/script/**
```

**示例（基于 flow-engine-framework 的脚本测试）：**

```java
// flow-engine-framework/src/test/java/com/codingapi/flow/script/ScriptRuntimeContextTest.java
package com.codingapi.flow.script;

import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScriptRuntimeContextTest {

    @Test
    void should_execute_groovy_script() {
        // given
        String script = "return 1 + 1";

        // when
        Object result = ScriptRuntimeContext.getInstance().executeScript(script, context);

        // then
        assertEquals(2, result);
    }
}
```

## 架构设计文档

详细架构设计见 `docs/architecture/` 目录：

| 文档 | 说明 |
|------|------|
| `01-项目概览.md` | 项目背景、技术栈 |
| `02-模块总览.md` | Maven 模块划分 |
| `03-节点模块.md` | 节点接口、节点类型 |
| `04-动作模块.md` | 动作接口、动作执行 |
| `05-策略模块.md` | 节点策略、工作流策略 |
| `06-脚本模块.md` | Groovy 脚本运行时 |
| `07-仓储模块.md` | 仓储接口、基础设施实现 |
| `08-构建器模块.md` | 建造者模式实现 |
| `09-管理器模块.md` | 业务管理器 |
| `10-服务模块.md` | 业务服务层 |
| `11-网关与上下文.md` | 网关接口、上下文 |
| `12-数据库设计.md` | 实体设计、表结构 |
| `13-API接口设计.md` | REST API 设计 |
