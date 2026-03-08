# @flow-engine/flow-pc-approval

Flow Engine PC 端审批组件库，提供待办/已办/审批处理等功能。

## 简介

`flow-pc-approval` 是 Flow Engine PC 端的审批页面组件库，提供：

- 待办列表（我的待办）
- 已办列表（我的已办）
- 我发起的（我的申请）
- 审批处理（审批操作）
- 流程详情查看
- 审批记录展示

### 依赖关系

- **依赖**: `@flow-engine/flow-pc-design`, `@flow-engine/flow-pc-ui`

## Setup

安装依赖:

```bash
pnpm install
```

## 开发

构建组件库:

```bash
pnpm run build
```

监听模式构建:

```bash
pnpm run dev
```

## 核心功能

### 待办列表

我的待办任务列表：

- 任务筛选（按状态、类型、时间等）
- 任务搜索
- 批量操作
- 分页加载

```typescript
import { TodoList } from '@flow-engine/flow-pc-approval';

<TodoList
  filters={filters}
  onTaskClick={handleTaskClick}
  onBatchApprove={handleBatchApprove}
/>
```

### 已办列表

我的已办任务列表：

- 历史记录查询
- 筛选和搜索
- 查看审批详情

```typescript
import { DoneList } from '@flow-engine/flow-pc-approval';

<DoneList
  filters={filters}
  onTaskClick={handleTaskClick}
/>
```

### 我发起的

我发起的流程列表：

- 流程状态查看
- 流程跟踪
- 撤回/取消操作

```typescript
import { MyRequestsList } from '@flow-engine/flow-pc-approval';

<MyRequestsList
  filters={filters}
  onFlowClick={handleFlowClick}
  onWithdraw={handleWithdraw}
/>
```

### 审批处理

审批操作组件：

- 表单展示（只读/编辑）
- 审批意见填写
- 审批操作（通过/拒绝/保存/转交/退回/委托）
- 附件上传

```typescript
import { ApprovalForm } from '@flow-engine/flow-pc-approval';

<ApprovalForm
  taskId={taskId}
  formSchema={formSchema}
  formValues={formValues}
  permissions={fieldPermissions}
  onApprove={handleApprove}
  onReject={handleReject}
  onSave={handleSave}
  onTransfer={handleTransfer}
  onReturn={handleReturn}
  onDelegate={handleDelegate}
/>
```

### 流程详情

流程详情查看：

- 流程基本信息
- 节点状态展示
- 审批记录时间轴
- 表单数据展示

```typescript
import { FlowDetail } from '@flow-engine/flow-pc-approval';

<FlowDetail
  workflowId={workflowId}
  instanceId={instanceId}
  onBack={handleBack}
/>
```

### 审批记录

审批记录展示：

- 时间轴展示
- 审批人信息
- 审批意见
- 审批时间
- 操作记录

```typescript
import { ApprovalTimeline } from '@flow-engine/flow-pc-approval';

<ApprovalTimeline
  records={approvalRecords}
  currentNodeId={currentNodeId}
/>
```

## 模块结构

```
flow-pc-approval/
├── src/
│   ├── todo/           # 待办列表
│   ├── done/           # 已办列表
│   ├── my-requests/    # 我发起的
│   ├── approval-form/  # 审批表单
│   ├── flow-detail/    # 流程详情
│   ├── timeline/       # 审批记录时间轴
│   ├── components/     # 公共组件
│   └── index.ts        # 统一导出
└── README.md
```

## 使用示例

### 待办页面

```typescript
import { TodoList, ApprovalModal } from '@flow-engine/flow-pc-approval';

const TodoPage = () => {
  const [selectedTask, setSelectedTask] = useState(null);
  const [showApprovalModal, setShowApprovalModal] = useState(false);

  const handleTaskClick = async (task) => {
    setSelectedTask(task);
    setShowApprovalModal(true);
  };

  return (
    <>
      <TodoList
        filters={filters}
        onTaskClick={handleTaskClick}
      />
      {showApprovalModal && (
        <ApprovalModal
          taskId={selectedTask.id}
          onClose={() => setShowApprovalModal(false)}
          onApproved={handleRefresh}
        />
      )}
    </>
  );
};
```

### 审批操作

```typescript
import { ApprovalForm } from '@flow-engine/flow-pc-approval';

const MyApprovalForm = () => {
  const handleSubmit = async (action, opinion) => {
    await approvalApi.submit({
      taskId,
      action,
      opinion,
      formData
    });
  };

  return (
    <ApprovalForm
      taskId={taskId}
      formSchema={formSchema}
      formValues={formValues}
      permissions={permissions}
      onApprove={(opinion) => handleSubmit('PASS', opinion)}
      onReject={(opinion) => handleSubmit('REJECT', opinion)}
      onSave={(data) => handleSubmit('SAVE', data)}
      onTransfer={(userId) => handleSubmit('TRANSFER', { userId })}
      onReturn={(nodeId) => handleSubmit('RETURN', { nodeId })}
      onDelegate={(userId) => handleSubmit('DELEGATE', { userId })}
    />
  );
};
```

## 审批动作

支持以下审批动作：

- `PASS` - 通过
- `REJECT` - 拒绝
- `SAVE` - 保存
- `ADD_AUDIT` - 加签
- `DELEGATE` - 委托
- `RETURN` - 退回
- `TRANSFER` - 转交
- `CUSTOM` - 自定义动作

## Learn more

- [Rslib documentation](https://lib.rsbuild.io/) - Rslib 特性和 API
- [Flow Engine Docs](https://github.com/codingapi/flow-engine) - 完整文档
- [CLAUDE.md](../../../CLAUDE.md) - 开发指南
