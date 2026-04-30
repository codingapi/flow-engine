---
module: flow-engine-framework
name: FlowScriptContext
description: Groovy 脚本运行时的 $bind 上下文对象，为脚本提供获取 Spring Bean、流程记录、审批人等数据能力的统一入口。
---

# FlowScriptContext

- **来源**: 自有
- **所属 module**: flow-engine-framework
- **Maven 坐标**: com.codingapi.flow:flow-engine-framework:0.0.28

## 何时使用

在 Groovy 流程脚本中需要访问外部数据时使用。FlowScriptContext 以 `$bind` 对象的形式注入到脚本运行环境，脚本可通过它获取 Spring Bean、流程记录（FlowRecord）、审批人（IFlowOperator）等业务数据，从而实现动态条件判断、人员加载、路由选择等逻辑。

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
| `FlowScriptContext` | `com.codingapi.flow.script.runtime` | 脚本运行时 $bind 上下文，单例模式，委托 IBeanFactory 提供数据访问能力 |
| `IBeanFactory` | `com.codingapi.flow.script.runtime` | Bean 工厂接口，定义 getBean / getRecordById / getOperatorById 等方法 |
| `GroovyScriptBind` | `com.codingapi.flow.script.request` | Groovy 脚本中 $bind 变量的实际类型，包装 FlowScriptContext 的方法调用 |
| `ScriptRuntimeContext` | `com.codingapi.flow.script.runtime` | Groovy 脚本执行引擎，负责创建 GroovyShell、绑定 $bind 对象并执行脚本 |
| `GatewayContext` | `com.codingapi.flow.context` | 网关上下文单例，IBeanFactory 默认通过它实现操作人查询 |

### 关键方法

#### FlowScriptContext（单例，通过 `FlowScriptContext.getInstance()` 获取）

| 方法签名 | 参数说明 | 返回值 | 说明 |
|----------|----------|--------|------|
| `static getInstance()` | 无 | `FlowScriptContext` | 获取单例实例 |
| `setBeanFactory(IBeanFactory)` | beanFactory - Spring 容器适配实现 | `void` | 设置 Bean 工厂（通常在 Spring 启动时由框架自动注入） |
| `getBean(Class<T>)` | clazz - Bean 类型 | `T` | 按类型获取 Spring Bean |
| `getBean(String, Class<T>)` | name - Bean 名称, clazz - Bean 类型 | `T` | 按名称和类型获取 Spring Bean |
| `getBeans(Class<T>)` | clazz - Bean 类型 | `List<T>` | 获取某类型的所有 Bean |
| `getRecordById(long)` | id - 流程记录 ID | `FlowRecord` | 根据 ID 获取流程流转记录 |
| `getOperatorById(long)` | userId - 用户 ID | `IFlowOperator` | 根据 ID 获取流程操作人 |
| `findOperatorsByIds(List<Long>)` | ids - 用户 ID 列表 | `List<IFlowOperator>` | 批量查询操作人 |

#### IBeanFactory 接口（FlowScriptContext 的委托目标）

| 方法签名 | 默认行为 | 说明 |
|----------|----------|------|
| `getBean(Class<T>)` | 返回 null | 需由 Spring 适配实现覆盖 |
| `getBean(String, Class<T>)` | 返回 null | 需由 Spring 适配实现覆盖 |
| `getBeans(Class<T>)` | 返回 null | 需由 Spring 适配实现覆盖 |
| `getRecordById(long)` | 返回 null | 需由基础设施层覆盖 |
| `getOperatorById(long)` | 委托 GatewayContext 查询 | 默认即可用 |
| `findOperatorsByIds(List<Long>)` | 委托 GatewayContext 查询 | 默认即可用 |

### 配置项

FlowScriptContext 自身无配置项。相关配置来自 ScriptRuntimeContext：

| 配置Key | 类型 | 默认值 | 说明 |
|---------|------|--------|------|
| `flow.script.cleanup.interval` | int (系统属性) | 300 | 脚本锁缓存自动清理间隔（秒） |

## 使用示例

### 基础用法（在 Groovy 脚本中使用 $bind）

```groovy
// 获取审批人信息
def run(request){
    def operator = $bind.getOperatorById(request.getCreatedOperatorId())
    return operator.getName()
}
```

### 在脚本中获取 Spring Bean

```groovy
// 通过 $bind 调用 Spring Bean 实现业务逻辑
def run(request){
    def myService = $bind.getBean(com.example.MyService.class)
    return myService.checkCondition(request.getFormData())
}
```

### 查询流程记录

```groovy
// 根据记录 ID 获取流程流转记录
def run(request){
    def record = $bind.getRecordById(1001)
    return record.getNodeName()
}
```

### 批量查询操作人

```groovy
// 批量获取操作人信息
def run(request){
    def operators = $bind.findOperatorsByIds([1, 2, 3])
    return operators.collect { it.getName() }
}
```

## 注意事项

- **单例模式**：FlowScriptContext 是全局单例，通过 `getInstance()` 获取，需在 Spring 容器启动时通过 `setBeanFactory()` 注入实际的 Bean 工厂实现
- **默认空实现**：构造时内置的 IBeanFactory 所有方法返回 null（getOperatorById / findOperatorsByIds 除外，它们委托 GatewayContext）。需由 starter-infra 层提供 Spring 适配实现
- **线程安全**：FlowScriptContext 本身通过 `setBeanFactory` 注入后不再变化，读取操作委托给 IBeanFactory，线程安全性取决于 IBeanFactory 实现
- **脚本绑定**：FlowScriptContext 并不直接暴露给脚本，而是通过 GroovyScriptBind 包装后以 `$bind` 变量名注入到 Groovy 脚本执行环境（参见 ScriptRuntimeContext.execute() 方法）
