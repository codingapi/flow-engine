# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Flow Engine is an enterprise workflow engine built with Java 17 and Spring Boot 3.5.9, with a React/TypeScript frontend. It supports workflow design, form configuration, node management, and script execution (Groovy).

## Common Commands

### Backend (Maven)

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

### Frontend (pnpm)

```bash
cd frontend
pnpm install

# Build the design library
pnpm run build:flow-engine

# Run PC app in dev mode
pnpm run dev:app-pc
```

## Architecture

### Backend Modules

- **flow-engine-framework** - Core framework with workflow engine, node types, form system, script execution
- **flow-engine-starter** - Spring Boot starter for web applications
- **flow-engine-starter-infra** - Infrastructure persistence layer
- **flow-engine-example** - Example application

### Frontend Structure

- **apps/app-pc** - PC client application
- **packages/flow-design** - Flow designer component
- **packages/flow-pc** - PC display components
- **packages/flow-mobile** - Mobile display components

### Core Workflow Concepts

**Node Types** (11 types defined in `FlowNodeFactory`):
- StartNode, EndNode - Process boundaries
- ApprovalNode, HandleNode - Task processing
- NotifyNode - Notifications
- ConditionBranchNode, ParallelBranchNode, RouterBranchNode, InclusiveBranchNode - Control flow
- SubProcessNode - Nested workflows
- DelayNode, TriggerNode - Timing and events

**Key Interfaces**:
- `IFlowNode` - All workflow nodes implement this
- `IFlowOperator` - User/operator abstraction
- `IFlowEdge` - Connections between nodes
- `IFlowButton` - Actions available on nodes

**Builder Pattern** - Used extensively for creating workflows and forms:
```java
Workflow workflow = WorkflowBuilder.builder()
    .title("Leave Request")
    .node(startNode)
    .node(approvalNode)
    .build();
```

### Form System

Forms have a main form with optional sub-forms. Fields support:
- Types: string, int, and others
- Permissions: READ, WRITE, NONE at field level
- Validation rules via metadata

### Script Execution

Groovy scripts run in `ScriptRuntimeContext` with:
- Variable injection for workflow data
- Operator matching scripts for routing decisions
- Custom function support

## Code Conventions

- Base package: `com.codingapi.flow`
- Interfaces prefixed with `I`: `IFlowNode`, `IFlowOperator`
- Builders use singleton pattern with static `builder()` method
- All comments and documentation in Chinese
- Lombok annotations for boilerplate (`@Data`, `@Getter`, `@Setter`)
- JUnit 5 with static assertions: `import static org.junit.jupiter.api.Assertions.*;`

## Documentation References

- **AGENTS.md** - Detailed coding guidelines and patterns
- **frontend/packages/flow-design/AGENTS.md** - Frontend library development
- **frontend/apps/app-pc/AGENTS.md** - Frontend app development
- **Rsbuild**: https://rsbuild.rs/llms.txt
- **Rspack**: https://rspack.rs/llms.txt
