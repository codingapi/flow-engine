# AGENTS.md

## Project Overview

Flow Engine - 企业流程引擎，基于 Spring Boot 3.5.9 和 Java 17 实现，支持流程设计、表单配置、节点管理和脚本执行。

## Build & Test Commands

### Backend (Java)

#### Maven Commands

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

# Build with specific profile
./mvnw clean install -P dev    # Development profile (default)
./mvnw clean install -P travis # Travis CI profile with coverage
./mvnw clean deploy -P ossrh   # Deploy to Maven Central
```

#### Testing Framework

- **JUnit 5** (Jupiter) for testing
- Use `@Test` annotation for test methods
- Import static assertions: `import static org.junit.jupiter.api.Assertions.*;`

### Frontend (TypeScript)

#### pnpm Commands

```bash
# Install dependencies (run from frontend directory)
cd frontend && pnpm install

# Run all tests for flow-design package
cd frontend && pnpm -F @flow-engine/flow-design test

# Run specific tests
cd frontend && pnpm -F @flow-engine/flow-design test:format    # format tests
cd frontend && pnpm -F @flow-engine/flow-design test:toNode    # node tests
cd frontend && pnpm -F @flow-engine/flow-design test:toEdge    # edge tests
cd frontend && pnpm -F @flow-engine/flow-design test:toRender  # render tests

# Build packages
cd frontend && pnpm build:flow-core     # Build flow-core
cd frontend && pnpm build:flow-types   # Build flow-types
cd frontend && pnpm build:flow-engine  # Build flow-design
cd frontend && pnpm build              # Build all packages

# Run development server
cd frontend && pnpm dev:app-pc         # PC app
cd frontend && pnpm dev:app-mobile     # Mobile app
cd frontend && pnpm watch:flow-design  # Watch flow-design changes
```

#### Testing Framework

- **Rstest** for testing TypeScript code
- Write tests in `*.test.ts` files
- Use `describe.sequential` for test suites
- Use `test` for test cases

## Code Style Guidelines

### Java (Backend)

#### Package Structure

- Base package: `com.codingapi.flow`
- Organize by functionality: `workflow`, `node`, `form`, `script`, `user`, `session`, `utils`, `button`, `edge`

#### Naming Conventions

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

#### Imports

Organize by groups:
1. Standard library (`java.*`)
2. Third-party (`org.junit.jupiter.*`, `lombok.*`, `groovy.*`, `org.apache.*`)
3. Project imports (`com.codingapi.flow.*`)

#### Lombok Usage

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

#### Error Handling

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

#### Testing Guidelines

- Test class name: `ClassNameTest`
- Test method name: `methodName()` - use descriptive names
- Use `assertEquals`, `assertNotNull`, `assertTrue` from JUnit 5
- Import assertions statically: `import static org.junit.jupiter.api.Assertions.*;`

### TypeScript (Frontend)

#### TypeScript

- **Target:** ES2022 with React JSX
- **Strict mode:** Enabled
- **Import style:** ES modules with `.js` extension

#### Naming Conventions

- **Classes:** PascalCase (e.g., `WorkflowEdgeConvertor`)
- **Interfaces:** PascalCase with descriptive names
- **Methods:** camelCase (e.g., `loadNextEdges`)
- **Private fields:** `private readonly` preferred
- **Constants:** UPPER_SNAKE_CASE for true constants

#### Import Order

1. External libraries (`@rstest/core`, `lodash-es`)
2. Workspace packages (`@flow-engine/*`)
3. Internal aliases (`@/*`)
4. Relative imports (`./utils`)

#### Error Handling

- Use type-safe error handling
- Validate inputs early with guard clauses
- Log debug info via `console.log` in tests

#### Architecture Patterns

- **Convertor Pattern:** Separate conversion logic (e.g., `WorkflowEdgeConvertor`)
- **Builder Utilities:** Use test utilities like `WorkflowUtils` for test setup
- **Filter Nodes:** Define node type filters as readonly arrays

#### Testing with rstest

```typescript
import {describe, expect, test} from "@rstest/core";

describe.sequential('Suite', () => {
    test('case', () => {
        expect(actual).toBe(expected);
    });
});
```

## Module Structure

### Backend

- `flow-engine-framework` - Core framework module (流程引擎核心逻辑)
- `flow-engine-starter` - Starter module (启动模块)
- `flow-engine-starter-infra` - Infrastructure layer starter (基础设施层)
- `flow-engine-starter-api` - API layer starter (API层)
- `flow-engine-starter-query` - Query layer starter (查询层)
- `flow-engine-example` - Example application (示例应用)

### Frontend

- `frontend/apps/app-pc` - PC 端应用
- `frontend/apps/app-mobile` - 移动端应用
- `frontend/packages/flow-core` - 核心功能库
- `frontend/packages/flow-types` - 类型定义库
- `frontend/packages/flow-design` - 流程设计器库

## Dependencies

### Backend

- **Spring Boot 3.5.9** - Framework
- **Lombok** - Boilerplate reduction
- **Apache Commons** - IO, Lang3, Crypto utilities
- **Groovy** - Script execution
- **FastJSON** - JSON processing
- **JJWT** - JWT token handling

### Frontend

- **React 18+** - UI framework
- **Ant Design** - UI components
- **Redux Toolkit** - State management
- **Rsbuild** - Build tool
- **Rstest** - Testing framework
- **nanoid** - Unique ID generation
- **styled-components** - CSS-in-JS

## Documentation

- **Design.md** - Detailed design document
- **PRD.md** - Product requirements document
- **README.md** - Project overview and getting started
- **TODO.md** - Planned features and improvements
- **CLAUDE.md** - Claude AI integration guidelines

For frontend development, see package-specific AGENTS.md files:
- `frontend/apps/app-pc/AGENTS.md`
- `frontend/apps/app-mobile/AGENTS.md`
- `frontend/packages/flow-design/AGENTS.md`
- `frontend/packages/flow-core/AGENTS.md`
- `frontend/packages/flow-types/AGENTS.md`
