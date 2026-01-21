# Flow Engine

> 企业级流程引擎 - 可视化流程设计、动态表单配置、多节点类型流转

## 项目简介

Flow Engine 是一个基于 Java 17 和 Spring Boot 3.5.9 构建的企业级流程引擎，提供完整的流程管理能力，包括可视化流程设计、动态表单配置、多节点类型流转、脚本扩展等功能。采用前后端分离架构，支持 PC 端和移动端。

### 核心特性

- **12 种节点类型** - 支持开始、结束、审批、办理、通知、条件分支、并行分支、路由分支、包容分支、子流程、延迟、触发节点
- **9 种操作类型** - 通过、拒绝、保存、加签、委派、退回、转办、自定义操作
- **策略驱动配置** - 所有关键配置通过策略实现，支持动态扩展
- **Groovy 脚本扩展** - 支持动态发起人匹配、审批人加载、条件判断、自定义操作等
- **多人审批模式** - 顺序审批、会签（可设置比例）、或签、随机审批
- **线程安全** - 脚本运行时采用细粒度同步锁，支持并发执行不同脚本
- **自动资源清理** - 双重清理机制（阈值触发 + 定时清理），避免内存泄漏
- **完善的异常体系** - 基于 RuntimeException 的框架异常体系

## 项目结构

```
flow-engine
├── flow-engine-framework         # 流程引擎核心框架
│   ├── src/main/java
│   │   └── com.codingapi.flow
│   │       ├── action            # 操作层
│   │       ├── builder           # 构建器
│   │       ├── context           # 上下文
│   │       ├── event             # 事件
│   │       ├── exception         # 异常
│   │       ├── form              # 表单
│   │       ├── gateway           # 网关
│   │       ├── node              # 节点层
│   │       ├── operator          # 操作人
│   │       ├── pojo              # 数据对象
│   │       ├── record            # 记录层
│   │       ├── script            # 脚本层
│   │       ├── session           # 会话层
│   │       ├── strategy          # 策略层
│   │       ├── user              # 用户
│   │       ├── utils             # 工具类
│   │       └── workflow          # 流程层
│   └── src/test/java             # 测试代码
├── flow-engine-starter           # Spring Boot 启动器
├── flow-engine-starter-infra     # 持久化层启动器
├── flow-engine-example           # 示例项目
└── frontend                      # 前端工程
    ├── apps
    │   ├── app-pc                # PC 端应用
    │   └── app-mobile            # 移动端应用
    └── packages
        ├── flow-design           # 流程设计器
        ├── flow-pc               # PC 端展示组件
        └── flow-mobile           # 移动端展示组件
```

## 技术栈

### 后端

- **Java 17** - 编程语言
- **Spring Boot 3.5.9** - 应用框架
- **Groovy** - 脚本引擎
- **Lombok** - 简化代码
- **Fastjson** - JSON 处理
- **Apache Commons** - 工具类库

### 前端

- **React** - UI 框架
- **TypeScript** - 类型安全
- **Rsbuild** - 构建工具
- **pnpm** - 包管理器

## 快速开始

### 后端

```bash
# 克隆项目
git clone https://github.com/codingapi/flow-engine.git
cd flow-engine

# 构建项目
./mvnw clean install

# 运行示例项目
cd flow-engine-example
mvn spring-boot:run
```

### 前端

```bash
cd frontend

# 安装依赖
pnpm install

# 构建设计库
pnpm run build:flow-engine

# 启动 PC 应用
pnpm run dev:app-pc
```

## 核心架构

### 八层架构

1. **Workflow Layer** - 流程定义层
2. **Node Layer** - 节点层（12 种节点类型）
3. **Action Layer** - 操作层（9 种操作类型）
4. **Record Layer** - 记录层
5. **Session Layer** - 会话层
6. **Manager Layer** - 管理器层
7. **Strategy Layer** - 策略层
8. **Script Layer** - 脚本层

### 设计模式

- **Builder Pattern** - 构建器模式
- **Factory Pattern** - 工厂模式
- **Strategy Pattern** - 策略模式
- **Template Method** - 模板方法模式
- **Singleton Pattern** - 单例模式
- **Chain of Responsibility** - 责任链模式
- **Composite Pattern** - 组合模式

## 节点类型

| 节点类型 | 说明 | NODE_TYPE |
|---------|------|-----------|
| StartNode | 开始节点 | `start` |
| EndNode | 结束节点 | `end` |
| ApprovalNode | 审批节点 | `approval` |
| HandleNode | 办理节点 | `handle` |
| NotifyNode | 通知节点 | `notify` |
| BranchNodeBranchNode | 条件分支 | `condition_branch` |
| ParallelBranchNode | 并行分支 | `parallel_branch` |
| RouterBranchNode | 路由分支 | `router_branch` |
| InclusiveBranchNode | 包容分支 | `inclusive_branch` |
| SubProcessNode | 子流程节点 | `sub_process` |
| DelayNode | 延迟节点 | `delay` |
| TriggerNode | 触发节点 | `trigger` |

## 操作类型

| 操作类型 | 说明 | ActionType |
|---------|------|------------|
| DefaultAction | 默认操作 | `DEFAULT` |
| PassAction | 通过 | `PASS` |
| RejectAction | 拒绝 | `REJECT` |
| SaveAction | 保存 | `SAVE` |
| ReturnAction | 退回 | `RETURN` |
| TransferAction | 转办 | `TRANSFER` |
| AddAuditAction | 加签 | `ADD_AUDIT` |
| DelegateAction | 委派 | `DELEGATE` |
| CustomAction | 自定义 | `CUSTOM` |

## 多人审批模式

| 模式 | 说明 | 完成条件 |
|-----|------|---------|
| SEQUENCE | 顺序审批 | 所有人按顺序完成 |
| MERGE | 会签 | 达到设定比例的人完成 |
| ANY | 或签 | 任意一人完成 |
| RANDOM_ONE | 随机审批 | 随机选中的一人完成 |

## 文档

- [PRD.md](PRD.md) - 产品需求文档
- [Design.md](Design.md) - 架构设计文档
- [AGENTS.md](AGENTS.md) - 编码规范
- [CLAUDE.md](CLAUDE.md) - Claude Code 指南

## 测试

```bash
# 运行所有测试
./mvnw test

# 运行指定模块测试
./mvnw test -pl flow-engine-framework

# 运行指定测试类
./mvnw test -Dtest=ScriptRuntimeContextTest
```

## 许可证

[LICENSE](LICENSE)

## 贡献

欢迎提交 Issue 和 Pull Request！
