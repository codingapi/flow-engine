# 能力（Capabilities）

> 🔄 此文件由 `rebuild_pkr_index.py` 自动生成，请勿手动编辑。

## ✅ 已实现

| 名称 | 模块 | 描述 | 范围 | 来源 |
|------|------|------|------|------|
| [commons/lang3](./commons/lang3.md) | commons | Apache Commons 工具库，提供字符串、集合、IO 等通用工具方法 | 后端 | 框架:Apache Commons |
| [flow-engine-framework/action](./flow-engine-framework/action.md) | flow-engine-framework | 流程动作体系，包含 8 种动作类型、动作接口和动作工厂 | 后端 | 项目自有 |
| [flow-engine-framework/exception](./flow-engine-framework/exception.md) | flow-engine-framework | 流程异常体系，包含异常基类和 5 种业务异常子类 | 后端 | 项目自有 |
| [flow-engine-framework/flow-service](./flow-engine-framework/flow-service.md) | flow-engine-framework | 流程服务层，封装流程的创建、审批、撤销、催办等核心业务操作 | 后端 | 项目自有 |
| [flow-engine-framework/gateway-context](./flow-engine-framework/gateway-context.md) | flow-engine-framework | 网关上下文，提供操作者网关的注入和线程级缓存 | 后端 | 项目自有 |
| [flow-engine-framework/node-manager](./flow-engine-framework/node-manager.md) | flow-engine-framework | 流程节点管理器，负责节点查找、下一节点计算和节点状态管理 | 后端 | 项目自有 |
| [flow-engine-framework/node-strategy](./flow-engine-framework/node-strategy.md) | flow-engine-framework | 节点策略体系，包含 15 种策略类型、策略接口、策略工厂和策略管理器 | 后端 | 项目自有 |
| [flow-engine-framework/node](./flow-engine-framework/node.md) | flow-engine-framework | 流程节点体系，包含 19 种节点类型、节点接口层次和节点工厂 | 后端 | 项目自有 |
| [flow-engine-framework/repository](./flow-engine-framework/repository.md) | flow-engine-framework | 仓储抽象层，定义流程引擎的持久化接口和仓储持有者上下文 | 后端 | 项目自有 |
| [flow-engine-framework/script](./flow-engine-framework/script.md) | flow-engine-framework | Groovy 脚本运行时，包含脚本注册表、脚本类型封装和脚本执行上下文 | 后端 | 项目自有 |
| [flow-engine-framework/workflow-strategy](./flow-engine-framework/workflow-strategy.md) | flow-engine-framework | 工作流级别策略体系，包含策略接口、工厂和具体策略实现 | 后端 | 项目自有 |
| [flow-engine-framework/workflow](./flow-engine-framework/workflow.md) | flow-engine-framework | 工作流引擎核心模型，包含流程定义、构建器、版本管理和运行时快照 | 后端 | 项目自有 |
| [flow-engine-starter/auto-configuration](./flow-engine-starter/auto-configuration.md) | flow-engine-starter | Spring Boot 自动配置入口，注册流程引擎的核心组件和仓储上下文 | 后端 | 项目自有 |
| [lombok/lombok](./lombok/lombok.md) | lombok | Lombok 代码简化工具，通过注解自动生成 getter/setter/builder 等样板代码 | 后端 | 框架:Lombok |
| [springboot/cache](./springboot/cache.md) | springboot | Spring Cache 声明式缓存能力，通过注解简化缓存操作 | 后端 | 框架:Spring Boot |
| [springboot/data-jpa](./springboot/data-jpa.md) | springboot | Spring Data JPA 能力，提供 ORM、Repository 抽象和分页查询 | 后端 | 框架:Spring Boot |
| [springboot/ioc](./springboot/ioc.md) | springboot | Spring Framework IoC 容器、依赖注入、AOP 和事件发布 | 后端 | 框架:Spring Boot |
| [springboot/web](./springboot/web.md) | springboot | Spring Boot Web 能力，提供 REST API、Controller 和请求处理 | 后端 | 框架:Spring Boot |

---

**统计**: 共 18 篇 — 已实现 18 / 计划中 0 / 已废弃 0
