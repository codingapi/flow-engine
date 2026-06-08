---
name: flow-engine-framework/exception
module: flow-engine-framework
description: 流程异常体系，包含异常基类和 5 种业务异常子类
status: 已实现
scope: 后端
source: 项目自有
import: "com.codingapi.flow:flow-engine-framework"
symbols:
  - FlowException
  - FlowExecutionException
  - FlowValidationException
  - FlowStateException
  - FlowNotFoundException
  - FlowPermissionException
content_hash: 1a3438bf88b6b98d84a03fabe6f08ed66948a8da8f44fea35b8a9d49d1f84458
---

## 解决什么问题

提供流程引擎的标准化异常体系，解决以下问题：

- **异常分类**：按业务语义将异常分为执行异常、验证异常、状态异常、未找到异常和权限异常五类
- **错误码标准化**：每种异常使用 `errorCode + message` 格式，支持国际化消息
- **工厂方法创建**：每种异常提供静态工厂方法，统一异常创建方式并确保错误码一致

## 如何使用

### 异常层次

```
FlowException（抽象基类，继承 LocaleMessageException）
├── FlowExecutionException   — 流程执行过程中的错误
├── FlowValidationException  — 流程配置或参数验证错误
├── FlowStateException       — 流程状态不允许当前操作
├── FlowNotFoundException    — 请求的资源不存在
└── FlowPermissionException  — 操作者无权限执行操作
```

### 各类异常的工厂方法

| 异常类 | 典型工厂方法 |
|--------|-------------|
| `FlowExecutionException` | `scriptExecutionError()`, `routerNodeNotFound()`, `createRecordSizeError()`, `operatorNotInScope()` |
| `FlowValidationException` | 参数验证相关错误 |
| `FlowStateException` | `repositoryNotRegistered()`, `recordAlreadyDone()`, `operatorNotMatch()`, `edgeConfigError()`, `recordLimitUrgeError()` |
| `FlowNotFoundException` | `workflow()`, `record()`, `node()`, `operator()`, `action()` |
| `FlowPermissionException` | `accessDenied()` |

### 扩展新异常

继承 `FlowException`，提供构造函数和静态工厂方法。

## 使用实例

```java
// 使用工厂方法抛出异常
throw FlowNotFoundException.workflow("leave-request");
// → FlowNotFoundException("notFound.workflow.definition", "Workflow definition not found: leave-request")

throw FlowStateException.recordAlreadyDone();
// → FlowStateException("state.record.alreadyDone", "Flow record is already completed...")

throw FlowPermissionException.accessDenied("approve");
// → FlowPermissionException("permission.access.denied", "Access denied to operation: approve")

// 全局异常处理（在 Controller/GlobalExceptionHandler 中）
@ExceptionHandler(FlowException.class)
public ResponseEntity<ErrorResponse> handleFlowException(FlowException ex) {
    return ResponseEntity.badRequest()
        .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
}
```
