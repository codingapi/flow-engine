---
name: lombok/lombok
module: lombok
description: Lombok 代码简化工具，通过注解自动生成 getter/setter/builder 等样板代码
status: 已实现
scope: 后端
source: 框架:Lombok
import: "org.projectlombok:lombok"
framework_version: "由 Spring Boot Parent 管理"
---

## 解决什么问题

消除 Java 的样板代码，解决以下问题：

- **Getter/Setter**：通过 `@Getter` / `@Setter` 自动生成，减少数百行重复代码
- **构造器**：通过 `@AllArgsConstructor` / `@NoArgsConstructor` / `@RequiredArgsConstructor` 自动生成构造器
- **Builder 模式**：通过 `@Builder` 自动生成建造者模式代码
- **日志**：通过 `@Slf4j` / `@Log4j2` 自动注入日志对象
- **异常处理**：通过 `@SneakyThrows` 简化受检异常的处理

## 如何使用

### Flow Engine 中常用的 Lombok 注解

| 注解 | 用途 |
|------|------|
| `@Getter` / `@Setter` | 自动生成 getter/setter |
| `@AllArgsConstructor` | 全参构造器 |
| `@NoArgsConstructor` | 无参构造器 |
| `@Builder` | 建造者模式（用于 Node、Action 等） |
| `@SneakyThrows` | 自动处理受检异常（用于工厂方法中的反射调用） |
| `@ToString` | toString 方法 |

### 典型使用场景

- 节点类（`ApprovalNode`、`ConditionNode` 等）使用 `@Builder` 提供链式构建
- 工厂类（`NodeFactory`、`FlowActionFactory` 等）使用 `@SneakyThrows` 简化反射
- 服务类使用 `@Getter` 暴露字段

## 使用实例

```java
// 节点类使用 Lombok
@Getter
@Setter
@AllArgsConstructor
public class WorkflowVersion {
    private long id;
    private String versionName;
    private boolean current;

    public void enableVersion() {
        this.current = true;
    }
}

// 建造者模式
@Builder
public class ApprovalNode extends BaseAuditNode {
    public static ApprovalNode defaultNode() {
        return ApprovalNode.builder()
            .id(UUID.randomUUID().toString())
            .name("审批节点")
            .build();
    }
}

// SneakyThrows 简化反射
@SneakyThrows
public IFlowNode createNode(NodeType type) {
    Class<? extends IFlowNode> clazz = nodesClasses.get(type.name());
    Method defaultNode = clazz.getMethod("defaultNode");
    return (IFlowNode) defaultNode.invoke(null);
}
```
