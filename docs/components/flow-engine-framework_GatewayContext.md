---
module: flow-engine-framework
name: GatewayContext
description: 流程操作者网关上下文，以防腐层模式（Anti-Corruption Layer）隔离框架与业务系统对用户数据的访问，是框架内部获取审批人信息的统一入口。
---

# GatewayContext

- **来源**: 自有
- **所属 module**: flow-engine-framework
- **Maven 坐标**: com.codingapi.flow:flow-engine-framework:0.0.28

## 何时使用

GatewayContext 是框架内部获取流程操作人（IFlowOperator）数据的统一入口。当业务系统集成 flow-engine 时，需通过 `setFlowOperatorGateway()` 注入实现了 `FlowOperatorGateway` 接口的自有用户查询逻辑，使框架在脚本执行、流程启动、动作处理等环节能正确查询到业务系统的用户信息。

## 如何引用

### Maven 坐标

```xml
<dependency>
    <groupId>com.codingapi.flow</groupId>
    <artifactId>flow-engine-framework</artifactId>
    <version>0.0.30</version>
</dependency>
```

## API 说明

### 核心类

| 类名 | 包路径 | 说明 |
|------|--------|------|
| `GatewayContext` | `com.codingapi.flow.context` | 网关上下文单例，持有 FlowOperatorGateway 引用，提供操作人查询的统一入口 |
| `FlowOperatorGateway` | `com.codingapi.flow.gateway` | 操作者防腐层接口，隔离框架与业务系统的用户数据访问 |
| `IFlowOperator` | `com.codingapi.flow.operator` | 流程操作人接口，网关查询的返回类型 |

### 关键方法

#### GatewayContext（单例，通过 `GatewayContext.getInstance()` 获取）

| 方法签名 | 参数说明 | 返回值 | 说明 |
|----------|----------|--------|------|
| `static getInstance()` | 无 | `GatewayContext` | 获取单例实例 |
| `setFlowOperatorGateway(FlowOperatorGateway)` | gateway - 业务系统实现的网关 | `void` | 注入操作者网关实现（Spring 启动时由基础设施层自动注入） |
| `getFlowOperatorGateway()` | 无 | `FlowOperatorGateway` | 获取当前网关实例 |
| `getFlowOperator(long)` | userId - 用户 ID | `IFlowOperator` | 根据用户 ID 查询操作人，委托给 FlowOperatorGateway.get() |
| `findByIds(List<Long>)` | ids - 用户 ID 列表 | `List<IFlowOperator>` | 批量查询操作人，委托给 FlowOperatorGateway.findByIds() |

#### FlowOperatorGateway 接口（需由业务系统实现）

| 方法签名 | 参数说明 | 返回值 | 说明 |
|----------|----------|--------|------|
| `get(long)` | id - 用户 ID | `IFlowOperator` | 根据 ID 获取单个操作人 |
| `findByIds(List<Long>)` | ids - 用户 ID 列表 | `List<IFlowOperator>` | 根据 ID 列表批量获取操作人 |

### 配置项

GatewayContext 自身无配置项。

## 使用示例

### 基础用法（实现 FlowOperatorGateway 并注入）

```java
// 1. 实现防腐层接口
@Repository
@AllArgsConstructor
public class FlowOperatorGatewayImpl implements FlowOperatorGateway {

    private final UserRepository userRepository;

    @Override
    public IFlowOperator get(long id) {
        return userRepository.getUserById(id);
    }

    @Override
    public List<IFlowOperator> findByIds(List<Long> ids) {
        return userRepository.findUserByIdIn(ids)
                .stream()
                .map(user -> (IFlowOperator) user)
                .toList();
    }
}

// 2. 在 Spring 配置中将网关注入到 GatewayContext
@Configuration
public class FlowEngineConfig {

    @Autowired
    public void configureGateway(FlowOperatorGateway gateway) {
        GatewayContext.getInstance().setFlowOperatorGateway(gateway);
    }
}
```

### 框架内部调用场景

```java
// 在 Groovy 脚本 $bind 中获取操作人（IBeanFactory 默认实现）
IFlowOperator operator = GatewayContext.getInstance().getFlowOperator(userId);

// 批量查询（FlowActionRequest 中查询转交审批人）
List<IFlowOperator> operators = GatewayContext.getInstance().findByIds(ids);
```

## 注意事项

- **启动注入**：必须在 Spring 容器启动后、流程引擎首次使用前通过 `setFlowOperatorGateway()` 注入网关实现，否则调用查询方法将产生 NullPointerException
- **单例模式**：全局唯一实例，通过 `getInstance()` 获取，不可直接构造
- **防腐层设计**：FlowOperatorGateway 是框架与业务系统之间的防腐层，业务方只需实现该接口并返回自己的用户对象（需实现 IFlowOperator），框架内部不感知具体的用户数据来源
- **线程安全**：`setFlowOperatorGateway` 通常只在启动阶段调用一次，后续读取操作委托给网关实现，线程安全性取决于业务实现
- **框架内多处引用**：IBeanFactory、Workflow、StartNode、FlowActionRequest 等核心组件均通过 GatewayContext 获取操作人数据，是流程引擎的关键基础设施
