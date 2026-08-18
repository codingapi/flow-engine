---
name: springboot/data-jpa
module: springboot
description: Spring Data JPA 能力，提供 ORM、Repository 抽象和分页查询
status: 已实现
scope: 后端
source: 框架:Spring Boot
import: "com.codingapi.springboot:springboot-starter-data-fast"
framework_version: "由 codingapi.framework.version 17.3.0 管理"
---

## 解决什么问题

提供 Spring Data JPA 的 ORM 能力，解决以下问题：

- **ORM 映射**：通过 JPA 注解将 Java 实体映射到数据库表
- **Repository 抽象**：通过 `FastRepository` 接口（基于 codingapi `springboot-starter-data-fast`）自动生成 CRUD 实现
- **分页查询**：内置 `Pageable` / `Page` 支持分页
- **多数据库支持**：同一代码支持 MySQL、PostgreSQL、H2 等多种数据库，示例/生产环境还支持达梦（DM）数据库

## 如何使用

### 核心注解

| 注解 | 用途 |
|------|------|
| `@Entity` / `@Table` | 实体类和表映射 |
| `@Id` / `@GeneratedValue` | 主键定义 |
| `@Column` | 列映射 |
| `@Repository` | Repository 接口声明 |

### 在 Flow Engine 中的使用

`flow-engine-starter-infra` 模块使用 Spring Data JPA：
- 定义 JPA Entity（`WorkflowEntity`、`FlowRecordEntity` 等）
- 实现基于 `FastRepository` 的 JPA Repository 接口
- 通过 `Convertor` 在 Entity 和领域模型间转换
- 支持 MySQL、PostgreSQL、H2 三种数据库，示例/生产环境还支持达梦（DM）数据库

### 数据库驱动依赖

```xml
<!-- MySQL -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>
<!-- PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
<!-- H2 (测试/内嵌) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
</dependency>
<!-- 达梦 (示例/生产) -->
<dependency>
    <groupId>com.dameng</groupId>
    <artifactId>DmJdbcDriver11</artifactId>
</dependency>
```

## 使用实例

```java
// Entity 定义
@Data
@Entity
@Table(name = "t_flow_workflow")
public class WorkflowEntity {
    @Id
    private String id;

    @Column(unique = true)
    private String code;

    @Lob
    private String title;
}

// JPA Repository（基于 codingapi springboot-starter-data-fast）
public interface WorkflowEntityRepository extends FastRepository<WorkflowEntity, String> {
    WorkflowEntity getWorkflowEntityById(String id);

    WorkflowEntity getWorkflowEntityByCode(String code);
}

// Repository 实现（构造器注入，由 infra AutoConfiguration 的 @Bean 方法注册）
@AllArgsConstructor
public class WorkflowRepositoryImpl implements WorkflowRepository {
    private final WorkflowEntityRepository workflowEntityRepository;

    @Override
    public Workflow getByCode(String code) {
        WorkflowEntity entity = workflowEntityRepository.getWorkflowEntityByCode(code);
        return WorkflowConvertor.convert(entity);
    }
}
```