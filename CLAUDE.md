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
   - `IFlowNode` - Interface defining node lifecycle methods (including `strategyManager()`)
   - `BaseFlowNode` - Abstract base for all nodes, manages both `actions` and `strategies`
   - `BaseAuditNode` - Abstract base for audit nodes (ApprovalNode, HandleNode), simplified to use strategies for all configurations
   - 11 node types: StartNode, EndNode, ApprovalNode, HandleNode, NotifyNode, ConditionBranchNode, ParallelBranchNode, RouterBranchNode, InclusiveBranchNode, DelayNode, TriggerNode, SubProcessNode

3. **Action Layer** (`com.codingapi.flow.action`, `com.codingapi.flow.action.actions`)
   - `IFlowAction` - Interface for node actions (including `copy()` method)
   - `BaseAction` - Abstract base with `triggerNode()` for recursive node traversal
   - 9 action types: DefaultAction, PassAction, RejectAction, SaveAction, ReturnAction, TransferAction, AddAuditAction, DelegateAction, CustomAction

4. **Record Layer** (`com.codingapi.flow.record`)
   - `FlowRecord` - Execution record with states (TODO/DONE, RUNNING/FINISH/ERROR/DELETE)
   - Tracks processId, nodeId, currentOperatorId, formData, parallel branch info

5. **Session Layer** (`com.codingapi.flow.session`)
   - `FlowSession` - Execution context with currentOperator, workflow, currentNode, currentAction, currentRecord, formData, advice
   - `FlowAdvice` - Encapsulates approval parameters (advice, signKey, backNode, transferOperators)

6. **Manager Layer** (`com.codingapi.flow.node.manager`)
   - `ActionManager` - Manages node actions (including `getAction(Class)`, `verifySession()`)
   - `OperatorManager` - Manages node operators
   - `StrategyManager` - Manages node strategies (including `loadOperators()`, `generateTitle()`, `verifySession()`)

7. **Strategy Layer** (`com.codingapi.flow.strategy`)
   - `INodeStrategy` - Interface for node strategies (including `copy()`, `getId()`)
   - 10 strategy types: MultiOperatorAuditStrategy, TimeoutStrategy, SameOperatorAuditStrategy, RecordMergeStrategy, ResubmitStrategy, AdviceStrategy, OperatorLoadStrategy, ErrorTriggerStrategy, NodeTitleStrategy, FormFieldPermissionStrategy

8. **Script Layer** (`com.codingapi.flow.script.runtime`)
   - `ScriptRuntimeContext` - Groovy script execution environment
   - OperatorMatchScript, OperatorLoadScript, NodeTitleScript, ConditionScript, ErrorTriggerScript, RejectActionScript

### Node Lifecycle (Critical for Understanding Flow)

When a node is executed, methods are called in this order:
1. `verifySession(session)` - Delegates to ActionManager and StrategyManager for validation
2. `continueTrigger(session)` - Returns true to continue to next node, false to generate records
3. `generateCurrentRecords(session)` - Generate FlowRecords for current node (uses StrategyManager.loadOperators() for audit nodes)
4. `fillNewRecord(session, record)` - Fill new record data (uses StrategyManager for audit nodes)
5. `isDone(session)` - Check if node is complete (uses StrategyManager for multi-person approval progress)

### Flow Execution Lifecycle

**FlowCreateService** (starting a workflow):
1. Validate request → Get workflow definition → Verify workflow → Create/get backup → Validate creator → Build form data → Create start session → Verify session → Generate records → Save records → Push events

**FlowActionService** (executing an action):
1. Validate request → Validate operator → Get record → Validate record state → Load workflow → Get current node → Get action → Build form data → Create session → Verify session → Execute action

**PassAction.run()** (typical action execution):
1. Check if node is done (via StrategyManager) → Update current record → Generate subsequent records → Trigger next nodes → Save records → Push events

### Parallel Branch Execution

When encountering `ParallelBranchNode`:
1. `filterBranches()` analyzes and finds the convergence end node
2. Record parallel info (parallelBranchNodeId, parallelBranchTotal, parallelId) in FlowRecord
3. Execute all branches simultaneously (generate records for each)
4. At convergence node, `isWaitParallelRecord()` checks if all branches have arrived
5. Once all arrive, clear parallel info and continue

### Key Design Patterns

- **Builder Pattern**: `WorkflowBuilder`, `BaseNodeBuilder`, `ActionBuilder` with singleton `builder()` method
- **Factory Pattern**: `NodeFactory.getInstance()` (uses reflection to call static `formMap()` methods), `NodeStrategyFactory`, `FlowActionFactory`
- **Strategy Pattern**: `INodeStrategy` with `StrategyManager` - strategy-driven node configuration (operators, titles, timeouts, permissions all via strategies)
- **Template Method**: `BaseFlowNode` defines node lifecycle, `BaseAction` defines action execution, `BaseStrategy` defines strategy template
- **Singleton**: `NodeFactory`, `ScriptRuntimeContext`, `RepositoryContext`, `GatewayContext` use static final instance
- **Chain of Responsibility**: `triggerNode()` recursively traverses subsequent nodes, `StrategyManager` iterates strategies to find matches
- **Composite Pattern**: Nodes contain multiple strategies and actions
- **Copy Pattern**: `INodeStrategy.copy()`, `IFlowAction.copy()`, `BaseFlowNode.setActions()`, `BaseFlowNode.setStrategies()` for incremental updates

### Strategy-Driven Node Configuration (Critical Architecture Change)

All node configurations are now implemented through strategies rather than direct properties:
- **Operator loading**: `OperatorLoadStrategy` with Groovy script
- **Node title**: `NodeTitleStrategy` with Groovy script
- **Timeout**: `TimeoutStrategy` with timeout value
- **Permissions**: `FormFieldPermissionStrategy` with field permission list
- **Error handling**: `ErrorTriggerStrategy` with Groovy script
- **Multi-person approval**: `MultiOperatorAuditStrategy` with type (SEQUENCE/MERGE/ANY/RANDOM_ONE)

The `StrategyManager` provides unified access to all strategies:
- `loadOperators(session)` - Load operators via OperatorLoadStrategy
- `generateTitle(session)` - Generate title via NodeTitleStrategy
- `getTimeoutTime()` - Get timeout via TimeoutStrategy
- `verifySession(session)` - Verify via various strategies

### Multi-Person Approval Implementation

The `MultiOperatorAuditStrategy.Type` enum defines four approval modes:
- **SEQUENCE**: Sequential approval, hides subsequent records until previous is done
- **MERGE**: Concurrent approval, requires percentage completion (configurable via `percent` property)
- **ANY**: Any one person approval, completes immediately when first person approves
- **RANDOM_ONE**: Random one person approval, randomly selects one person from the list

The `BaseAuditNode.isDone()` method implements the completion logic for each mode.

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
- **Custom Strategies**: Extend `BaseStrategy`, implement `INodeStrategy`
- **Custom Scripts**: Use `ScriptRuntimeContext` for Groovy execution
- **Event Listeners**: Listen to `FlowRecordStartEvent`, `FlowRecordTodoEvent`, `FlowRecordDoneEvent`, `FlowRecordFinishEvent`

## Documentation References

- **flow-engine-framework/src/test/Design.md** - Comprehensive architecture documentation with updated class design, lifecycle diagrams, design patterns, and key implementation details
- **AGENTS.md** - Detailed coding guidelines and patterns
- **frontend/packages/flow-design/AGENTS.md** - Frontend library development
- **frontend/apps/app-pc/AGENTS.md** - Frontend app development
- **Rsbuild**: https://rsbuild.rs/llms.txt
- **Rspack**: https://rspack.rs/llms.txt
