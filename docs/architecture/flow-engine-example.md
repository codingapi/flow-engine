# flow-engine-example

示例应用模块，演示如何基于 flow-engine-starter 构建完整的工作流应用。包含用户管理、JWT 安全认证、FlowOperatorGateway 实现、流程事件处理等集成示例。

## Maven 坐标

- groupId: com.codingapi.flow
- artifactId: flow-engine-example
- version: 0.0.28
- packaging: jar

## 关联关系

> 以下关系由 `mvn dependency:tree` 指令结果生成，非人工推断。

### 我被哪些模块依赖

无。本模块是 reactor 内最顶层的应用模块，不被任何其他模块依赖。

### 我依赖哪些模块

| 模块 | 说明 |
|------|------|
| flow-engine-starter | 自动配置入口（聚合 framework、infra、api、query） |

### 主要外部依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| com.codingapi.springboot:springboot-starter-security | — | JWT 安全认证框架 |
| io.jsonwebtoken:jjwt-api / jjwt-impl / jjwt-jackson | — | JWT Token 生成与验证 |
| com.dameng:DmJdbcDriver11 | 8.1.4.41033 | 达梦数据库驱动 |
| com.dameng:DmDialect-for-hibernate6.6 | 8.1.4.93 | 达梦 Hibernate 方言 |

## 项目结构

```
com.codingapi.example/
├── ServerApplication.java              # Spring Boot 启动类
├── controller/
│   └── UserController.java                  # 用户管理 API（/api/user）
├── dialect/
│   └── HydDmDialect.java                    # 达梦数据库 Hibernate 方言扩展
├── entity/
│   └── User.java                            # 用户实体（实现 IFlowOperator 接口）
├── gateway/
│   └── impl/
│       └── FlowOperatorGatewayImpl.java     # FlowOperatorGateway 实现（桥接 UserRepository）
├── handler/
│   └── MyFlowRecordUrgeEventHandler.java    # 催办事件处理器示例
├── repository/
│   └── UserRepository.java                  # 用户 JPA Repository
├── runner/
│   └── AdminInitializer.java                # 应用启动时初始化管理员账户
├── security/
│   ├── MyAuthenticationTokenFilter.java      # JWT Token 认证过滤器（设置 UserContext）
│   └── UserDetailServiceImpl.java           # Spring Security UserDetailsService 实现
└── service/
    └── UserService.java                     # 用户业务服务
```

## 核心功能

### 1. 用户管理与流程操作者集成

`User` 实体实现 `IFlowOperator` 接口，同时作为 JPA 实体和流程操作者。`FlowOperatorGatewayImpl` 实现 `FlowOperatorGateway` 接口，将流程引擎的用户查询桥接到 `UserRepository`。

关键类：`com.codingapi.example.entity.User`、`com.codingapi.example.gateway.impl.FlowOperatorGatewayImpl`

### 2. JWT 安全认证

集成 `springboot-starter-security` 提供 JWT 认证：
- `UserDetailServiceImpl` — 根据 account 加载用户并构建 Spring Security UserDetails
- `MyAuthenticationTokenFilter` — 从 Token 中提取用户 ID，查询 User 并设置到 `UserContext`

关键类：`com.codingapi.example.security.*`

### 3. 流程事件处理

`MyFlowRecordUrgeEventHandler` 实现 `IHandler<FlowRecordUrgeEvent>`，演示如何监听流程催办事件。

关键类：`com.codingapi.example.handler.MyFlowRecordUrgeEventHandler`

### 4. 数据库支持

- 默认使用 H2 嵌入式数据库（`application.properties`）
- 通过 `application-dm.properties` 和 `HydDmDialect` 支持达梦数据库

关键配置：`application.properties`

## 对外 API

### UserController

用户管理 API。

路径前缀：`/api/user`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/list` | GET | 用户列表（分页搜索） |
| `/save` | POST | 保存用户（密码加密） |
| `/remove` | POST | 删除用户 |

## 模块规范

- 用户实体实现 `IFlowOperator` 接口，统一身份体系
- `FlowOperatorGateway` 实现类标注 `@Repository`，由 Spring 容器管理
- 安全配置忽略静态资源路径（`/open/**`、CSS、JS、图片等）
- 应用启动时通过 `AdminInitializer` 自动创建 admin 账户

## 构建指令

```bash
mvn -pl flow-engine-example -am compile
mvn -pl flow-engine-example test
mvn -pl flow-engine-example package

# 运行示例应用（H2 数据库）
cd flow-engine-example && mvn spring-boot:run

# 运行示例应用（达梦数据库）
cd flow-engine-example && mvn spring-boot:run -Dspring.profiles.active=dm
```
