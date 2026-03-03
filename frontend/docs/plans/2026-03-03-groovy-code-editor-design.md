# GroovyCodeEditor 设计方案

## 概述

为 Flow Engine 工作流设计器创建一个专门的 Groovy 代码编辑器组件，用于替代当前使用的 PythonCodeEditor。

## 背景

当前 `ScriptEditor` 组件使用 `@flowgram.ai/form-materials` 中的 `PythonCodeEditor` 来编辑 Groovy 脚本，这存在以下问题：
1. 编辑器语言标识不正确（显示 Python 而非 Groovy）
2. 语法高亮不匹配（Python 语法与 Groovy 有差异）

## 技术选型

基于 **CodeMirror 6** 实现，理由：
- 项目中已安装相关依赖（@codemirror/* 系列）
- 社区成熟，支持扩展
- 与现有 @flowgram.ai/coze-editor 技术栈一致

### 依赖

- `@codemirror/view` - 核心视图
- `@codemirror/state` - 状态管理
- `@codemirror/lang-java` - Java 语法支持（Groovy 与 Java 语法高度兼容）
- `@codemirror/theme-one-dark` - 深色主题

## 组件接口

```typescript
interface GroovyCodeEditorProps {
  /** 当前脚本内容 */
  value: string;
  /** 是否只读 */
  readonly?: boolean;
  /** 内容变更回调 */
  onChange?: (value: string) => void;
  /** 占位符 */
  placeholder?: string;
  /** 主题 */
  theme?: 'dark' | 'light';
  /** 编辑器选项 */
  options?: {
    fontSize?: number;
    minHeight?: number;
    maxHeight?: number;
  };
}
```

## 文件结构

```
packages/flow-pc/flow-pc-design/src/components/script/components/
├── groovy-code-editor.tsx    # 新增：Groovy 代码编辑器
└── advanced-script-editor.tsx  # 修改：替换 PythonCodeEditor
```

## 实现要点

### 1. 语法高亮
使用 `@codemirror/lang-java` 实现 Groovy 语法高亮，该语言包与 Groovy 兼容性较好。

### 2. 主题支持
- 深色主题：使用 `@codemirror/theme-one-dark`
- 浅色主题：使用默认浅色主题

### 3. 样式适配
- 编辑器容器样式与现有设计系统一致
- 支持最小/最大高度限制
- 自定义字体大小

## 测试计划

1. 单元测试：验证组件渲染、props 传递
2. 手动测试：
   - 验证 Groovy 代码语法高亮效果
   - 验证主题切换功能
   - 验证只读模式
   - 验证与其他组件的集成

## 替代方案

如后续需要更完善的 Groovy 支持，可考虑：
1. 基于 @codemirror/language 自定义 Groovy 语法高亮
2. 添加 Groovy 特定关键字和内置函数自动补全
