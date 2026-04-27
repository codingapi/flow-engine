# IFlowOperator

## 何时使用

当需要在流程引擎中表达流程参与用户（审批人、创建人、转交人等）时实现此接口。`IFlowOperator` 是流程引擎的核心身份抽象，贯穿记录创建、权限校验、操作人匹配、转交链解析、Groovy 脚本等全部环节。典型场景：

- 业务系统的用户实体需要接入流程引擎（如 JPA `User` 实体实现 `IFlowOperator`）
- 自定义审批转交逻辑（`forwardOperator()`）
- 区分普通审批人和流程管理员（`isFlowManager()`）

## 如何引用

### Maven 坐标

```xml
<dependency>
    <groupId>com.codingapi.flow</groupId>
    <artifactId>flow-engine-framework</artifactId>
    <version>0.0.26</version>
</dependency>
```

## API 说明

### 核心类

| 类名 | 包路径 | 说明 |
|------|--------|------|
| `IFlowOperator` | `com.codingapi.flow.operator` | 流程参与用户接口，继承 `IUser`（来自 `com.codingapi.springboot.framework`） |
| `FlowOperatorGateway` | `com.codingapi.flow.gateway` | 操作人防腐层接口，通过 ID 查询 `IFlowOperator` 实现 |

### 关键方法

| 方法签名 | 参数说明 | 返回值 | 说明 |
|----------|----------|--------|------|
| `getUserId()` | 无 | `long` | 获取用户唯一 ID |
| `getName()` | 无 | `String` | 获取用户显示名称 |
| `isFlowManager()` | 无 | `boolean` | 是否流程管理员，管理员可强制干预他人流程 |
| `forwardOperator(GroovyScriptRequest)` | `request` — 流程会话上下文，可用于根据表单数据动态决定转交人 | `IFlowOperator` | 返回转交后的审批人，无需转交返回 `null` |

### 方法在框架中的调用场景

| 方法 | 调用位置 | 用途 |
|------|----------|------|
| `getUserId()` | `FlowRecord`、`OperatorManager`、`WorkflowStrategyManager`、`FlowAdviceBody`、`FlowSession`、`GroovyScriptRequest`、`FlowOperatorConvertor`、API 控制器 | 记录操作人 ID、权限校验、操作人匹配、数据库持久化、脚本上下文 |
| `getName()` | `FlowRecord`、`GroovyScriptRequest`、`FlowOperator`（DTO） | 记录操作人显示名称、脚本上下文、接口响应 |
| `isFlowManager()` | `WorkflowStrategyManager.verifyOperator()`、`GroovyScriptRequest`、`WorkflowController` | 权限绕过（管理员可操作他人记录）、脚本条件判断、接口访问控制 |
| `forwardOperator(GroovyScriptRequest)` | `FlowSession.loadFinalForwardOperator()`（递归调用） | 解析转交链，确定最终审批人 |

## 使用示例

```java
// 基础实现：JPA 用户实体
@Entity
@Table(name = "t_user")
public class User implements IFlowOperator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    private boolean flowManager;
    private Long flowOperatorId; // 转交目标用户 ID

    @Override
    public long getUserId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isFlowManager() {
        return flowManager;
    }

    @Override
    public IFlowOperator forwardOperator(GroovyScriptRequest request) {
        if (flowOperatorId != null && flowOperatorId > 0) {
            return GatewayContext.getInstance().getFlowOperator(flowOperatorId);
        }
        return null;
    }
}
```

```java
// 动态转交：根据表单数据决定转交人
@Override
public IFlowOperator forwardOperator(GroovyScriptRequest request) {
    // 根据表单金额决定审批人
    Object amount = request.getParameter("amount");
    if (amount != null && Double.parseDouble(amount.toString()) > 10000) {
        return GatewayContext.getInstance().getFlowOperator(CFO_USER_ID);
    }
    return null;
}
```

```java
// 单元测试中的简单实现
public class TestOperator implements IFlowOperator {

    private final long userId;
    private final String name;
    private final boolean manager;

    @Override
    public long getUserId() { return userId; }

    @Override
    public String getName() { return name; }

    @Override
    public boolean isFlowManager() { return manager; }

    @Override
    public IFlowOperator forwardOperator(GroovyScriptRequest request) {
        return null; // 测试中不涉及转交
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IFlowOperator other) {
            return this.userId == other.getUserId();
        }
        return false;
    }
}
```

## 注意事项

- **继承关系**：`IFlowOperator` 继承自 `com.codingapi.springboot.framework.user.IUser`，实现类需同时满足 `IUser` 的契约
- **转交链递归**：`forwardOperator()` 由 `FlowSession.loadFinalForwardOperator()` 递归调用，实现中应避免循环引用（A 转交给 B，B 又转交给 A），否则会导致栈溢出
- **JPA 持久化**：`IFlowOperator` 在数据库中通过 `FlowOperatorConvertor` 转换为 `Long`（用户 ID）存储，反序列化时通过 `GatewayContext` 重建完整对象
- **操作人查询**：`forwardOperator()` 实现中如需查询其他用户，应通过 `GatewayContext` 而非直接访问数据库，保持与框架解耦
- **`GroovyScriptRequest` 参数**：`forwardOperator()` 接收 `GroovyScriptRequest` 参数，可读取表单数据、当前节点信息等上下文，实现条件转交
