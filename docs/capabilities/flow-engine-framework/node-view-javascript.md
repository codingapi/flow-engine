---
name: flow-engine-framework/node-view-javascript
module: flow-engine-framework
description: 节点视图脚本能力，支持视图脚本的缓存、保存与查询（含缓存自动清理）
status: 已实现
scope: 后端
source: 项目自有
import: "com.codingapi.flow:flow-engine-framework"
symbols:
  - NodeViewJavaScript
  - NodeViewJavaScriptCacheContext
  - NodeViewJavaScriptRepository
  - NodeViewJavaScriptCacheContextRegister
  - NodeViewJavaScriptController
content_hash: ba840560c9bb583f5447203a5bcaf8cc8d619cc3d6ac1f9151d3d2ef273fac1a
---

## 解决什么问题

提供节点视图 JavaScript 脚本的管理能力，解决以下问题：

- **脚本缓存**：`NodeViewJavaScriptCacheContext` 单例保存视图脚本，缓存项超过 15 分钟自动清理
- **脚本持久化**：`NodeViewJavaScriptRepository` 抽象脚本的持久化，由业务项目实现（框架无默认实现）
- **共享清理线程**：使用单条共享调度线程（`node-view-javascript-clear`）替代每个缓存项一个 Timer 原生线程
- **REST 接口**：`NodeViewJavaScriptController` 提供脚本的保存与查询接口

## 如何使用

### 核心组件

| 组件 | 职责 |
|------|------|
| `NodeViewJavaScript` | 视图脚本领域对象，包含 code、script、createTime、updateTime |
| `NodeViewJavaScriptCacheContext` | 单例缓存上下文，提供 `cache` / `save` / `get` / `remove` / `delete` |
| `NodeViewJavaScriptRepository` | 仓储接口，定义 `save` / `delete` / `get` |
| `NodeViewJavaScriptCacheContextRegister` | 自动配置注册器，将业务实现的仓储注入缓存上下文 |
| `NodeViewJavaScriptController` | REST 接口（`/api/cmd/node-view`），提供 `save` 与 `getScript` |

### 缓存机制

- `cache(code, script)` 写入内存缓存，并启动 15 分钟自动清理任务（重复写入会取消旧任务的清理）
- `get(code)` 优先查缓存，未命中时回查仓储
- `save(javaScript)` 先写仓储再移除缓存；`delete(code)` 同时清理缓存与仓储

### 业务扩展

`NodeViewJavaScriptRepository` 框架层无默认实现，业务项目需自行实现并注册为 Spring Bean（参考 `flow-engine-example` 中的 `NodeViewJavaScriptRepositoryImpl`），否则脚本仅存在于内存缓存。

## 使用实例

```java
// 1. 缓存脚本
NodeViewJavaScriptCacheContext.getInstance().cache("node-001", "var x = 1;");

// 2. 查询脚本
NodeViewJavaScript js = NodeViewJavaScriptCacheContext.getInstance().get("node-001");

// 3. 持久化保存
NodeViewJavaScript viewJs = new NodeViewJavaScript("node-001", "var x = 1;", now, now);
viewJs.save(); // 通过 NodeViewJavaScriptCacheContext 写入仓储

// 4. REST 接口
// POST /api/cmd/node-view/save  body: {code, script}
// GET  /api/cmd/node-view/getScript?key=node-001
```