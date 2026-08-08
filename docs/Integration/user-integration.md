# 流程用户体系集成

Flow Engine 不与任何具体用户体系绑定。引擎通过**防腐层接口**访问业务方用户，业务方只需实现两个接口并注册为 Spring Bean，即可对接自己的用户体系。

## 1. 核心接口

### 1.1 `FlowOperatorGateway`（操作人网关）

文件：`flow-engine-framework/src/main/java/com/codingapi/flow/gateway/FlowOperatorGateway.java`

```java
package com.codingapi.flow.gateway;

/** 流程操作者防腐层 */
public interface FlowOperatorGateway {
    IFlowOperator get(long id);
    List<IFlowOperator> findByIds(List<Long> ids);
}
```

引擎所有「按 id 取用户 / 批量取用户」的请求都汇聚到这个接口。**框架不提供默认实现**（`flow-engine-starter-infra` 也不提供），必须由业务方提供一个 Spring Bean。

### 1.2 `IFlowOperator`（流程操作人）

文件：`flow-engine-framework/src/main/java/com/codingapi/flow/operator/IFlowOperator.java`

```java
package com.codingapi.flow.operator;

import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.springboot.framework.user.IUser;

/** 流程参与用户 */
public interface IFlowOperator extends IUser {
    long getUserId();                                      // 用户ID
    String getName();                                      // 用户名称
    boolean isFlowManager();                               // 流程管理员可强制干预流程
    IFlowOperator forwardOperator(GroovyScriptRequest request); // 转交审批人；无需转交返回 null
}
```

要点：

- 父接口 `IUser`（来自外部依赖 `com.codingapi.springboot:springboot-starter`）为**空标记接口**，无需实现任何方法。
- `isFlowManager()` 返回 `true` 表示流程管理员：开启干预策略时，管理员可强制审批他人待办（见 [扩展点](./extension-points.md#工作流策略)）。
- `forwardOperator(request)` 实现「转交审批」（转办链）：返回转交目标操作人，无需转交时返回 `null`。引擎在审批时调用 `session.loadFinalForwardOperator(...)` 递归解析转交链。

## 2. 引擎内部如何取用户

用户读取统一经过 `GatewayContext` 单例（`flow-engine-framework/.../context/GatewayContext.java`），并带线程级缓存：

```java
public class GatewayContext {
    @Getter private final static GatewayContext instance = new GatewayContext();
    @Setter @Getter private FlowOperatorGateway flowOperatorGateway;

    public IFlowOperator getFlowOperator(long userId) {
        return FlowOperatorLocalThreadCache.getInstance().get(userId, () -> flowOperatorGateway.get(userId));
    }
    public List<IFlowOperator> findByIds(List<Long> ids) {
        return FlowOperatorLocalThreadCache.getInstance().find(ids, (idList) -> flowOperatorGateway.findByIds(idList));
    }
}
```

- **注入方式**：`GatewayContext.getInstance().setFlowOperatorGateway(bean)`，由 starter 的 `GatewayContextRegister`（`InitializingBean`）在启动时执行。
- **线程缓存** `FlowOperatorLocalThreadCache`（`flow-engine-framework/.../cache/FlowOperatorLocalThreadCache.java`）：`ThreadLocal<Map<Long, IFlowOperator>>`，未命中时调用网关并回写；`find` 会过滤 null/<=0 的 id、去重、保序返回（保证多人审批顺序稳定）。`FlowService` 每个操作入口都会 `clear()` 该缓存。

## 3. 集成示例（完整可运行）

以 `flow-engine-example` 模块为范本。

### 3.1 用户实体实现 `IFlowOperator`

`flow-engine-example/src/main/java/com/codingapi/example/entity/User.java`：

```java
package com.codingapi.example.entity;

import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.springboot.script.annotation.ScriptType;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "t_user")
@Data
@ScriptType(description = "用户信息")   // 暴露给 Groovy 脚本的类型元数据
public class User implements IFlowOperator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    /** 是否流程管理员 */
    private boolean flowManager;
    /** 转交审批人 id（>0 时启用转交） */
    private Long flowOperatorId;
    @Column(unique = true)
    private String account;
    private String password;

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

> 业务方完全可以用普通 POJO/DTO 实现 `IFlowOperator`，不必是 JPA 实体。

### 3.2 实现 `FlowOperatorGateway`

`flow-engine-example/src/main/java/com/codingapi/example/gateway/impl/FlowOperatorGatewayImpl.java`：

```java
package com.codingapi.example.gateway.impl;

import com.codingapi.example.repository.UserRepository;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.operator.IFlowOperator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

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
        return userRepository.findUserByIdIn(ids).stream()
                .map(user -> (IFlowOperator) user)
                .toList();
    }
}
```

配套仓储（基于 `FastRepository`，来自 `springboot-starter-data-fast`）：

```java
public interface UserRepository extends FastRepository<User, Long> {
    User getUserById(long id);
    List<User> findUserByIdIn(List<Long> ids);
    User getUserByAccount(String account);
}
```

### 3.3 注册

`FlowOperatorGateway` 作为 Spring Bean 注册后，starter 的 `GatewayContextRegister` 自动完成注入，业务代码无需干预。

## 4. 当前登录人（REST 层上下文）

REST API 层通过外部框架的 `UserContext`（`com.codingapi.springboot.framework.user.UserContext`）获取当前登录人：

```java
IFlowOperator currentOperator = (IFlowOperator) UserContext.getInstance().current();
```

- `UserContext` 由外部框架（`springboot-starter-security` 等安全方案）负责填充，业务方需自行接入登录态。
- 示例应用通过 `MyAuthenticationTokenFilter`（Spring Security + Token）在认证后将用户写入 `UserContext`。
- REST 层同时支持请求参数 `operatorId` 显式指定当前用户（见 [REST API](./rest-api.md#操作人解析规则)），便于无登录态集成与测试。

## 5. 流程管理员（flowManager）的作用

- 开启**干预策略**（`InterfereStrategy`，默认开启）后，`WorkflowStrategyManager.verifyOperator` 校验操作人与待办持有者一致；**流程管理员可越过该校验**，强制审批任意待办。
- 示例应用通过 `AdminInitializer`（`ApplicationRunner`）初始化内置 `admin` 账号（`flowManager = true`）。

## 6. 转交审批链

当用户实体配置了 `forwardOperator`（转交人）时，引擎动作执行前会解析转交链：

```
session.loadFinalForwardOperator(currentOperator)
  → currentOperator.forwardOperator(request)
    → 返回目标 → 继续递归解析该目标的 forwardOperator
      → 返回 null → 最终审批人
```

- 转交目标也通过 `GatewayContext` 获取（同样带线程缓存）。
- 转交覆盖了「当前操作人」：`FlowRecord.currentOperatorId/Name` 记录最终实际审批人。