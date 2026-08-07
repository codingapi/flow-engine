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
- **Builder 模式**：Lombok 提供 `@Builder` 自动生成建造者模式代码（本项目未使用，节点类为手写 Builder）
- **日志**：通过 `@Slf4j` / `@Log4j2` 自动注入日志对象
- **异常处理**：通过 `@SneakyThrows` 简化受检异常的处理

## 如何使用

### Flow Engine 中常用的 Lombok 注解

| 注解 | 用途 |
|------|------|
| `@Getter` / `@Setter` | 自动生成 getter/setter |
| `@AllArgsConstructor` | 构造器注入依赖（服务类、Controller、Repository 实现） |
| `@NoArgsConstructor` | 无参构造器 |
| `@SneakyThrows` | 自动处理受检异常（用于工厂方法中的反射调用） |

> 注：`@Builder`、`@ToString` 在项目中未使用；节点类的链式构建是手写 Builder（继承 `BaseNodeBuilder`）实现。

### 典型使用场景

- 节点类（`ApprovalNode`、`ConditionNode` 等）通过手写 Builder（`extends BaseNodeBuilder`）提供链式构建，`defaultNode()` 静态工厂方法手工 set 属性
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

// 手写 Builder（继承 BaseNodeBuilder）实现链式构建
public class ApprovalNode extends BaseAuditNode implements IDisplayNode {

    public static ApprovalNode defaultNode() {
        ApprovalNode approvalNode = new ApprovalNode();
        approvalNode.setId(FlowIDGeneratorGatewayContext.getInstance().generateNodeId());
        approvalNode.setName(DEFAULT_NAME);
        approvalNode.setView(DEFAULT_VIEW);
        approvalNode.setCode(FlowIDGeneratorGatewayContext.getInstance().generateViewCode());
        approvalNode.setActions(defaultActions());
        approvalNode.setStrategies(defaultStrategies());
        return approvalNode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, ApprovalNode> {
        public Builder() {
            super(ApprovalNode.defaultNode());
        }
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