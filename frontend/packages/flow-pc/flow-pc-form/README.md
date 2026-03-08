# @flow-engine/flow-pc-form

Flow Engine PC 端表单组件库，提供表单设计器、表单渲染等功能。

## 简介

`flow-pc-form` 是 Flow Engine PC 端的表单相关组件库，提供：

- 表单设计器（可视化表单设计）
- 表单渲染器（动态表单渲染）
- 表单字段组件
- 表单验证规则
- 表单权限控制

### 依赖关系

- **依赖**: `@flow-engine/flow-core`, `@flow-engine/flow-types`

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

### 表单设计器

可视化表单设计器，支持：

- 拖拽添加字段
- 字段属性配置
- 字段排序
- 表单预览
- 表单 schema 导出

```typescript
import { FormDesigner } from '@flow-engine/flow-pc-form';

<FormDesigner
  schema={formSchema}
  onChange={handleSchemaChange}
  onPreview={handlePreview}
/>
```

### 表单渲染器

根据表单 schema 动态渲染表单：

```typescript
import { FormRenderer } from '@flow-engine/flow-pc-form';

<FormRenderer
  schema={formSchema}
  values={formValues}
  onChange={handleValuesChange}
  permissions={fieldPermissions}
/>
```

### 表单字段

支持多种字段类型：

- 单行文本
- 多行文本
- 数字
- 日期
- 下拉选择
- 单选/多选
- 人员选择
- 部门选择
- 附件上传

### 表单验证

内置验证规则：

- 必填验证
- 长度限制
- 格式验证（邮箱、手机号等）
- 自定义验证规则

### 表单权限

字段级别权限控制：

- 可编辑
- 只读
- 隐藏
- 必填

## 模块结构

```
flow-pc-form/
├── src/
│   ├── designer/       # 表单设计器
│   ├── renderer/       # 表单渲染器
│   ├── fields/         # 表单字段组件
│   ├── validator/      # 表单验证
│   ├── schema/         # 表单 schema 定义
│   └── index.ts        # 统一导出
└── README.md
```

## 使用示例

### 表单设计器

```typescript
import { FormDesigner } from '@flow-engine/flow-pc-form';
import type { FlowForm } from '@flow-engine/flow-types';

const MyFormDesigner = () => {
  const [schema, setSchema] = useState<FlowForm>({ ... });

  return (
    <FormDesigner
      schema={schema}
      onChange={setSchema}
      onSave={handleSave}
    />
  );
};
```

### 表单渲染器

```typescript
import { FormRenderer } from '@flow-engine/flow-pc-form';
import type { FlowForm, FormFieldPermission } from '@flow-engine/flow-types';

const MyFormRenderer = () => {
  const [values, setValues] = useState({});

  return (
    <FormRenderer
      schema={formSchema}
      values={values}
      onChange={setValues}
      permissions={permissions}
    />
  );
};
```

## Learn more

- [Rslib documentation](https://lib.rsbuild.io/) - Rslib 特性和 API
- [Flow Engine Docs](https://github.com/codingapi/flow-engine) - 完整文档
- [CLAUDE.md](../../../CLAUDE.md) - 开发指南
