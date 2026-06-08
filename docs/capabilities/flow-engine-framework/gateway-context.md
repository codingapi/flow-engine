---
name: flow-engine-framework/gateway-context
module: flow-engine-framework
description: 网关上下文，提供操作者网关的注入和线程级缓存
status: 已实现
scope: 后端
source: 项目自有
import: "com.codingapi.flow:flow-engine-framework"
symbols:
  - GatewayContext
  - FlowOperatorGateway
  - FlowOperatorLocalThreadCache
content_hash: 9a35fe960766c3a944ad60043ea71ccd5915e14add45650eed22e6771b95473b
---

## 解决什么问题

提供流程引擎与外部系统集成的网关抽象，解决以下问题：

- **操作者信息获取**：`FlowOperatorGateway` 抽象操作者（用户）信息的获取，框架不依赖具体的用户系统
- **网关上下文注入**：`GatewayContext` 作为单例持有网关实现，通过 Spring 自动配置注入
- **线程级缓存**：`FlowOperatorLocalThreadCache` 在同一次请求中缓存操作者信息，避免重复查询

## 如何使用

### 核心组件

| 组件 | 职责 |
|------|------|
| `FlowOperatorGateway` | 网关接口，定义 `get(userId)` 和 `findByIds(ids)` 方法 |
| `GatewayContext` | 单例上下文，持有 `FlowOperatorGateway` 实现 |
| `FlowOperatorLocalThreadCache` | ThreadLocal 缓存，同请求内缓存操作者信息 |

### FlowOperatorGateway 接口

业务系统需实现此接口以对接自身的用户系统：

```java
public interface FlowOperatorGateway {
    IFlowOperator get(long userId);
    List<IFlowOperator> findByIds(List<Long> ids);
}
```

### 缓存机制

- 每次 `FlowService` 操作前调用 `FlowOperatorLocalThreadCache.clear()` 清理缓存
- 同一请求内，通过 `GatewayContext.getFlowOperator(userId)` 获取操作者时自动缓存
- 批量查询通过 `GatewayContext.findByIds(ids)` 获取，同样走缓存

## 使用实例

```java
// 1. 实现网关接口（在业务项目中）
@Component
public class FlowOperatorGatewayImpl implements FlowOperatorGateway {

    @Autowired
    private UserRepository userRepository;

    @Override
    public IFlowOperator get(long userId) {
        User user = userRepository.findById(userId);
        return new FlowOperator(user.getId(), user.getName(), user.getDepartment());
    }

    @Override
    public List<IFlowOperator> findByIds(List<Long> ids) {
        return userRepository.findAllById(ids).stream()
            .map(u -> new FlowOperator(u.getId(), u.getName(), u.getDepartment()))
            .toList();
    }
}

// 2. 通过 GatewayContext 获取操作者（自动缓存）
IFlowOperator operator = GatewayContext.getInstance().getFlowOperator(userId);

// 3. 批量获取
List<IFlowOperator> operators = GatewayContext.getInstance().findByIds(userIds);
```
