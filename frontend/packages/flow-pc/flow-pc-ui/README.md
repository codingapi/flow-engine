# @flow-engine/flow-pc-ui

Flow Engine PC 端基础 UI 组件库，提供原子化 UI 组件。

## 简介

`flow-pc-ui` 是 Flow Engine PC 端的基础 UI 组件库，提供原子化的 UI 组件：

- 基础输入组件（按钮、输入框、选择器等）
- 数据展示组件（表格、列表、卡片等）
- 反馈组件（弹窗、消息提示、加载等）
- 布局组件（容器、分割线、间距等）

基于 Ant Design 组件库构建，提供统一的设计风格。

### 依赖关系

- **依赖**: 无

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

## 核心组件

### 基础输入组件

- `Button` - 按钮
- `Input` - 输入框
- `Select` - 选择器
- `Checkbox` - 复选框
- `Radio` - 单选框
- `Switch` - 开关
- `DatePicker` - 日期选择器

### 数据展示组件

- `Table` - 表格
- `List` - 列表
- `Card` - 卡片
- `Descriptions` - 描述列表
- `Timeline` - 时间轴

### 反馈组件

- `Modal` - 对话框
- `Message` - 全局提示
- `Notification` - 通知提醒框
- `Spin` - 加载状态
- `Progress` - 进度条

### 布局组件

- `Container` - 容器
- `Divider` - 分割线
- `Space` - 间距
- `Grid` - 栅格布局

## 模块结构

```
flow-pc-ui/
├── src/
│   ├── button/         # 按钮组件
│   ├── input/          # 输入框组件
│   ├── select/         # 选择器组件
│   ├── table/          # 表格组件
│   ├── modal/          # 对话框组件
│   └── index.ts        # 统一导出
└── README.md
```

## 使用示例

```typescript
import { Button, Input, Modal } from '@flow-engine/flow-pc-ui';

// 使用按钮组件
<Button type="primary" onClick={handleClick}>
  提交
</Button>

// 使用输入框组件
<Input
  placeholder="请输入内容"
  value={value}
  onChange={handleChange}
/>

// 使用对话框组件
Modal.confirm({
  title: '确认提交',
  content: '确定要提交该流程吗？',
  onOk: handleSubmit
});
```

## 主题定制

支持通过 CSS 变量进行主题定制:

```css
:root {
  --primary-color: #1890ff;
  --success-color: #52c41a;
  --warning-color: #faad14;
  --error-color: #f5222d;
}
```

## Learn more

- [Rslib documentation](https://lib.rsbuild.io/) - Rslib 特性和 API
- [Flow Engine Docs](https://github.com/codingapi/flow-engine) - 完整文档
- [CLAUDE.md](../../../CLAUDE.md) - 开发指南
