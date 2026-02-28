# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在本项目中工作时提供指导。

## 项目概述

Flow Engine 是一个企业级工作流引擎，基于 Java 17 和 Spring Boot 3.5.9 构建。提供可视化流程设计、动态表单配置、多节点类型流转以及脚本扩展功能。项目采用前后端分离架构，支持 PC 端和移动端客户端。

## 常用命令

### 后端 (Java/Maven)

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

### 前端 (React/pnpm)

```bash
# 安装依赖（使用 pnpm，而非 npm）
cd frontend && pnpm install

# 构建所有包
pnpm run build

# 构建特定应用
pnpm run build:app-pc

# 构建特定包
pnpm run build:flow-core   # 构建核心 API 库
pnpm run build:flow-types # 构建类型定义库
pnpm run build:flow-pc    # 构建 PC 端组件库

# 开发模式
pnpm run dev:app-pc    # PC 端应用
pnpm run dev:app-mobile # 移动端应用
```

## 架构

### 后端分层架构（8 层架构）

1. **工作流层** - 流程定义
2. **节点层** - 15 种节点类型（StartNode、EndNode、ApprovalNode、HandleNode、NotifyNode、RouterNode、SubProcessNode、DelayNode、TriggerNode、ConditionNode、ParallelNode、InclusiveNode 及其分支变体）
3. **动作层** - 8 种动作类型（Pass、Reject、Save、AddAudit、Delegate、Return、Transfer、Custom）
4. **记录层** - 流程实例记录
5. **会话层** - 会话管理
6. **管理器层** - 业务管理器（NodeStrategyManager、OperatorManager、ActionManager 等）
7. **策略层** - 策略驱动的配置
8. **脚本层** - Groovy 脚本运行时

### 设计模式

- **建造者模式** - WorkflowBuilder、NodeBuilder、FormMetaBuilder
- **工厂模式** - FlowActionFactory
- **策略模式** - NodeStrategy、WorkflowStrategy
- **模板方法模式** - BaseAction、BaseNodeBuilder
- **责任链模式** - 动作执行链
- **组合模式** - 带 blocks 的节点层级结构

### 模块结构

#### 后端模块

| 模块 | 描述 |
|--------|-------------|
| `flow-engine-framework` | 核心工作流引擎框架 |
| `flow-engine-starter` | Spring Boot 启动器 |
| `flow-engine-starter-api` | API 层 |
| `flow-engine-starter-infra` | 基础设施层 |
| `flow-engine-starter-query` | 查询层 |
| `flow-engine-example` | 示例应用 |

#### 前端模块

| 模块 | 描述 | 依赖 |
|--------|-------------|------|
| `flow-core` | 核心框架库（HTTP、Hooks、Presenter 等），不包含 UI 组件 | 无 |
| `flow-types` | TypeScript 类型定义（流程实例、表单、审批等业务类型） | flow-core |
| `flow-pc-ui` | PC 端基础 UI 组件库（按钮、输入框等原子组件） | 无 |
| `flow-pc-form` | PC 端表单相关组件（表单设计器、表单渲染等） | flow-core, flow-types |
| `flow-pc-design` | PC 端流程设计器组件（节点配置、属性面板等） | flow-core, flow-types, flow-pc-ui |
| `flow-pc-approval` | PC 端审批页面（待办/已办/审批处理等） | flow-pc-design, flow-pc-ui |

**前端模块依赖关系**：

```
flow-core (无UI)
    ↑
flow-types (类型定义)
    ↑       ↑
    │       └── flow-pc-form
    │               ↑
    └───────→ flow-pc-design ──→ flow-pc-approval
                    ↑
            flow-pc-ui (基础UI)
```

**模块划分原则**：

- **flow-core**：全局框架依赖，只包含与 UI 无关的基础能力（HTTP、状态管理、工具函数等）
- **flow-types**：全局类型定义，包含流程审批相关的业务类型（手机端和 PC 端共用）
- **flow-pc-ui**：PC 端基础 UI 组件库，提供原子化组件
- **flow-pc-form**：表单相关功能，依赖 flow-core + flow-types
- **flow-pc-design**：流程设计器功能，包含节点配置、属性面板、脚本配置等（本次优化的主要模块）
- **flow-pc-approval**：审批页面功能，依赖 flow-pc-design

### 技术栈

- **后端**：Java 17、Spring Boot 3.5.9、Groovy
- **前端**：React 18、TypeScript、pnpm、Rsbuild、Rslib、Antd

## 关键包

- `com.codingapi.flow.node` - 节点实现（15 种类型）
- `com.codingapi.flow.action` - 动作实现（8 种类型）
- `com.codingapi.flow.strategy` - 策略接口和实现
- `com.codingapi.flow.repository` - 数据访问层
- `com.codingapi.flow.service` - 业务服务层
- `com.codingapi.flow.script` - Groovy 脚本运行时

## 开发规范

- **与用户沟通及编写文档时，所有内容必须使用中文表述**
- 前端包管理使用 pnpm（根据用户配置）
- 前端文件命名规范：使用小写字母 + 下划线组合（如 `script_editor.tsx`、`variable_picker.tsx`）
- **前端导入规范**：使用 `@/` 路径别名导入项目内部模块，避免使用相对路径导入
- 设计涉及流程或 UML 图形的解决方案时，使用 Mermaid Markdown 语法
- 在编写计划的时候要遵循 TDD 的开发规范，务必要在方案中进行对实现代码逻辑的单元测试设计。
- 在设计计划方案或执行方案过程中，对于代码的设计规划与调整修改要遵循本项目的代码风格和架构设计规则
- 设计的计划要保存到本地的 `docs/` 目录下，每一个计划以时间+标题的方式命名创建文件夹，例如 `2026-02-26-xxxx`，文件夹下内容分为 `plan.md` 以及其他设计文件（如设计文件 xxx.pen 或其他设计内容信息）

```typescript
// ✅ 正确：使用 @/ 路径别名
import { GroovySyntaxConverter } from '@/components/design-editor/script/service/groovy-syntax-converter';
import { ScriptType } from '@/components/design-editor/typings/groovy-script';

// ❌ 错误：避免使用相对路径
import { GroovySyntaxConverter } from '../../../src/components/...';
```

### 面向对象开发规范

除前端 **.tsx 组件** 以外的所有代码（Java 后端、TypeScript 非组件文件）均采用面向对象方式开发：

- **Service 层**：使用 class 定义，通过依赖注入或单例模式使用
- **工具类**：使用 class 定义，提供实例方法或静态方法
- **适配器/转换器**：使用 class 实现，支持扩展

```typescript
// ✅ 正确：使用 class 定义 Service
export class GroovySyntaxConverter {
  private adapters: Map<ScriptType, ScriptAdapter> = new Map();

  public registerAdapter(adapter: ScriptAdapter): void {
    this.adapters.set(adapter.scriptType, adapter);
  }

  public toScript(...): string { ... }
}

// ❌ 错误：避免直接使用函数导出
export function toScript(...): string { ... }
export const toScript = (...): string => { ... };
```

