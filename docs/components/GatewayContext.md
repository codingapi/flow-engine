# GatewayContext

## 何时使用

当需要在无法依赖 Spring 注入的框架层代码中查询流程操作人时使用。`GatewayContext` 是操作人网关的全局访问点，通过单例模式使 JPA 转换器、Groovy 脚本、领域对象等非 Spring 管理的代码也能获取操作人信息。典型场景：

- JPA `AttributeConverter` 中将数据库存储的用户 ID 反序列化为 `IFlowOperator`
- 节点执行时解析流程创建人或操作人
- 领域对象（如 `Workflow`）反序列化时重建操作人引用
- Groovy 脚本默认的 `IBeanFactory` 委托操作人查询

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
| `GatewayContext` | `com.codingapi.flow.context` | 操作人网关单例，持有 `FlowOperatorGateway` 引用 |
| `FlowOperatorGateway` | `com.codingapi.flow.gateway` | 操作人防腐层接口，定义操作人查询方法 |

### 关键方法

#### GatewayContext

| 方法签名 | 参数说明 | 返回值 | 说明 |
|----------|----------|--------|------|
| `getInstance()` | 无 | `GatewayContext` | 获取单例实例 |
| `setFlowOperatorGateway(FlowOperatorGateway)` | `flowOperatorGateway` — 网关实现 | `void` | 设置操作人网关（由 Starter 自动配置调用） |
| `getFlowOperatorGateway()` | 无 | `FlowOperatorGateway` | 获取当前网关实例 |
| `getFlowOperator(long)` | `userId` — 操作人 ID | `IFlowOperator` | 根据 ID 获取操作人，委托给 `FlowOperatorGateway.get(long)` |
| `findByIds(List<Long>)` | `ids` — 操作人 ID 列表 | `List<IFlowOperator>` | 批量获取操作人，委托给 `FlowOperatorGateway.findByIds(List)` |

#### FlowOperatorGateway 接口

| 方法签名 | 返回值 | 说明 |
|----------|--------|------|
| `get(long id)` | `IFlowOperator` | 根据 ID 查询单个操作人 |
| `findByIds(List<Long> ids)` | `List<IFlowOperator>` | 根据 ID 列表批量查询操作人 |

## 使用示例

```java
// 基础用法：查询操作人
GatewayContext context = GatewayContext.getInstance();
IFlowOperator operator = context.getFlowOperator(1001L);
List<IFlowOperator> operators = context.findByIds(List.of(1001L, 1002L, 1003L));
```

```java
// Spring Boot 自动配置中注册（flow-engine-starter 内部调用）
@Bean
public GatewayContextRegister gatewayContextRegister(FlowOperatorGateway flowOperatorGateway) {
    return new GatewayContextRegister(flowOperatorGateway);
}

// GatewayContextRegister 在 afterPropertiesSet() 中完成注册：
// GatewayContext.getInstance().setFlowOperatorGateway(flowOperatorGateway);
```

```java
// 单元测试中手动注入
@BeforeEach
void setUp() {
    GatewayContext.getInstance().setFlowOperatorGateway(new FlowOperatorGateway() {
        @Override
        public IFlowOperator get(long id) {
            return new TestOperator(id, "TestUser");
        }
        @Override
        public List<IFlowOperator> findByIds(List<Long> ids) {
            return ids.stream().map(id -> new TestOperator(id, "User" + id)).toList();
        }
    });
}
```

```java
// 实现 FlowOperatorGateway 对接企业用户系统
@Component
public class EnterpriseOperatorGateway implements FlowOperatorGateway {

    private final UserService userService;

    @Override
    public IFlowOperator get(long id) {
        User user = userService.findById(id);
        return new FlowOperatorAdapter(user);
    }

    @Override
    public List<IFlowOperator> findByIds(List<Long> ids) {
        return userService.findByIds(ids).stream()
            .map(FlowOperatorAdapter::new)
            .toList();
    }
}
```

## 注意事项

- **注册时机**：`GatewayContextRegister` 必须在其他使用 `GatewayContext` 的 Bean 之前初始化。`RepositoryHolderContextRegister` 通过构造器依赖 `GatewayContextRegister` 来保证初始化顺序
- **线程安全性**：单例使用饿汉式初始化，`setFlowOperatorGateway()` 无同步保护，应在应用启动阶段完成设置
- **空指针风险**：在调用 `getFlowOperator()` 或 `findByIds()` 前必须确保 `flowOperatorGateway` 已设置，否则会抛出 `NullPointerException`
- **防腐层设计**：`FlowOperatorGateway` 是防腐层接口，业务系统需提供其实现以桥接用户体系，框架本身不包含具体实现
