---
module: flow-engine-framework
name: IFlowOperator
description: 流程操作人核心接口，定义用户 ID、名称、管理员标识和转交审批人能力，是流程引擎中所有用户相关操作的统一抽象。
---

# IFlowOperator

- **来源**: 自有
- **所属 module**: flow-engine-framework
- **Maven 坐标**: com.codingapi.flow:flow-engine-framework:0.0.28

## 何时使用

IFlowOperator 是流程引擎中对参与流程的用户（审批人、创建者、提交者等）的统一抽象。业务系统集成 flow-engine 时，需让自身的用户实体类实现该接口，提供用户 ID、名称、是否流程管理员以及转交审批人等能力。框架内部通过此接口与业务用户解耦，不依赖具体的用户数据结构。

## 如何引用

### Maven 坐标

```xml
<dependency>
    <groupId>com.codingapi.flow</groupId>
    <artifactId>flow-engine-framework</artifactId>
    <version>0.0.28</version>
</dependency>
```

## API 说明

### 核心类

| 类名 | 包路径 | 说明 |
|------|--------|------|
| `IFlowOperator` | `com.codingapi.flow.operator` | 流程操作人接口，继承自 IUser 标记接口 |
| `IUser` | `com.codingapi.springboot.framework.user` | 父级标记接口（空接口，无方法定义） |
| `FlowOperatorGateway` | `com.codingapi.flow.gateway` | 操作者防腐层，返回 IFlowOperator 实例 |

### 关键方法

#### IFlowOperator 接口方法

| 方法签名 | 返回值 | 说明 |
|----------|--------|------|
| `getUserId()` | `long` | 获取用户 ID，作为用户唯一标识 |
| `getName()` | `String` | 获取用户名称，用于流程记录中的显示 |
| `isFlowManager()` | `boolean` | 是否流程管理员，管理员可强制干预流程（如强制通过、终止等） |
| `forwardOperator(GroovyScriptRequest request)` | `IFlowOperator` | 获取转交审批人。根据请求上下文动态返回转交目标，返回 null 表示不转交 |

### 接口继承关系

```
IUser (标记接口，无方法)
  └── IFlowOperator
        ├── getUserId()
        ├── getName()
        ├── isFlowManager()
        └── forwardOperator(GroovyScriptRequest)
```

### 框架中的引用范围

IFlowOperator 是框架中使用最广泛的接口之一，被以下核心模块引用：

| 模块 | 引用位置 |
|------|----------|
| 上下文 | GatewayContext、FlowScriptContext、IBeanFactory、RepositoryHolderContext |
| 脚本 | GroovyScriptBind、GroovyScriptRequest、GroovyWorkflowRequest |
| 节点 | StartNode、NotifyNode、BaseAuditNode |
| 动作 | PassAction、TransferAction、DelegateAction、AddAuditAction |
| 服务 | FlowActionService、FlowCreateService、FlowDetailService、FlowProcessNodeService 等 |
| 流程记录 | FlowRecord |
| 工作流 | Workflow、WorkflowBuilder、WorkflowVersion |

### 配置项

IFlowOperator 自身无配置项。

## 使用示例

### 基础实现（简单场景）

```java
public class User implements IFlowOperator {

    private final long userId;
    private final String name;
    private final boolean manager;

    public User(long userId, String name) {
        this(userId, name, false);
    }

    public User(long userId, String name, boolean manager) {
        this.userId = userId;
        this.name = name;
        this.manager = manager;
    }

    @Override
    public long getUserId() {
        return userId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isFlowManager() {
        return manager;
    }

    @Override
    public IFlowOperator forwardOperator(GroovyScriptRequest request) {
        // 不需要转交，返回 null
        return null;
    }
}
```

### JPA 实体实现（含转交逻辑）

```java
@Data
@Entity
@Table(name = "t_user")
public class User implements IFlowOperator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Boolean flowManager;
    private Long flowOperatorId;

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
        // 如果配置了转交人 ID，则查询并返回转交目标
        if (flowOperatorId != null && flowOperatorId > 0) {
            return GatewayContext.getInstance().getFlowOperator(flowOperatorId);
        }
        return null;
    }
}
```

### 在脚本中访问操作人信息

```groovy
// 获取当前审批人名称
def run(request){
    def operator = request.getCurrentOperator()
    return operator.getName()
}

// 判断是否流程管理员
def run(request){
    return request.isFlowManager()
}
```

## 注意事项

- **必须实现**：业务系统必须提供一个实现 IFlowOperator 的用户类，并通过 FlowOperatorGateway 返回实例
- **getUserId() 返回 long**：用户 ID 固定为 long 类型，不支持 String 等其他类型
- **forwardOperator 可返回 null**：当不需要转交时返回 null 即可，框架会正确处理；返回非 null 时该操作人将替代当前操作人执行审批
- **equals 方法**：建议在实现类中重写 equals，基于 userId 判断相等性（框架中 FlowSession 等组件会进行操作人比较）
- **标记接口 IUser**：IFlowOperator 继承自 `com.codingapi.springboot.framework.user.IUser`，该接口无方法定义，仅作类型标记
- **查询入口**：框架内部通过 GatewayContext / FlowScriptContext 的 `getFlowOperator(long)` 和 `findByIds(List<Long>)` 获取 IFlowOperator 实例，业务方需确保这两个查询方法返回正确结果
