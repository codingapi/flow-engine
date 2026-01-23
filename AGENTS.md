# AGENTS.md

## Project Overview

Flow Engine - 企业流程引擎，基于Spring Boot 3.5.9和Java 17实现，支持流程设计、表单配置、节点管理和脚本执行。

## Build & Test Commands

### Maven Commands

```bash
# Build entire project
./mvnw clean install

# Run all tests
./mvnw test

# Run tests for specific module
./mvnw test -pl flow-engine-framework

# Run single test class
./mvnw test -Dtest=RandomUtilsTest

# Run single test method
./mvnw test -Dtest=RandomUtilsTest#generateCode

# Skip tests during build
./mvnw clean install -DskipTests
```

### Testing Framework

- **JUnit 5** (Jupiter) for testing
- Use `@Test` annotation for test methods
- Import static assertions: `import static org.junit.jupiter.api.Assertions.*;`

## Code Style Guidelines

### Package Structure

- Base package: `com.codingapi.flow`
- Organize by functionality: `workflow`, `node`, `form`, `script`, `user`, `session`, `utils`, `button`, `edge`

### Naming Conventions

**Classes**
- PascalCase: `Workflow`, `ApprovalNode`, `FlowForm`
- Builder classes: `WorkflowBuilder`, `FlowFormBuilder`
- Abstract base: `BaseNode`

**Interfaces**
- Prefix with `I`: `IFlowNode`, `IFlowOperator`, `IFlowEdge`, `IFlowButton`

**Methods**
- camelCase: `generateNodeId()`, `matchCreatedOperator()`, `build()`
- Getters/Setters: Use Lombok `@Getter` and `@Setter`
- Boolean getters: `isFlowManager()`, `match()`

**Fields**
- camelCase: `nodeName`, `formCode`, `createdTime`
- Constants: UPPER_SNAKE_CASE: `NODE_TYPE`, `SCRIPT_ANY`

**Enums**
- PascalCase: `FlowButtonType`, `PermissionType`
- Enum values: UPPER_SNAKE_CASE: `PASS`, `READ`, `WRITE`

### Imports

Organize by groups:
1. Standard library (`java.*`)
2. Third-party (`org.junit.jupiter.*`, `lombok.*`, `groovy.*`, `org.apache.*`)
3. Project imports (`com.codingapi.flow.*`)

### Lombok Usage

```java
// For simple DTOs
@Data
public class FormFieldMeta { }

// For getters/setters on individual fields
@Setter
@Getter
public class Workflow { }

// For constructors
@AllArgsConstructor
public class OperatorMatchScript { }
```

### Builder Pattern

Use fluent builder pattern with:
- Static `builder()` method returning singleton instance
- Methods return `this` for chaining
- `build()` method returns the constructed object

```java
public static WorkflowBuilder builder() {
    return instance;
}

public WorkflowBuilder title(String title) {
    workflow.setTitle(title);
    return this;
}
```

### Static Factory Methods

Provide static factory methods for common cases:

```java
public static OperatorMatchScript any() {
    return new OperatorMatchScript(SCRIPT_ANY);
}
```

### Singleton Pattern

Use static final instance for factories:

```java
private final static FlowNodeFactory instance = new FlowNodeFactory();

@Getter
private final static ScriptRuntimeContext instance = new ScriptRuntimeContext();
```

### Generics

Use generics with bounded wildcards for flexibility:

```java
public <T> T run(String script, Class<T> returnType, Object... args)
```

### Collections

- Initialize lazily in builders: `if (workflow.getNodes() == null) { workflow.setNodes(new ArrayList<>()); }`
- Use `List.of()` for immutable empty collections in default implementations

### Annotations

- Use Javadoc for classes and methods
- Use block comments for field descriptions
- Write comments in Chinese to match project style

```java
/**
 * 流程对象
 */
public class Workflow {
    /**
     * 流程id
     */
    private String id;
}
```

### Error Handling

- Wrap checked exceptions in RuntimeException
- Use descriptive exceptions from appropriate libraries
- Don't swallow exceptions without logging

**Framework Exception Usage**:
- Use static factory methods to create exceptions: `FlowValidationException.required("fieldName")`
- Exception code format: `category.subcategory.errorType` (e.g., `notFound.workflow.definition`)
- Never use `new FlowXXXException()` directly - always use factory methods
- Never use `ERROR_CODE_PREFIX` constants
- All exception messages must be in English

**Exception Examples**:
```java
// Correct - use factory method
throw FlowValidationException.required("workflow id");
throw FlowNotFoundException.workflow(workflowId);
throw FlowStateException.recordAlreadyDone();
throw FlowExecutionException.scriptExecutionError(method, e);

// Incorrect - direct construction
throw new FlowValidationException("validation.required", "Field is required");
throw new FlowConfigException(ERROR_CODE_PREFIX + "001", "Config error");
```

### Enums

Define enums with clear constants and comments:

```java
public enum FlowButtonType {
    // 通过
    PASS,
    // 拒绝
    REJECT,
}
```

### Type Safety

- Use interfaces as parameter types: `List<IFlowNode> nodes`
- Avoid raw types
- Use `@SuppressWarnings` only when necessary

### Testing

- Test class name: `ClassNameTest`
- Test method name: `methodName()` - use descriptive names
- Use `assertEquals`, `assertNotNull`, `assertTrue` from JUnit 5
- Import assertions statically: `import static org.junit.jupiter.api.Assertions.*;`

### Dependencies

- Spring Boot 3.5.9
- Lombok for boilerplate reduction
- Apache Commons (IO, Lang3, Crypto)
- Groovy for script execution
- Fastjson for JSON processing

## Module Structure

- `flow-engine-framework` - Core framework module
- `flow-engine-starter` - Starter module
- `flow-engine-starter-infra` - Infrastructure layer starter
- `flow-engine-example` - Example application
- `frontend` - Frontend applications (React/TypeScript)

## Frontend

For frontend development, see `frontend/AGENTS.md` in respective packages:
- `frontend/apps/app-pc/AGENTS.md`
- `frontend/packages/flow-design/AGENTS.md`

Frontend uses:
- pnpm for package management
- Rsbuild for building
- TypeScript for type safety
