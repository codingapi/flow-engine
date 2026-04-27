# FlowScriptContext

## 何时使用

当需要在 Groovy 脚本中访问 Spring Bean、流程记录或操作人数据时使用。`FlowScriptContext` 是脚本运行时的 `$bind` 上下文对象，由 `ScriptRuntimeContext` 在每次脚本执行时注入为 `$bind` 变量。典型场景：

- 在自定义 Groovy 脚本中查询流程记录（`$bind.getRecordById(...)`）
- 在脚本中获取操作人信息（`$bind.getOperatorById(...)`）
- 在脚本中调用 Spring Bean（`$bind.getBean(...)`）
- 需要替换脚本上下文的 Bean 工厂实现（如单元测试中注入 Mock）

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
| `FlowScriptContext` | `com.codingapi.flow.script.runtime` | 脚本运行时 `$bind` 上下文单例，持有 `IBeanFactory` 委托 |
| `IBeanFactory` | `com.codingapi.flow.script.runtime` | Bean 工厂接口，定义 Bean 查询、记录查询、操作人查询方法 |
| `GroovyScriptBind` | `com.codingapi.flow.script.request` | Groovy 脚本绑定对象，包装 `FlowScriptContext` 并作为 `$bind` 注入脚本 |

### 关键方法

#### FlowScriptContext

| 方法签名 | 参数说明 | 返回值 | 说明 |
|----------|----------|--------|------|
| `getInstance()` | 无 | `FlowScriptContext` | 获取单例实例 |
| `setBeanFactory(IBeanFactory)` | `beanFactory` — Bean 工厂实现 | `void` | 设置自定义 Bean 工厂（由 Starter 自动配置或测试手动注入） |
| `getBean(Class<T>)` | `clazz` — Bean 类型 | `T` | 按类型获取 Spring Bean |
| `getBean(String, Class<T>)` | `name` — Bean 名称, `clazz` — Bean 类型 | `T` | 按名称和类型获取 Spring Bean |
| `getBeans(Class<T>)` | `clazz` — Bean 类型 | `List<T>` | 按类型获取所有 Spring Bean |
| `getRecordById(long)` | `id` — 记录 ID | `FlowRecord` | 根据 ID 获取流程记录 |
| `getOperatorById(long)` | `userId` — 操作人 ID | `IFlowOperator` | 根据 ID 获取操作人 |
| `findOperatorsByIds(List<Long>)` | `ids` — 操作人 ID 列表 | `List<IFlowOperator>` | 批量获取操作人 |

#### IBeanFactory 接口默认实现

| 方法 | 默认行为 | 说明 |
|------|----------|------|
| `getBean(Class<T>)` | 返回 `null` | 需由 Spring 环境实现覆盖 |
| `getBean(String, Class<T>)` | 返回 `null` | 需由 Spring 环境实现覆盖 |
| `getBeans(Class<T>)` | 返回 `null` | 需由 Spring 环境实现覆盖 |
| `getRecordById(long)` | 返回 `null` | 需由 Spring 环境实现覆盖 |
| `getOperatorById(long)` | 委托给 `GatewayContext` | 默认通过网关上下文获取操作人 |
| `findOperatorsByIds(List<Long>)` | 委托给 `GatewayContext` | 默认通过网关上下文批量获取 |

## 使用示例

```groovy
// 在 Groovy 脚本中使用 $bind（脚本由 ScriptRuntimeContext 自动注入）
def run(request) {
    // 查询流程记录
    def record = $bind.getRecordById(request.getRecordId())

    // 获取操作人信息
    def operator = $bind.getOperatorById(record.getCurrentOperatorId())

    // 调用自定义 Spring Bean
    def notifyService = $bind.getBean(com.example.NotifyService)
    notifyService.send(operator.getEmail(), "您有一条待办")

    return 'PASS'
}
```

```java
// Spring Boot 自动配置中注册（flow-engine-starter 内部调用）
@Component
public class FlowScriptContextRegister implements InitializingBean {

    private final ApplicationContext applicationContext;
    private final FlowOperatorGateway flowOperatorGateway;
    private final FlowRecordRepository flowRecordRepository;

    @Override
    public void afterPropertiesSet() {
        FlowScriptContext.getInstance().setBeanFactory(new IBeanFactory() {
            @Override
            public <T> T getBean(Class<T> clazz) {
                return applicationContext.getBean(clazz);
            }
            @Override
            public <T> T getBean(String name, Class<T> clazz) {
                return applicationContext.getBean(name, clazz);
            }
            @Override
            public <T> List<T> getBeans(Class<T> clazz) {
                return new ArrayList<>(applicationContext.getBeansOfType(clazz).values());
            }
            @Override
            public FlowRecord getRecordById(long id) {
                return flowRecordRepository.getFlowRecord(id);
            }
        });
    }
}
```

```java
// 单元测试中手动注入
FlowScriptContext.getInstance().setBeanFactory(new IBeanFactory() {
    @Override
    public FlowRecord getRecordById(long id) {
        return testRecordRepository.getFlowRecord(id);
    }
});
```

## 注意事项

- **注入机制**：`FlowScriptContext` 本身不执行查询逻辑，全部委托给 `IBeanFactory`。生产环境由 `FlowScriptContextRegister` 在 Spring 启动时自动注入，测试环境需手动设置
- **`$bind` 绑定**：`GroovyScriptBind` 包装 `FlowScriptContext` 实例，由 `ScriptRuntimeContext.execute()` 自动注入为脚本中的 `$bind` 变量，无需手动创建
- **线程安全性**：单例使用饿汉式初始化，`setBeanFactory()` 无同步保护，应在应用启动阶段完成设置
- **默认实现**：`IBeanFactory` 的操作人查询方法默认委托给 `GatewayContext`，而 Bean 和记录查询默认返回 `null`，必须由 Spring 环境覆盖
- **Groovy 脚本中的使用**：脚本内直接使用 `$bind` 关键字，无需 import 或显式获取实例
