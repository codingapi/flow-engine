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

### Core Layered Architecture

The workflow engine is organized into 8 layers:

1. **Workflow Layer** (`com.codingapi.flow.workflow`)
   - `Workflow` - Top-level container with nodes, edges, form definition
   - `WorkflowBuilder` - Builder pattern for workflow construction

2. **Node Layer** (`com.codingapi.flow.node`)
   - `IFlowNode` - Interface defining node lifecycle methods
   - `BaseFlowNode` - Abstract base for all nodes
   - `BaseAuditNode` - Abstract base for audit nodes (ApprovalNode, HandleNode)
   - 11 node types: StartNode, EndNode, ApprovalNode, HandleNode, NotifyNode, ConditionBranchNode, ParallelBranchNode, RouterBranchNode, InclusiveBranchNode, DelayNode, TriggerNode, SubProcessNode

3. **Action Layer** (`com.codingapi.flow.action`)
   - `IFlowAction` - Interface for node actions
   - `BaseAction` - Abstract base with `triggerNode()` for recursive node traversal
   - 8 action types: PassAction, RejectAction, SaveAction, ReturnAction, TransferAction, AddAuditAction, DelegateAction, CustomAction

4. **Record Layer** (`com.codingapi.flow.record`)
   - `FlowRecord` - Execution record with states (TODO/DONE, RUNNING/FINISH/ERROR/DELETE)
   - Tracks processId, nodeId, currentOperatorId, formData, parallel branch info

5. **Session Layer** (`com.codingapi.flow.session`)
   - `FlowSession` - Execution context with currentOperator, workflow, currentNode, currentAction, currentRecord, formData, advice
   - `FlowAdvice` - Encapsulates approval parameters (advice, signKey, backNode, transferOperators)

6. **Manager Layer** (`com.codingapi.flow.node.manager`)
   - `ActionManager` - Manages node actions
   - `OperatorManager` - Manages node operators
   - `FieldPermissionManager` - Manages form field permissions
   - `StrategyManager` - Manages node strategies

7. **Strategy Layer** (`com.codingapi.flow.strategy`)
   - `INodeStrategy` - Interface for node strategies
   - MultiOperatorAuditStrategy, TimeoutStrategy, SameOperatorAuditStrategy, RecordMergeStrategy, ResubmitStrategy, AdviceStrategy

8. **Script Layer** (`com.codingapi.flow.script.runtime`)
   - `ScriptRuntimeContext` - Groovy script execution environment
   - OperatorMatchScript, OperatorLoadScript, NodeTitleScript, ConditionScript, ErrorTriggerScript

### Node Lifecycle (Critical for Understanding Flow)

When a node is executed, methods are called in this order:
1. `verifySession(session)` - Validate session parameters
2. `continueTrigger(session)` - Returns true to continue to next node, false to generate records
3. `generateCurrentRecords(session)` - Generate FlowRecords for current node
4. `fillNewRecord(session, record)` - Fill new record data
5. `isDone(session)` - Check if node is complete (multi-person approval nodes need progress check)

### Flow Execution Lifecycle

**FlowCreateService** (starting a workflow):
1. Validate request → Get workflow definition → Verify workflow → Create/get backup → Validate creator → Build form data → Create start session → Verify session → Generate records → Save records → Push events

**FlowActionService** (executing an action):
1. Validate request → Validate operator → Get record → Validate record state → Load workflow → Get current node → Get action → Build form data → Create session → Verify session → Execute action

**PassAction.run()** (typical action execution):
1. Check if node is done → Update current record → Generate subsequent records → Trigger next nodes → Save records → Push events

### Parallel Branch Execution

When encountering `ParallelBranchNode`:
1. `filterBranches()` analyzes and finds the convergence end node
2. Record parallel info (parallelBranchNodeId, parallelBranchTotal, parallelId) in FlowRecord
3. Execute all branches simultaneously (generate records for each)
4. At convergence node, `isWaitParallelRecord()` checks if all branches have arrived
5. Once all arrive, clear parallel info and continue

### Key Design Patterns

- **Builder Pattern**: `WorkflowBuilder`, `BaseNodeBuilder`, `AuditNodeBuilder` with singleton `builder()` method
- **Factory Pattern**: `NodeFactory.getInstance()`, `FlowActionFactory`
- **Strategy Pattern**: `INodeStrategy` for multi-operator approval, timeout, etc.
- **Template Method**: `BaseFlowNode` defines node lifecycle, `BaseAction` defines action execution
- **Singleton**: `NodeFactory`, `ScriptRuntimeContext`, `RepositoryContext` use static final instance
- **Chain of Responsibility**: `triggerNode()` recursively traverses subsequent nodes

## Code Conventions

- Base package: `com.codingapi.flow`
- Interfaces prefixed with `I`: `IFlowNode`, `IFlowOperator`
- Builders use singleton pattern with static `builder()` method
- All comments and documentation in Chinese
- Lombok annotations for boilerplate (`@Data`, `@Getter`, `@Setter`)
- JUnit 5 with static assertions: `import static org.junit.jupiter.api.Assertions.*;`

## Extension Points

- **Custom Nodes**: Extend `BaseFlowNode` or `BaseAuditNode`, implement `IFlowNode`
- **Custom Actions**: Extend `BaseAction`, implement `IFlowAction`
- **Custom Strategies**: Implement `INodeStrategy`
- **Custom Scripts**: Use `ScriptRuntimeContext` for Groovy execution
- **Event Listeners**: Listen to `FlowRecordStartEvent`, `FlowRecordTodoEvent`, `FlowRecordDoneEvent`, `FlowRecordFinishEvent`

## Documentation References

- **flow-engine-framework/src/test/Design.md** - Comprehensive architecture documentation with class design, lifecycle diagrams, and design patterns
- **AGENTS.md** - Detailed coding guidelines and patterns
- **frontend/packages/flow-design/AGENTS.md** - Frontend library development
- **frontend/apps/app-pc/AGENTS.md** - Frontend app development
- **Rsbuild**: https://rsbuild.rs/llms.txt
- **Rspack**: https://rspack.rs/llms.txt
