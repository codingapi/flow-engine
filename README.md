# Flow Engine

> Enterprise Workflow Engine - Visual Process Design, Dynamic Form Configuration, Multi-node Type Flow

## Introduction

Flow Engine is an enterprise-grade workflow engine built with Java 17 and Spring Boot 3.5.9, providing complete workflow management capabilities including visual process design, dynamic form configuration, multi-node type flow, and script extension. It adopts a frontend-backend separation architecture, supporting both PC and mobile endpoints.

### Core Features

- **12 Node Types** - Start, End, Approval, Handle, Notify, Condition Branch, Parallel Branch, Router, Inclusive Branch, Sub-process, Delay, Trigger nodes
- **8 Action Types** - Pass, Reject, Save, Add Audit, Delegate, Return, Transfer, Custom
- **Strategy-driven Configuration** - All key configurations implemented through strategies with dynamic extension support
- **Groovy Script Extension** - Supports dynamic initiator matching, approver loading, condition evaluation, custom actions, etc.
- **Multi-person Approval Modes** - Sequential approval, merged approval (configurable ratio), any-one approval, random approval
- **Thread Safety** - Script runtime uses fine-grained synchronization locks, supporting concurrent execution of different scripts
- **Auto Resource Cleanup** - Dual cleanup mechanism (threshold-triggered + scheduled cleanup) to avoid memory leaks
- **Comprehensive Exception System** - Framework exception hierarchy based on RuntimeException

## Project Structure

```
flow-engine
├── flow-engine-framework         # Core workflow engine framework
│   └── src/main/java/com.codingapi.flow
│       ├── action                # Action layer
│       │   ├── actions           # Action implementations (8 classes)
│       │   │   ├── PassAction    # Pass action
│       │   │   ├── RejectAction  # Reject action
│       │   │   ├── SaveAction    # Save action
│       │   │   ├── ReturnAction  # Return action
│       │   │   ├── TransferAction # Transfer action
│       │   │   ├── AddAuditAction # Add audit action
│       │   │   ├── DelegateAction # Delegate action
│       │   │   └── CustomAction  # Custom action
│       │   ├── factory           # FlowActionFactory action factory
│       │   ├── ActionType        # Action type enum (9 types)
│       │   ├── BaseAction        # Action abstract base class
│       │   └── IFlowAction       # Action interface
│       ├── backup                # Workflow backup
│       │   └── WorkflowBackup    # Workflow backup management
│       ├── builder               # Builders (5 types)
│       │   ├── ActionBuilder             # Action builder
│       │   ├── BaseNodeBuilder            # Node builder base class
│       │   ├── FormFieldPermissionsBuilder # Field permission builder
│       │   ├── NodeMapBuilder             # Node map builder
│       │   └── NodeStrategyBuilder        # Node strategy builder
│       ├── context               # Context
│       │   ├── GatewayContext           # Gateway context
│       │   └── RepositoryHolderContext  # Repository holder context
│       ├── delay                 # Delay task
│       │   ├── DelayTask         # Delay task
│       │   └── DelayTaskManager  # Delay task manager
│       ├── edge                  # Node connection
│       │   └── FlowEdge          # Flow edge
│       ├── error                 # Error handling
│       │   └── ErrorThrow        # Error thrower
│       ├── event                 # Event system (5 types)
│       │   ├── IFlowEvent                # Event interface
│       │   ├── FlowRecordStartEvent      # Flow start event
│       │   ├── FlowRecordTodoEvent       # Todo event
│       │   ├── FlowRecordDoneEvent       # Done event
│       │   └── FlowRecordFinishEvent     # Flow finish event
│       ├── exception             # Exception system (6 types)
│       │   ├── FlowException             # Exception base class
│       │   ├── FlowValidationException   # Parameter validation exception
│       │   ├── FlowNotFoundException     # Resource not found exception
│       │   ├── FlowStateException        # State exception
│       │   ├── FlowPermissionException   # Permission exception
│       │   ├── FlowConfigException       # Configuration exception
│       │   └── FlowExecutionException    # Execution exception
│       ├── form                  # Form system
│       │   ├── permission        # Field permissions (READ/WRITE/NONE)
│       │   ├── FormData          # Form data
│       │   ├── FormMeta          # Form metadata
│       │   └── FormMetaBuilder   # Form builder
│       ├── gateway               # Gateway interface anti-corruption layer
│       │   └── FlowOperatorGateway # Operator gateway
│       ├── operator              # Operator interface
│       │   └── IFlowOperator     # Operator interface
│       ├── node                  # Node layer
│       │   ├── nodes             # Node implementations (12 types)
│       │   │   ├── StartNode     # Start node
│       │   │   ├── EndNode       # End node
│       │   │   ├── ApprovalNode  # Approval node
│       │   │   ├── HandleNode    # Handle node
│       │   │   ├── NotifyNode    # Notify node
│       │   │   ├── ConditionBranchNode # Condition branch node
│       │   │   ├── ParallelBranchNode    # Parallel branch node
│       │   │   ├── RouterNode    # Router node
│       │   │   ├── InclusiveBranchNode  # Inclusive branch node
│       │   │   ├── SubProcessNode # Sub-process node
│       │   │   ├── DelayNode     # Delay node
│       │   │   └── TriggerNode   # Trigger node
│       │   ├── factory           # NodeFactory node factory
│       │   ├── helper            # ParallelNodeRelationHelper parallel helper
│       │   ├── manager           # Node managers
│       │   │   ├── ActionManager     # Action manager
│       │   │   ├── OperatorManager    # Operator manager
│       │   │   ├── StrategyManager    # Strategy manager
│       │   │   └── FieldPermissionManager # Field permission manager
│       │   ├── BaseFlowNode      # Node abstract base class
│       │   ├── BaseAuditNode     # Audit node abstract base class
│       │   └── IFlowNode         # Node interface
│       ├── pojo                  # Data objects
│       │   ├── body              # FlowAdviceBody request body
│       │   └── request           # FlowActionRequest, FlowCreateRequest
│       ├── record                # Flow record
│       │   └── FlowRecord        # Execution record (TODO/DONE state)
│       ├── repository            # Repository interfaces (persistence abstraction)
│       │   ├── WorkflowRepository
│       │   ├── FlowRecordRepository
│       │   ├── WorkflowBackupRepository
│       │   ├── ParallelBranchRepository
│       │   └── DelayTaskRepository
│       ├── script                # Script system
│       │   ├── node              # Node scripts (9 types)
│       │   │   ├── OperatorMatchScript  # Initiator matching script
│       │   │   ├── OperatorLoadScript   # Approver loading script
│       │   │   ├── NodeTitleScript      # Node title script
│       │   │   ├── ConditionScript      # Condition evaluation script
│       │   │   ├── RouterNodeScript     # Router script
│       │   │   ├── SubProcessScript     # Sub-process script
│       │   │   ├── TriggerScript        # Trigger script
│       │   │   └── ErrorTriggerScript   # Error trigger script
│       │   ├── runtime           # Script runtime
│       │   │   ├── ScriptRuntimeContext # Groovy script execution environment
│       │   │   ├── FlowScriptContext    # Script context
│       │   │   └── IBeanFactory         # Bean factory interface
│       │   └── action            # Action scripts (2 types)
│       │       ├── RejectActionScript   # Reject action script
│       │       └── CustomScript         # Custom action script
│       ├── service               # Service layer
│       │   ├── impl              # Service implementations
│       │   │   ├── FlowCreateService    # Flow creation service
│       │   │   ├── FlowActionService    # Flow action service
│       │   │   └── FlowDelayTriggerService # Delay trigger service
│       │   └── FlowService       # Service interface
│       ├── session               # Session layer
│       │   ├── FlowSession       # Execution context
│       │   └── FlowAdvice        # Approval parameters (opinion, signature, return node, etc.)
│       ├── strategy              # Strategy layer (13 types)
│       │   ├── MultiOperatorAuditStrategy  # Multi-person approval strategy
│       │   ├── TimeoutStrategy          # Timeout strategy
│       │   ├── SameOperatorAuditStrategy # Same operator approval strategy
│       │   ├── RecordMergeStrategy      # Record merge strategy
│       │   ├── ResubmitStrategy         # Resubmit strategy
│       │   ├── AdviceStrategy           # Approval opinion strategy
│       │   ├── OperatorLoadStrategy     # Approver loading strategy
│       │   ├── ErrorTriggerStrategy     # Error trigger strategy
│       │   ├── NodeTitleStrategy        # Node title strategy
│       │   ├── FormFieldPermissionStrategy # Field permission strategy
│       │   ├── DelayStrategy            # Delay strategy
│       │   ├── TriggerStrategy          # Trigger strategy
│       │   ├── SubProcessStrategy       # Sub-process strategy
│       │   ├── NodeStrategyFactory      # Strategy factory
│       │   ├── BaseStrategy             # Strategy abstract base class
│       │   └── INodeStrategy            # Strategy interface
│       ├── utils                 # Utility classes
│       │   ├── RandomUtils       # Random utility
│       │   └── Sha256Utils       # SHA256 encryption utility
│       └── workflow              # Workflow layer
│           ├── Workflow          # Workflow object
│           └── WorkflowBuilder   # Workflow builder
│   └── src/test/java             # Test code
├── flow-engine-starter           # Spring Boot starter
├── flow-engine-starter-infra     # Persistence layer starter
├── flow-engine-example           # Example project
└── frontend                      # Frontend project
    ├── apps
    │   ├── app-pc                # PC application
    │   └── app-mobile            # Mobile application
    └── packages
        ├── flow-design           # Flow designer
        ├── flow-pc               # PC display components
        └── flow-mobile           # Mobile display components
```

## Tech Stack

### Backend

- **Java 17** - Programming language
- **Spring Boot 3.5.9** - Application framework
- **Groovy** - Script engine
- **Lombok** - Code simplification
- **Fastjson** - JSON processing
- **Apache Commons** - Utility library

### Frontend

- **React** - UI framework
- **TypeScript** - Type safety
- **Rsbuild** - Build tool
- **pnpm** - Package manager

## Quick Start

### Backend

```bash
# Clone project
git clone https://github.com/codingapi/flow-engine.git
cd flow-engine

# Build project
./mvnw clean install

# Run example project
cd flow-engine-example
mvn spring-boot:run
```

### Frontend

```bash
cd frontend

# Install dependencies
pnpm install

# Build design library
pnpm run build:flow-engine

# Start PC application
pnpm run dev:app-pc
```

## Core Architecture

### Eight-layer Architecture

1. **Workflow Layer** - Workflow definition layer
2. **Node Layer** - Node layer (12 node types)
3. **Action Layer** - Action layer (9 action types)
4. **Record Layer** - Record layer
5. **Session Layer** - Session layer
6. **Manager Layer** - Manager layer
7. **Strategy Layer** - Strategy layer
8. **Script Layer** - Script layer

### Design Patterns

- **Builder Pattern** - Builder pattern
- **Factory Pattern** - Factory pattern
- **Strategy Pattern** - Strategy pattern
- **Template Method** - Template method pattern
- **Singleton Pattern** - Singleton pattern
- **Chain of Responsibility** - Chain of responsibility pattern
- **Composite Pattern** - Composite pattern

## Node Types

| Node Type | Description | NODE_TYPE |
|-----------|-------------|-----------|
| StartNode | Start node | `start` |
| EndNode | End node | `end` |
| ApprovalNode | Approval node | `approval` |
| HandleNode | Handle node | `handle` |
| NotifyNode | Notify node | `notify` |
| ConditionBranchNode | Condition branch | `condition_branch` |
| ParallelBranchNode | Parallel branch | `parallel_branch` |
| RouterNode | Router branch | `router` |
| InclusiveBranchNode | Inclusive branch | `inclusive_branch` |
| SubProcessNode | Sub-process node | `sub_process` |
| DelayNode | Delay node | `delay` |
| TriggerNode | Trigger node | `trigger` |

## Action Types

| Action Type | Description | ActionType |
|-------------|-------------|------------|
| PassAction | Pass | `PASS` |
| RejectAction | Reject | `REJECT` |
| SaveAction | Save | `SAVE` |
| ReturnAction | Return | `RETURN` |
| TransferAction | Transfer | `TRANSFER` |
| AddAuditAction | Add audit | `ADD_AUDIT` |
| DelegateAction | Delegate | `DELEGATE` |
| CustomAction | Custom | `CUSTOM` |

## Multi-person Approval Modes

| Mode | Description | Completion Condition |
|------|-------------|---------------------|
| SEQUENCE | Sequential approval | All complete in sequence |
| MERGE | Merged approval | Configured ratio of people complete |
| ANY | Any-one approval | Any one person completes |
| RANDOM_ONE | Random approval | Randomly selected person completes |

## Exception Code Format

All framework exceptions use string-based error codes following the pattern:

```
category.subcategory.errorType
```

Examples:
- `notFound.workflow.definition` - Workflow definition not found
- `permission.field.readOnly` - Field is read-only
- `state.record.alreadyDone` - Record already completed
- `validation.field.required` - Required field is empty
- `config.node.strategies.required` - Node strategies required
- `execution.script.error` - Script execution error

All exception messages are in English.

## Documentation

- [PRD.md](PRD.md) - Product requirements document
- [Design.md](Design.md) - Architecture design document
- [AGENTS.md](AGENTS.md) - Coding standards
- [CLAUDE.md](CLAUDE.md) - Claude Code guide

## Testing

```bash
# Run all tests
./mvnw test

# Run specific module tests
./mvnw test -pl flow-engine-framework

# Run specific test class
./mvnw test -Dtest=ScriptRuntimeContextTest
```

## License

[LICENSE](LICENSE)

## Contributing

Issues and Pull Requests are welcome!
