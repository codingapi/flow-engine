# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Flow Engine is an enterprise-grade workflow engine built with Java 17 and Spring Boot 3.5.9. It provides visual process design, dynamic form configuration, multi-node type transitions, and script extensions. The project uses a front-end/back-end separated architecture, supporting both PC and mobile clients.

## Common Commands

### Backend (Java/Maven)

```bash
# Build the entire project
./mvnw clean install

# Run all tests
./mvnw test

# Run tests for a specific module
./mvnw test -pl flow-engine-framework

# Run a specific test class
./mvnw test -Dtest=ScriptRuntimeContextTest

# Run the example application
cd flow-engine-example && mvn spring-boot:run
```

### Frontend (React/pnpm)

```bash
# Install dependencies (use pnpm, not npm)
cd frontend && pnpm install

# Build all packages
pnpm run build

# Build specific app
pnpm run build:app-pc

# Development mode
pnpm run dev:app-pc    # PC application
pnpm run dev:app-mobile # Mobile application
```

## Architecture

### Backend Layers (8-layer architecture)

1. **Workflow Layer** - Process definition
2. **Node Layer** - 15 node types (StartNode, EndNode, ApprovalNode, HandleNode, NotifyNode, RouterNode, SubProcessNode, DelayNode, TriggerNode, ConditionNode, ParallelNode, InclusiveNode, and their branch variants)
3. **Action Layer** - 8 action types (Pass, Reject, Save, AddAudit, Delegate, Return, Transfer, Custom)
4. **Record Layer** - Process instance records
5. **Session Layer** - Session management
6. **Manager Layer** - Business managers (NodeStrategyManager, OperatorManager, ActionManager, etc.)
7. **Strategy Layer** - Policy-driven configuration
8. **Script Layer** - Groovy script runtime

### Design Patterns Used

- **Builder Pattern** - WorkflowBuilder, NodeBuilder, FormMetaBuilder
- **Factory Pattern** - FlowActionFactory
- **Strategy Pattern** - NodeStrategy, WorkflowStrategy
- **Template Method Pattern** - BaseAction, BaseNodeBuilder
- **Chain of Responsibility Pattern** - Action execution chain
- **Composite Pattern** - Node hierarchy with blocks

### Module Structure

| Module | Description |
|--------|-------------|
| `flow-engine-framework` | Core workflow engine framework |
| `flow-engine-starter` | Spring Boot starter |
| `flow-engine-starter-api` | API layer |
| `flow-engine-starter-infra` | Infrastructure layer |
| `flow-engine-starter-query` | Query layer |
| `flow-engine-example` | Example application |
| `frontend/apps/app-pc` | PC workflow designer and admin |
| `frontend/apps/app-mobile` | Mobile todo/done pages |

### Technology Stack

- **Backend**: Java 17, Spring Boot 3.5.9, Groovy
- **Frontend**: React 18, TypeScript, pnpm, Rsbuild, Rslib, Antd

## Key Packages

- `com.codingapi.flow.node` - Node implementations (15 types)
- `com.codingapi.flow.action` - Action implementations (8 types)
- `com.codingapi.flow.strategy` - Strategy interfaces and implementations
- `com.codingapi.flow.repository` - Data access layer
- `com.codingapi.flow.service` - Business service layer
- `com.codingapi.flow.script` - Groovy script runtime

## Development Notes

- Use pnpm for frontend package management (per user configuration)
- All node configurations use policy-driven approach, supporting dynamic extensions
- Scripts use fine-grained synchronization locks for thread safety
- Process definitions use hierarchical node structure via `blocks` property (not separate edge relationships)
