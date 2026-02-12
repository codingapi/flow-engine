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
- **flow-engine-starter-infra** - Infrastructure persistence layer with JPA entities, convertors, and repository implementations
  - `entity/` - JPA entities (WorkflowEntity, FlowRecordEntity, DelayTaskEntity, etc.)
  - `convert/` - Entity-Domain convertors
  - `jpa/` - Spring Data JPA repositories
  - `repository/impl/` - Repository interface implementations
- **flow-engine-example** - Example application

### Frontend Structure

- **apps/app-pc** - PC client application
- **apps/app-mobile** - Mobile client application (in development)
- **packages/flow-design** - Flow designer component library using @flowgram.ai fixed-layout-editor
- **packages/flow-core** - Core API library with HTTP client and service interfaces
- **packages/flow-types** - TypeScript type definitions for the entire frontend
  - `pages/design-panel/types.ts` - TypeScript interfaces for Workflow, FlowNode, FlowForm
  - `FlowNode.blocks?: FlowNode[]` - Child nodes for hierarchical structure
  - `FlowNode.strategies` - Node strategies configuration
  - `FlowNode.actions` - Node actions configuration

Note: `app-mobile` is currently in development. `flow-pc` and `flow-mobile` packages are planned but not yet implemented.

### Data Structure: Blocks vs Edges

**Critical**: This project uses a hierarchical node structure via `blocks`, NOT edge-based connections.

**Backend (Java)**:
- `IFlowNode.blocks()` returns `List<IFlowNode>` - child nodes
- Block nodes: `ConditionNode`, `ParallelNode`, `InclusiveNode` contain branch nodes
- Branch nodes: `ConditionBranchNode`, `ParallelBranchNode`, `InclusiveBranchNode` are children
- `FlowNodeEdgeManager` traverses blocks recursively to find next nodes

**Frontend (TypeScript)**:
- `FlowNode.blocks?: FlowNode[]` - optional child nodes array
- Legacy `FlowEdge` interface exists but is deprecated
- @flowgram.ai fixed-layout-editor handles the visual representation

### Core Layered Architecture

The workflow engine is organized into 8 layers:

1. **Workflow Layer** (`com.codingapi.flow.workflow`)
   - `Workflow` - Top-level container with nodes, form definition
   - `WorkflowBuilder` - Builder pattern for workflow construction

2. **Node Layer** (`com.codingapi.flow.node`)
   - `IFlowNode` - Interface defining node lifecycle methods, includes `blocks()` method for child nodes
   - `BaseFlowNode` - Abstract base for all nodes, manages actions, strategies, and blocks
   - `BaseAuditNode` - Abstract base for audit nodes (ApprovalNode, HandleNode)
   - 15 node types: StartNode, EndNode, ApprovalNode, HandleNode, NotifyNode, ConditionBranchNode, ParallelBranchNode, RouterNode, InclusiveBranchNode, SubProcessNode, DelayNode, TriggerNode, ConditionNode, ParallelNode, InclusiveNode
   - **Block nodes** (containers with child blocks): ConditionNode, ParallelNode, InclusiveNode
   - **Branch nodes** (child nodes in blocks): ConditionBranchNode, ParallelBranchNode, InclusiveBranchNode

3. **Action Layer** (`com.codingapi.flow.action`, `com.codingapi.flow.action.actions`)
   - `IFlowAction` - Interface for node actions with `copy()` method
   - `BaseAction` - Abstract base with `triggerNode()` for recursive traversal
   - 8 action types (enum): PassAction, RejectAction, SaveAction, ReturnAction, TransferAction, AddAuditAction, DelegateAction, CustomAction

4. **Record Layer** (`com.codingapi.flow.record`)
   - `FlowRecord` - Execution record with states (TODO/DONE, RUNNING/FINISH/ERROR/DELETE)
   - `FlowTodoRecord` - Todo record for pending tasks
   - `FlowTodoMerge` - Todo merge record for aggregation
   - Tracks processId, nodeId, currentOperatorId, formData, parallel branch info

5. **Session Layer** (`com.codingapi.flow.session`)
   - `FlowSession` - Execution context (currentOperator, workflow, currentNode, currentAction, currentRecord, formData, advice)
   - `FlowAdvice` - Approval parameters (advice, signKey, backNode, transferOperators)

6. **Manager Layer** (`com.codingapi.flow.manager`)
   - `ActionManager` - Manages node actions, provides `getAction(Class)`, `verifySession()`
   - `OperatorManager` - Manages node operators
   - `NodeStrategyManager` - Manages node strategies, provides `loadOperators()`, `generateTitle()`, `verifySession()`, `getTimeoutTime()`, `isDone()`
   - `WorkflowStrategyManager` - Manages workflow strategies
   - `FlowNodeManager` - Node management functionality
   - `FlowNodeState` - Classifies nodes as block nodes or branch nodes for traversal
   - `FlowNodeEdgeManager` - Traverses hierarchical node structure via blocks to find next nodes

7. **Strategy Layer** (`com.codingapi.flow.strategy`)
   - `INodeStrategy` - Interface with `copy()`, `getId()`, `strategyType()`
   - **Node strategies** (16 types): MultiOperatorAuditStrategy, TimeoutStrategy, SameOperatorAuditStrategy, RecordMergeStrategy, ResubmitStrategy, AdviceStrategy, OperatorLoadStrategy, ErrorTriggerStrategy, NodeTitleStrategy, FormFieldPermissionStrategy, DelayStrategy, TriggerStrategy, RouterStrategy, SubProcessStrategy, RevokeStrategy
   - **Workflow strategies** (2 types): InterfereStrategy, UrgeStrategy

8. **Script Layer** (`com.codingapi.flow.script.runtime`)
   - `ScriptRuntimeContext` - Groovy script execution with thread-safe design and auto-cleanup
   - Script types: OperatorMatchScript, OperatorLoadScript, NodeTitleScript, ConditionScript, RouterNodeScript, SubProcessScript, TriggerScript, ErrorTriggerScript, RejectActionScript, CustomScript

### Supporting Architectures

- **Common Interfaces** (`com.codingapi.flow.common`)
   - `ICopyAbility` - Interface for copy capability (used by strategies and actions)
   - `IMapConvertor` - Interface for Map conversion (used by strategies and actions)
- **Repository Pattern** (`com.codingapi.flow.repository`) - Abstraction for data persistence, isolates framework from implementation. Includes: WorkflowRepository, FlowRecordRepository, WorkflowBackupRepository, ParallelBranchRepository, DelayTaskRepository, UrgeIntervalRepository, FlowTodoRecordRepository, FlowTodoMergeRepository. Implementations are in `flow-engine-starter-infra` under `com.codingapi.flow.infra.repository.impl`. Access via `RepositoryHolderContext` singleton. Pattern: Interface in framework, implementation in infra using JPA entities and convertors.
- **Gateway Pattern** (`com.codingapi.flow.gateway`) - Anti-corruption layer for external system integration (operators, users). Access via `GatewayContext` singleton.
- **Domain Objects** (`com.codingapi.flow.domain`) - DelayTask, DelayTaskManager, UrgeInterval
- **Event System** (`com.codingapi.flow.event`) - 5 event types: FlowRecordStartEvent, FlowRecordTodoEvent, FlowRecordDoneEvent, FlowRecordFinishEvent, FlowRecordUrgeEvent
- **Backup System** (`com.codingapi.flow.backup`) - WorkflowBackup for workflow versioning

### Node Lifecycle (Critical for Understanding Flow)

When a node is executed, methods are called in this order:
1. `verifySession(session)` - Delegates to ActionManager and StrategyManager
2. `continueTrigger(session)` - Returns true to continue, false to generate records
3. `generateCurrentRecords(session)` - Generate FlowRecords (uses StrategyManager.loadOperators() for audit nodes)
4. `fillNewRecord(session, record)` - Fill record data (uses StrategyManager for audit nodes)
5. `isDone(session)` - Check if complete (uses StrategyManager for multi-person approval)

### Flow Execution Lifecycle

**FlowCreateService** (starting a workflow):
1. Validate request → Get workflow → Verify workflow → Create backup → Validate creator → Build form data → Create start session → Verify session → Generate records → Save records → Push events

**FlowActionService** (executing an action):
1. Validate request → Validate operator → Get record → Validate state → Load workflow → Get current node → Get action → Build form data → Create session → Verify session → Execute action

**FlowDetailService** (querying workflow details):
1. Fetch workflow details by processId → Include workflow definition, current records, form data

**PassAction.run()** (typical action):
1. Check if node done (StrategyManager) → Update current record → Generate subsequent records → Trigger next nodes → Save records → Push events

### Parallel Branch Execution

When encountering `ParallelBranchNode`:
1. `filterBranches()` finds the convergence end node
2. Record parallel info (parallelBranchNodeId, parallelBranchTotal, parallelId) in FlowRecord
3. Execute all branches simultaneously
4. At convergence, `isWaitParallelRecord()` checks if all branches arrived
5. Once all arrive, clear parallel info and continue

### Delay and Trigger Nodes

**DelayNode**: Uses `DelayStrategy` with time units (SECOND/MINUTE/HOUR/DAY). Creates `DelayTask` which is managed by `DelayTaskManager` using `java.util.Timer` for scheduled execution. On trigger, `FlowDelayTriggerService` resumes the flow.

**TriggerNode**: Uses `TriggerStrategy` with `TriggerScript`. Executes trigger script via `ScriptRuntimeContext` when node is reached.

### Workflow-Level Features

**Urge (催办)**: `UrgeStrategy` with configurable interval (default 60 seconds). Uses `UrgeInterval` domain object to track timing. Triggers `FlowRecordUrgeEvent` when interval expires. Used for automatic reminders of pending tasks.

**Interfere (干预)**: `InterfereStrategy` controls whether admin intervention is allowed in the workflow. Enable/disable at workflow level.

**Revoke (撤回)**: `RevokeStrategy` at node level supports two types:
- `REVOKE_NEXT`: Revoke subordinates, delete subsequent pending records, return to current node
- `REVOKE_CURRENT`: Revoke to current node, restore current node to pending state

Key difference from Return (退回): Return is executed by current approver to go back to previous completed nodes; Revoke is executed by approved personnel to revoke subordinates or restore current node.

### Key Design Patterns

- **Builder**: `WorkflowBuilder`, `BaseNodeBuilder` with singleton `builder()` method
- **Factory**: `NodeFactory.getInstance()` (reflection + static `formMap()`), `NodeStrategyFactory`, `FlowActionFactory`
- **Strategy**: `INodeStrategy` with `StrategyManager` - strategy-driven configuration
- **Template Method**: `BaseFlowNode` (node lifecycle), `BaseAction` (action execution), `BaseStrategy` (strategy template)
- **Singleton**: `NodeFactory`, `ScriptRuntimeContext`, `RepositoryHolderContext`, `GatewayContext` (static final instance)
- **Chain of Responsibility**: `triggerNode()` recursive traversal, `StrategyManager` strategy iteration
- **Composite**: Nodes contain multiple strategies and actions
- **Copy Pattern**: `INodeStrategy.copy()`, `IFlowAction.copy()`, `BaseFlowNode.setActions()`, `BaseFlowNode.setStrategies()`

### Strategy-Driven Node Configuration (Critical Architecture)

All node configurations are implemented through strategies:
- **Operator loading**: `OperatorLoadStrategy` with Groovy script
- **Node title**: `NodeTitleStrategy` with Groovy script
- **Timeout**: `TimeoutStrategy` with timeout value and type
- **Permissions**: `FormFieldPermissionStrategy` with field permissions
- **Error handling**: `ErrorTriggerStrategy` with Groovy script
- **Multi-person approval**: `MultiOperatorAuditStrategy` with type (SEQUENCE/MERGE/ANY/RANDOM_ONE)

`StrategyManager` provides unified access: `loadOperators()`, `generateTitle()`, `getTimeoutTime()`, `verifySession()`

### Multi-Person Approval Modes

`MultiOperatorAuditStrategy.Type` enum:
- **SEQUENCE**: Sequential, hides subsequent records
- **MERGE**: Concurrent, requires percentage completion
- **ANY**: Any one person, completes on first approval
- **RANDOM_ONE**: Randomly selects one person

Implemented in `BaseAuditNode.isDone()`.

### ScriptRuntimeContext Auto-Cleanup

Thread-safe Groovy script execution with dual auto-cleanup:
- **Threshold-based**: Triggers when `SCRIPT_LOCKS.size() > maxLockCacheSize` (default 1000)
- **Scheduled**: Periodic cleanup every `CLEANUP_INTERVAL_SECONDS` (default 300s)
- **Configuration**: JVM property `-Dflow.script.cleanup.interval`, or `setMaxLockCacheSize(int)`
- **Lifecycle**: Auto-starts on singleton init, registers shutdown hook, supports runtime enable/disable

Thread safety: Each execution creates independent GroovyClassLoader/GroovyShell. Fine-grained synchronization using script hashCode as lock key - same script serializes, different scripts run concurrently.

### Framework Exception Hierarchy

All framework exceptions extend `FlowException` (RuntimeException). Exception codes follow the format `category.subcategory.errorType`.

**Exception types**:
- `FlowValidationException` - Parameter validation (e.g., `validation.field.required`)
- `FlowNotFoundException` - Resource not found (e.g., `notFound.workflow.definition`)
- `FlowStateException` - Invalid state (e.g., `state.record.alreadyDone`)
- `FlowPermissionException` - Permission issues (e.g., `permission.field.readOnly`)
- `FlowConfigException` - Configuration errors (e.g., `config.node.strategies.required`)
- `FlowExecutionException` - Execution errors (e.g., `execution.script.error`)

**Critical exception rules**:
- MUST use static factory methods: `FlowValidationException.required("fieldName")`
- NEVER use `new FlowXXXException()` directly
- NEVER use `ERROR_CODE_PREFIX` constants
- All exception messages MUST be in English

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
- **Custom Strategies**: Extend `BaseStrategy`, implement `INodeStrategy` for node strategies or `IWorkflowStrategy` for workflow strategies
- **Custom Scripts**: Use `ScriptRuntimeContext` for Groovy execution
- **Event Listeners**: Listen to `FlowRecordStartEvent`, `FlowRecordTodoEvent`, `FlowRecordDoneEvent`, `FlowRecordFinishEvent`, `FlowRecordUrgeEvent`
- **Repository Implementations**: Implement repository interfaces in infra layer for persistence
- **Gateway Implementations**: Implement gateway interfaces for system integration

## Documentation References

- **Design.md** (root) - Comprehensive architecture documentation with class design, lifecycle diagrams, design patterns, and key implementation details
- **frontend/apps/app-pc/AGENTS.md** - Frontend app development guidelines
- **Rsbuild**: https://rsbuild.rs/llms.txt
- **Rspack**: https://rspack.rs/llms.txt
