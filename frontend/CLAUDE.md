# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Flow Engine frontend is a React/TypeScript monorepo built with Rsbuild/Rspack. It provides a visual workflow designer (flow-design) and runtime applications (app-pc, app-mobile) that integrate with the Java backend workflow engine.

## Common Commands

### Package Manager

This project uses **pnpm workspaces**. Always use `pnpm` for package management.

```bash
# Install all dependencies
pnpm install

# Add a dependency to a specific package
pnpm -F @flow-engine/flow-design add <package>
```

### Building

```bash
# Build all packages in order
pnpm run build

# Build individual packages
pnpm run build:flow-types         # Build types first (no dependencies)
pnpm run build:flow-core          # Build core API library
pnpm run build:flow-pc-design     # Build flow-pc-design
pnpm run build:flow-pc-form       # Build flow-pc-form
pnpm run build:flow-pc-ui         # Build flow-pc-ui
pnpm run build:flow-pc-approval   # Build flow-pc-approval
pnpm run build:flow-pc            # Build all flow-pc packages
pnpm run build:app-pc             # Build PC application

# Watch mode for development
pnpm run watch:flow-pc-design     # Rebuild flow-pc-design on changes
pnpm run watch:flow-pc-form       # Rebuild flow-pc-form on changes
```

### Development

```bash
pnpm run dev:app-pc            # Run PC app on localhost:3000
pnpm run dev:app-mobile        # Run mobile app
```

### Testing

```bash
# Run tests (uses @rstest/core)
cd packages/flow-design
pnpm run test

# Currently: Only one basic test exists in flow-design/tests/demo.test.ts
```

## Monorepo Structure

```
frontend/
├── apps/                    # Runtime applications
│   ├── app-pc/             # PC client (React Router, proxies to localhost:8090)
│   └── app-mobile/         # Mobile client (in development)
│
├── packages/               # Shared libraries
│   ├── flow-types/         # TypeScript definitions (no dependencies)
│   ├── flow-core/          # API client + service interfaces
│   └── flow-pc/            # PC component libraries
│       ├── flow-pc-design/      # Workflow designer component library
│       ├── flow-pc-form/        # Form components
│       ├── flow-pc-ui/          # UI components
│       └── flow-pc-approval/    # Approval components
│
└── pnpm-workspace.yaml     # Workspace configuration
```

### Package Dependencies

```
flow-types (no dependencies)
    ↓
flow-core (depends on: flow-types)
    ↓
flow-pc/* (depends on: flow-types, flow-core)
    ↓                    ↓
app-pc               app-mobile
```

When making changes:
1. **flow-types**: Rebuild everything that depends on it
2. **flow-core**: Rebuild flow-pc/* and apps
3. **flow-pc/***: Rebuild apps

## Architecture

### @flowgram.ai Integration

The flow-design package is built on top of `@flowgram.ai/fixed-layout-editor`:

- **fixed-layout-editor**: Core editor that handles the visual canvas, node rendering, and drag-drop
- **fixed-semi-materials**: Semi Design UI components for node configuration
- **panel-manager-plugin**: Manages side panels (left/right panels)
- **minimap-plugin**: Adds minimap overview
- **export-plugin**: Workflow export functionality

**Critical**: The editor uses a **fixed layout system**, not absolute positioning like traditional flowchart libraries. Nodes are positioned using a coordinate system managed by the editor.

### Redux Architecture (flow-design)

State management follows a **presenter pattern** with Redux Toolkit:

```
Context (useDesignContext)
    └── state: { workflow, view }
    └── presenter: DesignPanelPresenter
        ├── getWorkflowPresenter()
        ├── getNodePresenter()
        ├── getFlowActionPresenter()
        └── getFormPresenter()
```

- **Context**: Created via `createDesignContext(props)`, provides state and presenter
- **Presenters**: Handle business logic and API calls
- **Store**: Uses Redux Toolkit slices (`workflowSlice`, `viewSlice`)
- **Selectors**: Access state via `useDesignContext()` hook

### Design Panel Structure

The `DesignPanelLayout` follows a Header-Body-Footer pattern using Ant Design components:

```
DesignPanelLayout
├── Header         - Tabs (base/form/flow/setting) + action buttons
├── Body           - Tab content components
└── Footer         - Empty (reserved)
```

**Tabs**:
- `base` - Basic workflow info (name, code)
- `form` - Form design with field configuration
- `flow` - Visual workflow design with @flowgram.ai editor
- `setting` - Advanced workflow parameters

### Approval Layout Structure

The `ApprovalLayout` uses Ant Design Layout with collapsible sidebar:

```
ApprovalLayout
├── Header         - Typography.Title "审批详情" + Button group
└── Body (Layout)
    ├── Content    - Card with FormViewComponent
    └── Sider      - Collapsible sidebar with FlowNodeHistory (Timeline)
```

**Key Ant Design Components Used**:
- `Layout`, `Content`, `Sider` - Main layout structure
- `Flex` - Flexible box layout for headers and button groups
- `Card` - Form container with title
- `Timeline` - Approval history display
- `Tag` - Status badges (color prop for different states)
- `Typography` - Text styling with `type="secondary"` for muted text
- `Button` - Action buttons with `type="primary"`, `danger`, `ghost`
- `Space` - Button grouping with spacing
- `Empty` - Empty state display

## Key Components

### flow-pc/flow-pc-design Package

**Directory Structure**:
```
packages/flow-pc/flow-pc-design/src/
├── components/
│   ├── design-panel/         # Main design panel
│   │   ├── layout/           # Layout components (Header, Body, Footer)
│   │   ├── tabs/             # Tab content components
│   │   ├── context/          # DesignPanelContext
│   │   ├── hooks/            # use-design-context
│   │   ├── store/            # Redux store and slices
│   │   └── types.ts          # TypeScript interfaces
│   │
│   ├── design-editor/        # @flowgram.ai editor wrapper
│   │   └── index.tsx         # Editor initialization
│   │
│   ├── flow-approval/        # Approval workflow components
│   │   ├── layout/           # ApprovalLayout (Header, Body)
│   │   ├── components/       # FormViewComponent, FlowNodeHistory
│   │   ├── context/          # ApprovalContext
│   │   ├── hooks/            # use-approval-context
│   │   └── typings/          # TypeScript interfaces
│   │
│   └── node-components/      # Node configuration components
│       └── strategy/         # Node strategy components
│           └── node-title.tsx # Node title configuration
│
├── services/
│   └── groovy-variable-service.ts  # Variable mapping service
│
└── utils/
    └── title-syntax-converter.ts   # Title expression syntax converter
```

### Type Definitions (flow-types)

**Critical Types** in `packages/flow-types/src/`:

```typescript
// src/types/index.ts
interface Workflow {
    id: string;
    title: string;
    code: string;
    form: FlowForm;
    strategies?: any[];
    nodes?: FlowNode[];
}

interface FlowNode {
    id: string;
    name: string;
    type: NodeType;
    blocks?: FlowNode[];    // HIERARCHICAL: child nodes
    strategies?: any[];
    actions?: FlowAction[];
}

interface FlowForm {
    name: string;
    code: string;
    fields: FormField[];
    subForms: FlowForm[];
}
```

**Important**: `FlowNode.blocks` is the hierarchical structure - NOT edge-based connections.

## State Management Patterns

### DesignPanel Context

```typescript
const {state, context} = useDesignContext();

// Access state
state.workflow.title
state.view.tabPanel

// Use presenter to trigger actions
context.getPresenter().updateTitle("New Title");
context.getPresenter().getFlowActionPresenter().action(actionId);
```

### Approval Context

```typescript
const {state, context} = useApprovalContext();

// Process nodes (approval history)
context.getPresenter().processNodes().then(nodes => {...});

// Action execution
context.getPresenter().getFlowActionPresenter().action(actionId);
```

## UI Component Conventions

### Using Ant Design Components

This project uses **Ant Design 6.x** as the primary UI library. Follow these patterns:

**Layout Components**:
```typescript
import { Layout, Flex, Space } from 'antd';

<Layout>
  <Flex justify="space-between" align="center">
    {/* Left content */}
    {/* Right content */}
  </Flex>
</Layout>
```

**Buttons**:
```typescript
import { Button } from 'antd';

// Primary action (first button)
<Button type="primary">通过</Button>

// Secondary/Destructive action
<Button danger>驳回</Button>

// Tertiary/Ghost action
<Button type="ghost">关闭</Button>

// Button group with spacing
<Space>
  <Button>Button 1</Button>
  <Button>Button 2</Button>
</Space>
```

**Card with Title**:
```typescript
import { Card } from 'antd';

<Card title="流程表单" bordered={false}>
  {/* Content */}
</Card>
```

**Collapsible Sidebar**:
```typescript
import { Layout, Sider } from 'antd';

<Layout hasSider>
  <Content>{/* Main content */}</Content>
  <Sider
    width={400}
    collapsible
    collapsedWidth={48}
    trigger={null}
  >
    {/* Sidebar content */}
  </Sider>
</Layout>
```

**Typography**:
```typescript
import { Typography } from 'antd';

const { Title, Text } = Typography;

<Title level={4}>审批详情</Title>
<Text type="secondary">辅助文本</Text>
```

**Timeline (Approval History)**:
```typescript
import { Timeline, Tag } from 'antd';
import {
  CheckCircleFilled,
  ClockCircleOutlined,
  LoadingOutlined
} from '@ant-design/icons';

<Timeline>
  <Timeline.Item
    dot={<CheckCircleFilled style={{ color: '#52c41a' }} />}
  >
    <div>节点名称</div>
    <Tag color="success">通过</Tag>
  </Timeline.Item>
</Timeline>
```

**Tag Colors for Status**:
- `color="success"` - Completed/通过
- `color="error"` - Current/待审批
- `color="default"` - Pending/待处理

**Spacing**:
- Use Ant Design's standard spacing: 8, 16, 24, 32, 48
- Use `Space` component for consistent gaps
- Use `Flex` with `gap` prop for layout spacing

### Component Patterns

**Header Pattern**:
```typescript
<Flex justify="space-between" align="center" style={{ padding: '16px 24px', borderBottom: '1px solid #f0f0f0' }}>
  <Typography.Title level={5} style={{ margin: 0 }}>页面标题</Typography.Title>
  <Space>
    <Button type="primary">主要操作</Button>
    <Button>次要操作</Button>
  </Space>
</Flex>
```

**Sidebar Pattern**:
```typescript
<Sider
  width={sidebarWidth}
  collapsible
  collapsed={collapsed}
  collapsedWidth={48}
  trigger={null}
  style={{ background: '#fff', borderLeft: '1px solid #f0f0f0' }}
>
  {!collapsed ? (
    <div>
      <Typography.Title level={5}>侧边栏标题</Typography.Title>
      {/* Content */}
    </div>
  ) : (
    <div style={{ textAlign: 'center', padding: 16 }}>
      {/* Collapsed content */}
    </div>
  )}
</Sider>
```

## Path Aliases

All packages use `@/` as an alias for `src/`:

```typescript
import {Header} from "@/components/design-panel/layout/header";
import {DesignPanelTypes} from "@/components/design-panel/types";
```

## API Integration

### Backend Proxy

During development, API requests are proxied to the backend:

```javascript
// apps/app-pc/rsbuild.config.ts
proxy: {
  '/api': 'http://localhost:8090',
  '/open': 'http://localhost:8090',
  '/user': 'http://localhost:8090',
}
```

### flow-core API Client

The `flow-core` package provides the HTTP client and service interfaces:

```typescript
// API client setup in flow-core
const client = axios.create({
  baseURL: '/api',
  timeout: 30000,
});
```

## Build System (Rsbuild/Rspack)

- **Rsbuild**: High-level build configuration (like Vite)
- **Rspack**: Rust-powered bundler (5-10x faster than webpack)
- **Rslib**: Library mode for building packages

**Configuration Files**:
- Apps: `rsbuild.config.ts`
- Libraries: `rslib.config.ts`

**Key Features**:
- TypeScript support with strict mode
- Sass/Less support for styling
- ES module output
- Tree-shaking enabled
- React Fast Refresh in dev mode

## Extension Points

### Adding New Node Types

1. Update `NodeType` in `flow-types/src/types/`
2. Create node configuration component in `flow-pc/flow-pc-design/src/components/design-editor/node-components/`
3. Register in `@flowgram.ai` editor materials

### Adding New Layout Components

Follow Ant Design patterns:
1. Use Ant Design Layout components (Layout, Content, Sider)
2. Use Flex for responsive layouts
3. Use Space for consistent spacing
4. Use Card for content containers
5. Use Typography for text styling
6. Follow existing component patterns in flow-approval/layout/

### Node Title Configuration

The node title expression supports two modes:

**Normal Mode**: Visual editor with variable picker
- Use `${label}` format: `${当前操作人}的审批`
- Insert variables via picker
- Auto-converts to Groovy script

**Advanced Mode**: Direct Groovy script editing
- Write raw Groovy code
- Must include `// @TITLE` comment
- Cannot parse back to visual mode

**Services**:
- `GroovyVariableService` - Variable mappings management (`flow-pc/flow-pc-design/src/services/groovy-variable-service.ts`)
- `TitleSyntaxConverter` - Syntax conversion utilities (`flow-pc/flow-pc-design/src/utils/title-syntax-converter.ts`)

## Documentation References

- **packages/flow-pc/flow-pc-design/README.md** - Component library documentation
- **apps/app-pc/AGENTS.md** - App development guidelines
- **../designs/title-expression-ui/README.md** - Title expression UI design
- **Ant Design**: https://ant.design/components/overview-cn/
- **Rsbuild**: https://rsbuild.rs/llms.txt
- **Rspack**: https://rspack.rs/llms.txt
- **Parent CLAUDE.md** (../CLAUDE.md) - Backend architecture and Java patterns
