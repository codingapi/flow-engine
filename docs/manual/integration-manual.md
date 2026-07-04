# Flow Engine 集成手册

本手册面向后端开发者，介绍如何将 Flow Engine 集成到一个 Spring Boot 3 项目中。集成完成后，项目即可获得流程设计、流转、待办已办、催办撤销等全部工作流能力。

## 1. 环境要求

| 项 | 要求 |
|----|------|
| JDK | 17 及以上 |
| Spring Boot | 3.5.x |
| 持久化 | Spring Data JPA（Hibernate） |
| 数据库 | 默认 H2，已验证支持达梦 DM；理论上支持任意 JPA 兼容数据库 |
| 构建工具 | Maven |

## 2. 引入依赖

### 2.1 一站式引入（推荐）

业务项目通常只需要引入 `flow-engine-starter` 单个依赖，它会传递依赖 framework、infra、api、query 四个子模块：

```xml
<dependency>
    <groupId>com.codingapi.flow</groupId>
    <artifactId>flow-engine-starter</artifactId>
    <version>0.0.52</version>
</dependency>
```

`flow-engine-starter` 通过 Spring Boot 自动装配机制（`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`）注册以下 `AutoConfiguration`：

| 自动配置类 | 来源模块 | 职责 |
|-----------|---------|------|
| `com.codingapi.flow.AutoConfiguration` | flow-engine-starter | 注册 `FlowService`、`WorkflowService`、`FlowRecordService`、`RepositoryHolderContext`、网关上下文、脚本上下文、延迟任务 Runner |
| `com.codingapi.flow.infra.AutoConfiguration` | flow-engine-starter-infra | 注册 10 个 JPA 仓储实现，并通过 `FlowJpaPackageRegistrar` 注册 JPA 扫描包 `com.codingapi.flow.infra` |
| `com.codingapi.flow.api.AutoConfiguration` | flow-engine-starter-api | 扫描 `com.codingapi.flow.api`，注册命令类 Controller |
| `com.codingapi.flow.query.AutoConfiguration` | flow-engine-starter-query | 扫描 `com.codingapi.flow.query`，注册查询类 Controller |

### 2.2 按需引入（可选）

若只想使用部分能力（例如只接查询、不暴露命令 API），可分别引入对应子模块：

```xml
<dependency>
    <groupId>com.codingapi.flow</groupId>
    <artifactId>flow-engine-framework</artifactId>
    <version>0.0.52</version>
</dependency>
<!-- 持久化层（JPA 实现），承载仓储 -->
<dependency>
    <groupId>com.codingapi.flow</groupId>
    <artifactId>flow-engine-starter-infra</artifactId>
    <version>0.0.52</version>
</dependency>
<!-- 命令 API（流程发起/审批等） -->
<dependency>
    <groupId>com.codingapi.flow</groupId>
    <artifactId>flow-engine-starter-api</artifactId>
    <version>0.0.52</version>
</dependency>
<!-- 查询 API（待办/已办/抄送） -->
<dependency>
    <groupId>com.codingapi.flow</groupId>
    <artifactId>flow-engine-starter-query</artifactId>
    <version>0.0.52</version>
</dependency>
```

> `flow-engine-starter-infra` 依赖 `springboot-starter-data-fast`，会自动引入 Spring Data JPA。若项目已自行引入 JPA，注意依赖版本一致性。

## 3. 必需配置

### 3.1 application.properties

Flow Engine 本身无强制配置项，但需要保证以下 Spring Data JPA 配置正确：

```properties
# 数据源
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.url=jdbc:h2:file:./flow.db
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect

# 表结构自动维护（建议生产环境改为 validate 或使用 Flyway/Liquibase）
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### 3.2 达梦 DM 数据库适配

项目已内置达梦方言支持，参考 `flow-engine-example`：

```properties
spring.datasource.url=jdbc:dm://localhost:5236
spring.datasource.username=SYSDBA
spring.datasource.password=SYSDBA001
spring.datasource.driverClassName=dm.jdbc.driver.DmDriver
spring.jpa.properties.hibernate.dialect=com.codingapi.example.dialect.HydDmDialect

# 达梦驱动与方言依赖
# <artifactId>DmJdbcDriver11</artifactId>
# <artifactId>DmDialect-for-hibernate6.6</artifactId>
```

> 自定义方言示例见 `flow-engine-example/src/main/java/com/codingapi/example/dialect/HydDmDialect.java`，它继承 `org.hibernate.dialect.Dialect`，针对达梦对 Hibernate 6.6 的部分语法做了修正。

## 4. 必需实现的接口

Flow Engine 的领域模型与具体用户体系解耦，集成方**必须**实现以下两个接口，否则引擎无法识别"谁在操作流程"以及"如何加载审批人"。

### 4.1 `IFlowOperator`（流程参与用户）

该接口定义流程的参与用户，业务系统的用户实体应实现它：

```java
package com.codingapi.flow.operator;

public interface IFlowOperator extends IUser {  // IUser 来自 com.codingapi.springboot.framework

    long getUserId();            // 用户 ID

    String getName();            // 用户名称

    boolean isFlowManager();     // 是否流程管理员（可强制干预流程）

    // 转交审批人：不为空时，当前操作者将由返回的审批人执行
    // 可根据 GroovyScriptRequest（表单数据/当前节点）动态决定转交人，无需转交返回 null
    IFlowOperator forwardOperator(GroovyScriptRequest request);
}
```

示例（`flow-engine-example` 中的 `User` 实体）：

```java
@Entity
public class User implements IFlowOperator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private boolean flowManager;

    @Override
    public long getUserId() { return id; }

    @Override
    public String getName() { return name; }

    @Override
    public boolean isFlowManager() { return flowManager; }

    @Override
    public IFlowOperator forwardOperator(GroovyScriptRequest request) {
        return null; // 默认不转交
    }
    // 省略 getter/setter
}
```

### 4.2 `FlowOperatorGateway`（操作者防腐层）

该接口是流程引擎访问用户体系的唯一入口，集成方需提供一个 Spring Bean 实现：

```java
package com.codingapi.flow.gateway;

public interface FlowOperatorGateway {
    IFlowOperator get(long id);
    List<IFlowOperator> findByIds(List<Long> ids);
}
```

示例实现：

```java
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
```

> 此 Bean 由 `flow-engine-starter` 的 `GatewayContextRegister` 注入到 `GatewayContext` 单例，引擎在加载审批人、转办人、加签人时统一通过它查询。**实现该 Bean 是集成成功的关键步骤。**

### 4.3 当前操作者上下文（重要）

命令类 Controller 通过 `UserContext.getInstance().current()` 获取当前登录用户作为操作者。集成方需要：

1. 在鉴权过滤器中，将认证后的用户（`IFlowOperator` 实例）写入 `UserContext`。
2. 示例见 `flow-engine-example/src/main/java/com/codingapi/example/security/MyAuthenticationTokenFilter.java`。

> 调试时可跳过登录态：在请求参数中传 `operatorId=123` 直接指定操作者 ID，详见《使用手册》"调试技巧"。

## 5. 可选实现：脚本持久化

流程节点中的路由、条件分支、表单权限等运行时逻辑由 Groovy 脚本驱动。脚本可**内联**在流程定义 JSON 中，也可**独立持久化**为命名脚本重复引用。若需后者，实现 `NodeViewJavaScriptRepository` 接口并注册为 Bean：

```java
public interface NodeViewJavaScriptRepository {
    void save(NodeViewJavaScript javaScript);
    void delete(String code);
    NodeViewJavaScript get(String code);
}
```

`flow-engine-starter` 的 `NodeViewJavaScriptCacheContextRegister` 会自动扫描该 Bean 并加载脚本缓存。`flow-engine-example` 提供了基于 JPA 的实现（`NodeViewJavaScriptRepositoryImpl`），可直接参考。

> 若不需要脚本独立存储，可不实现，引擎会使用流程定义 JSON 中内联的脚本。

## 6. 扩展点

### 6.1 事件订阅

引擎在流程关键节点推送 7 种事件，业务方通过实现 `IHandler<E>` 订阅（无需手动注册，被 Spring 扫描即可）：

| 事件 | 触发时机 |
|------|---------|
| `FlowRecordStartEvent` | 流程发起 |
| `FlowRecordTodoEvent` | 产生待办 |
| `FlowRecordDoneEvent` | 办理完成（已办） |
| `FlowRecordFinishEvent` | 流程结束 |
| `FlowRecordUrgeEvent` | 催办 |
| `FlowRecordRevokeEvent` | 撤销 |
| `FlowRecordDeleteEvent` | 删除流程 |

示例（催办事件处理）：

```java
@Slf4j
@Component
public class MyFlowRecordUrgeEventHandler implements IHandler<FlowRecordUrgeEvent> {
    @Override
    public void handler(FlowRecordUrgeEvent event) {
        log.info("催办 event:{}", event);
        // 例如：发送站内信 / 邮件 / 钉钉通知
    }
}
```

### 6.2 自定义节点策略

节点行为由策略驱动（`strategy/node/` 下 15+ 策略），常见策略如 `OperatorLoadStrategy`（审批人加载）、`RecordMergeStrategy`（记录合并）、`RevokeStrategy`（撤销）、`RouterStrategy`（路由）等。详见 capability 文档 [node-strategy](../capabilities/flow-engine-framework/node-strategy.md)。

### 6.3 自定义动作

内置 8 种动作（通过/拒绝/退回/转办/委派/加签/保存/自定义）。当 `CustomAction` 不满足需求时，可扩展 `BaseAction` 实现自定义动作，详见 [action](../capabilities/flow-engine-framework/action.md)。

### 6.4 自定义脚本注册

引擎默认使用 `DefaultScriptRegistry` 提供路由、分支等内置 Groovy 脚本。如需替换默认脚本：

```java
ScriptRegistryContext.getInstance().setRegistry(new CustomScriptRegistry());
```

`CustomScriptRegistry` 实现 `IScriptRegistry` 接口即可。

## 7. 集成验证清单

完成上述步骤后，按清单逐项验证：

- [ ] 项目可正常启动，无 Bean 缺失异常
- [ ] 数据库生成 Flow Engine 相关表（`flow_record`、`flow_todo_record`、`workflow`、`workflow_version` 等）
- [ ] 实现了 `FlowOperatorGateway` 并能根据 ID 查到用户
- [ ] 用户实体实现了 `IFlowOperator`
- [ ] 鉴权过滤器将当前用户写入 `UserContext`
- [ ] 调用 `POST /api/cmd/workflow/create` 能创建空流程定义
- [ ] 调用 `GET /api/query/record/todo` 不报错
- [ ] （可选）实现 `NodeViewJavaScriptRepository` 支持脚本存储

全部通过即集成成功，可进入《使用手册》开始业务流程开发。

## 8. 参考：示例应用

`flow-engine-example` 模块是一个开箱即用的完整集成示例，包含：

| 文件 | 作用 |
|------|------|
| `ServerApplication.java` | 启动类 |
| `gateway/impl/FlowOperatorGatewayImpl.java` | 操作者网关实现 |
| `entity/User.java` | 实现 `IFlowOperator` 的用户实体 |
| `security/MyAuthenticationTokenFilter.java` | 鉴权过滤器，写入 `UserContext` |
| `repository/impl/NodeViewJavaScriptRepositoryImpl.java` | 脚本持久化实现 |
| `handler/MyFlowRecordUrgeEventHandler.java` | 催办事件订阅示例 |
| `runner/AdminInitializer.java` | 初始化管理员账号 |
| `dialect/HydDmDialect.java` | 达梦方言 |
| `application.properties` | H2 配置 |
| `application-dm.properties` | 达梦配置 |

启动示例：

```bash
# H2 模式（默认）
cd flow-engine-example
mvn spring-boot:run

# 达梦模式
mvn spring-boot:run -Dspring-boot.run.profiles=dm
```

启动后访问 `http://localhost:8090`。
