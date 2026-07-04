# Flow Engine 文档

Flow Engine 是企业级工作流引擎，采用前后端分离架构：后端（Spring Boot 3.5.9 / Java 17）提供流程引擎与 REST API，前端（flow-frontend，React 18 / TypeScript / pnpm monorepo）提供可视化流程设计器与 PC/移动端审批组件。

本目录提供两份完整手册，均为 **HTML 长文档**（内嵌 SVG 架构图与界面示意图，可直接浏览器打开）：

| 文档 | 适用读者 | 内容视角 |
|------|----------|----------|
| [集成手册（HTML）](./integration-manual.html) | 后端 / 前端开发者、架构师 | **如何集成**：后端 Spring Boot 集成 + 前端 packages 集成能力 + 插件扩展机制 + 基于 example / app-pc / app-mobile 的示例分析 + 全栈集成最佳实践 |
| [使用手册（HTML）](./usage-manual.html) | 业务用户、前端、实施人员 | **如何操作**：从前端功能操作视角介绍流程设计器编排、节点/动作/策略配置、PC 审批、移动审批、Mock 演练等，含界面示意图与操作步骤路径 |

> 早期 Markdown 版本（`integration-manual.md` / `usage-manual.md`）以后端 API 视角为主，HTML 版本在此基础上补充了前端集成与操作视角，二者可互为参考。

## 推荐阅读顺序

1. **首次接入**：读《集成手册》→ 完成后端依赖引入、接口实现、数据库配置，前端引入 packages 并跑通 app-pc / app-mobile。
2. **业务使用**：读《使用手册》→ 了解流程设计器操作、审批操作、移动端操作，开始业务流程编排与审批。

## 当前版本

- 版本号：`0.0.52`（前后端对齐）
- 后端：JDK 17 / Spring Boot 3.5.9 / Spring Data JPA（默认 H2，支持达梦 DM）
- 前端：React 18 / TypeScript 5 / pnpm 10 / antd 6（PC）/ antd-mobile 5（移动）/ @flowgram.ai 1.0.8

## 前端 packages 速览（详见集成手册）

| 层 | 包 | 说明 |
|------|------|------|
| 基础层 | `flow-core` | 无 UI：EventBus / HttpClient / Presenter / ViewBindPlugin / GroovyFormatter |
| 基础层 | `flow-types` / `flow-icons` / `flow-approval-presenter` | 类型定义 / 图标 / 审批展示器框架（Redux） |
| PC | `flow-pc-ui` / `flow-pc-form` / `flow-pc-approval` | antd 封装 / 表单渲染 / 审批组件（ActionFactory） |
| PC | `flow-design` | 流程设计器（基于 @flowgram.ai，19 种节点） |
| 移动 | `flow-mobile-ui` / `flow-mobile-form` / `flow-mobile-approval` | antd-mobile 封装 / 表单 / 移动审批 |
| 应用 | `app-pc` / `app-mobile` | 官方示例应用（集成演示） |

> 更多架构细节与可复用能力见 [docs/capabilities/index.md](../capabilities/index.md)，开发规范见 [docs/conventions/index.md](../conventions/index.md)。
