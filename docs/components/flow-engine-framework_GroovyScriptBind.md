---
module: flow-engine-framework
name: GroovyScriptBind
description: Groovy 脚本中 $bind 变量的实际绑定对象，包装 FlowScriptContext 的数据访问方法，为脚本提供获取 Spring Bean、流程记录和审批人的能力。
---

# GroovyScriptBind

- **来源**: 自有
- **所属 module**: flow-engine-framework
- **Maven 坐标**: com.codingapi.flow:flow-engine-framework:0.0.28

## 何时使用

GroovyScriptBind 是脚本编写者在流程 Groovy 脚本中通过 `$bind` 变量直接交互的对象。当脚本需要访问 Spring Bean、查询流程流转记录或获取审批人信息时，通过 `$bind` 调用相应方法。脚本开发者无需关心此类的构造方式，只需在脚本中使用 `$bind.方法名()` 即可。

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
| `GroovyScriptBind` | `com.codingapi.flow.script.request` | $bind 绑定对象，包装 FlowScriptContext 的所有公共方法 |
| `FlowScriptContext` | `com.codingapi.flow.script.runtime` | 被委托的上下文单例，GroovyScriptBind 的构造参数 |
| `ScriptRuntimeContext` | `com.codingapi.flow.script.runtime` | 脚本执行引擎，负责创建 GroovyScriptBind 并绑定到脚本 |

### 关键方法

#### GroovyScriptBind（在 Groovy 脚本中以 `$bind` 变量名访问）

| 方法签名 | 参数说明 | 返回值 | 说明 |
|----------|----------|--------|------|
| `getBean(Class<T>)` | clazz - Bean 类型 | `T` | 按类型获取 Spring Bean，委托 FlowScriptContext |
| `getBean(String, Class<T>)` | name - Bean 名称, clazz - Bean 类型 | `T` | 按名称和类型获取 Spring Bean |
| `getBeans(Class<T>)` | clazz - Bean 类型 | `List<T>` | 获取某类型的所有 Bean |
| `getRecordById(long)` | id - 流程记录 ID | `FlowRecord` | 根据 ID 获取流程流转记录 |
| `getOperatorById(long)` | userId - 用户 ID | `IFlowOperator` | 根据 ID 获取流程操作人 |
| `findOperatorsByIds(List<Long>)` | ids - 用户 ID 列表 | `List<IFlowOperator>` | 批量查询操作人 |

### 构造方式

GroovyScriptBind 不由业务代码直接构造，由 `ScriptRuntimeContext.execute()` 在每次脚本执行时自动创建：

```java
// ScriptRuntimeContext.execute() 内部
runtime.setProperty("$bind", new GroovyScriptBind(FlowScriptContext.getInstance()));
```

### 配置项

GroovyScriptBind 自身无配置项。数据访问能力取决于 FlowScriptContext 中 IBeanFactory 的配置。

## 使用示例

### 在脚本中获取审批人

```groovy
def run(request){
    def operator = $bind.getOperatorById(request.getCreatedOperatorId())
    return operator.getName()
}
```

### 在脚本中获取 Spring Bean

```groovy
def run(request){
    def myService = $bind.getBean(com.example.MyService.class)
    return myService.checkCondition(request.getFormData())
}
```

### 在脚本中查询流程记录

```groovy
def run(request){
    def record = $bind.getRecordById(1001)
    return record.getNodeName()
}
```

### 在脚本中批量查询操作人

```groovy
def run(request){
    def operators = $bind.findOperatorsByIds([1, 2, 3])
    return operators.collect { it.getName() }
}
```

### 在脚本中获取所有某类型的 Bean

```groovy
def run(request){
    def handlers = $bind.getBeans(com.example.EventHandler.class)
    return handlers.size()
}
```

## 注意事项

- **不可直接构造**：GroovyScriptBind 由 ScriptRuntimeContext 在脚本执行前自动创建并注入，业务代码不应直接实例化
- **纯委托模式**：GroovyScriptBind 自身无状态，所有方法调用直接委托给构造时传入的 FlowScriptContext 单例
- **生命周期**：每次 `ScriptRuntimeContext.execute()` 调用都会创建新的 GroovyScriptBind 实例，随脚本执行结束而回收
- **脚本变量名**：在 Groovy 脚本中固定以 `$bind` 变量名访问，该名称由 ScriptRuntimeContext 硬编码设置
- **方法与 FlowScriptContext 一一对应**：GroovyScriptBind 暴露的方法签名和语义与 FlowScriptContext 完全一致，参见 FlowScriptContext 组件文档
