# 标题表达式交互式UI设计

## 1. 需求概述

将节点标题表达式的配置从"直接编写Groovy脚本"改为"交互式可视化配置"，同时保留高级用户直接编写脚本的能力。

### 核心功能
- 文字内容用户直接输入
- 变量（request属性）通过选择方式插入
- 复杂场景可开启"高级配置"直接编辑Groovy脚本
- 支持重置配置回到初始状态

### 设计原则
- **所有标题本质都是Groovy脚本**，只是提供不同的配置方式
- **普通模式**：通过UI交互选择变量插入，显示中文格式
- **高级模式**：直接编写Groovy代码
- 高级模式下无法解析回可视化，预览区域显示提示
- 通过固定注释区分标题配置

---

## 2. request对象设计

### 2.1 Java对象结构

统一提供 `TitleGroovyRequest` 对象给所有Groovy脚本使用：

```java
public class TitleGroovyRequest {
    // 操作人信息
    private String operatorName;      // 当前操作人姓名
    private Integer operatorId;       // 当前操作人ID
    private Boolean isFlowManager;   // 是否流程管理员

    // 流程信息
    private String workflowTitle;     // 流程标题
    private String workflowCode;     // 流程编码

    // 节点信息
    private String nodeName;         // 当前节点名称
    private String nodeType;         // 当前节点类型

    // 创建人信息
    private String creatorName;      // 流程创建人姓名

    // 表单数据
    private Map<String, Object> formData;  // 表单字段值

    // 流程编号
    private String workCode;        // 流程编号

    // Getters
    public String getOperatorName();
    public Integer getOperatorId();
    public Boolean getIsFlowManager();
    public String getWorkflowTitle();
    public String getWorkflowCode();
    public String getNodeName();
    public String getNodeType();
    public String getCreatorName();
    public Object getFormData(String key);
    public String getWorkCode();
}
```

---

## 3. 变量映射类设计（核心）

### 3.1 映射类结构

创建 `GroovyVariableMapping` 映射类，统一处理变量显示和转换（作为公共对象使用）：

```java
public class GroovyVariableMapping {
    private String label;           // 中文显示名称：如"当前操作人"
    private String value;           // 变量展示名：如"request.operatorName"
    private String expression;      // 用户界面表达式：如"request.getOperatorName()"
    private String tag;             // 分组，例如表单、流程、操作人等
    private int order;              // 展示的数据顺序
}
```

### 3.2 预定义变量映射

| label(中文显示) | value(变量名) | expression(Groovy语法) | tag(分组) |
|-----------------|---------------|----------------------|----------|
| 当前操作人 | request.operatorName | request.getOperatorName() | 操作人相关 |
| 当前操作人ID | request.operatorId | request.getOperatorId() | 操作人相关 |
| 是否管理员 | request.isFlowManager | request.getIsFlowManager() | 操作人相关 |
| 流程创建人 | request.creatorName | request.getCreatorName() | 操作人相关 |
| 流程标题 | request.workflowTitle | request.getWorkflowTitle() | 流程相关 |
| 流程编码 | request.workflowCode | request.getWorkflowCode() | 流程相关 |
| 当前节点 | request.nodeName | request.getNodeName() | 流程相关 |
| 节点类型 | request.nodeType | request.getNodeType() | 流程相关 |
| 流程编号 | request.workCode | request.getWorkCode() | 流程编号 |

### 3.3 表单字段映射

表单字段需要动态从 `FlowFromMeta.fields` 获取：

```java
// 从FlowFromMeta动态构建
for (FlowFormFieldMeta field : flowFromMeta.getFields()) {
    GroovyVariableMapping mapping = new GroovyVariableMapping();
    mapping.setLabel(field.getName());                      // 如"请假天数"
    mapping.setValue("request.formData(\"" + field.getCode() + "\")");  // 如request.formData("days")
    mapping.setExpression("request.getFormData(\"" + field.getCode() + "\")");  // 如request.getFormData("days")
    mapping.setTag("表单字段（当前表单）");
    mapping.setOrder(100 + index);
}
```

---

## 4. 语法规范

### 4.1 用户界面显示语法

在界面显示和编辑时，使用 `${中文标签}` 格式：

```
${当前操作人}
${请假天数}
${流程标题}
```

**完整示例：**
```
你好，${当前操作人}，有一笔${请假天数}元的审批
```

### 4.2 实际Groovy脚本语法

```groovy
return "你好，" + request.getOperatorName() + "，有一笔 " + request.getFormData("days") + " 元的审批"
```

### 4.3 语法转换规则

通过 GroovyVariableMapping 映射类进行转换：

| label(界面显示) | value(变量名) | expression(Groovy语法) |
|-----------------|---------------|----------------------|
| 当前操作人 | request.operatorName | request.getOperatorName() |
| 请假天数 | request.formData("days") | request.getFormData("days") |
| 流程标题 | request.workflowTitle | request.getWorkflowTitle() |

### 4.4 通过注释区分标题配置

在Groovy脚本中添加固定注释来标识这是标题配置：

```groovy
// @TITLE
return "审批：" + request.getOperatorName()
```

系统通过检测 `// @TITLE` 注释来识别标题表达式配置。

---

## 5. 界面设计

### 5.1 界面清单

| 界面 | 用途 | 位置 |
|------|------|------|
| 节点属性面板 | 展示标题内容 + 编辑按钮 | 节点属性面板中 |
| 标题配置面板 | 点击编辑后的配置界面 | 弹框 |
| 变量选择器 | 插入变量（使用映射类数据） | 弹框 |
| 高级配置 | Groovy代码编辑 | 弹框 |

### 5.2 节点属性面板

```
┌─────────────────────────────────────┐
│ 节点标题                            │
├─────────────────────────────────────┤
│ ${当前操作人}的审批         [编辑] │
└─────────────────────────────────────┘
```

- 展示当前配置的标题内容（使用 `${label}` 格式）
- 点击"编辑"按钮进入标题配置面板

### 5.3 标题配置面板

#### 普通模式
```
┌─────────────────────────────────────────────────────────┐
│ 标题配置                                               │
├─────────────────────────────────────────────────────────┤
│ 预览                                                    │
│ 你好，${当前操作人}，有一笔${请假天数}元的审批
│ ─────────────────────────────────────────────────────  │
│ [插入变量]                        [高级配置]            │
│ ─────────────────────────────────────────────────────  │
│ 内容                                                    │
│ ┌─────────────────────────────────────────────────────┐│
│ │ 你好，${当前操作人}，有一笔${请假天数}元的审批       ││
│ └─────────────────────────────────────────────────────┘│
│ 点击上方按钮插入变量，或直接输入文字内容                  │
│ ─────────────────────────────────────────────────────  │
│                        [取消]  [确定]                  │
└─────────────────────────────────────────────────────────┘
```

#### 高级模式
```
┌─────────────────────────────────────────────────────────┐
│ 标题配置                                               │
├─────────────────────────────────────────────────────────┤
│ 预览                                                    │
│ ⚠ 用户自定义配置，无法预览                            │
│ ─────────────────────────────────────────────────────  │
│ [重置]                              [高级配置]          │
│ ─────────────────────────────────────────────────────  │
│ 内容                                                    │
│ ┌─────────────────────────────────────────────────────┐│
│ │ // @TITLE                                        ││
│ │ return "审批：" + request.getOperatorName()      ││
│ └─────────────────────────────────────────────────────┘│
│ ─────────────────────────────────────────────────────  │
│                        [取消]  [确定]                  │
└─────────────────────────────────────────────────────────┘
```

**关键设计说明：**
- 高级模式下预览区域显示"⚠ 用户自定义配置，无法预览"提示
- 高级模式下"高级配置"按钮变为"重置"按钮
- 点击"重置"可清除高级配置，回到普通模式

### 5.4 变量选择器（弹框）

```
┌─────────────────────────────┐
│ 选择变量                    │
├─────────────────────────────┤
│ ┌─────────────────────────┐│
│ │ 🔍 搜索变量...          ││
│ └─────────────────────────┘│
│ ▶ 操作人相关                │
│   ● 当前操作人姓名    request.operatorName
│   ● 当前操作人ID      request.operatorId
│   ● 是否管理员        request.isFlowManager
│   ● 流程创建人        request.creatorName
│
│ ▶ 流程相关                  │
│   ● 流程标题          request.workflowTitle
│   ● 流程编码          request.workflowCode
│   ● 当前节点          request.nodeName
│   ● 节点类型          request.nodeType
│
│ ▶ 表单字段（当前表单）       │
│   ● 请假天数          request.formData("days")
│   ● 请假原因          request.formData("reason")
│   ● 审批金额          request.formData("amount")
│   ● 更多字段...       动态加载当前表单字段
│
│ ▶ 流程编号                  │
│   ● 流程编号          request.workCode
└─────────────────────────────┘
```

**显示规则：**
- 左侧：label（中文显示名称），如"当前操作人姓名"
- 右侧：value（变量名），如"request.operatorName"

---

## 6. 实现方案

### 6.1 涉及文件

**后端（Java）：**
- 创建 `TitleGroovyRequest` 对象封装所需数据
- 创建 `TitleVariableMapping` 映射类（用于变量选择和语法转换）

**前端（TypeScript）：**
- 修改 `node-title.tsx` - 节点标题组件
- 新增 `VariablePicker.tsx` - 变量选择器组件

### 6.2 核心逻辑

#### 6.2.1 变量映射服务

```typescript
// 前端：GroovyVariableMapping 公共对象
interface GroovyVariableMapping {
  label: string;           // 中文显示名称：如"当前操作人"
  value: string;          // 变量展示名：如"request.operatorName"
  expression: string;     // 用户界面表达式：如"request.getOperatorName()"
  tag: string;            // 分组：如"操作人相关"
  order: number;          // 排序
}

// 预定义变量映射
const VARIABLE_MAPPINGS: GroovyVariableMapping[] = [
  { label: '当前操作人', value: 'request.operatorName', expression: 'request.getOperatorName()', tag: '操作人相关', order: 1 },
  { label: '当前操作人ID', value: 'request.operatorId', expression: 'request.getOperatorId()', tag: '操作人相关', order: 2 },
  // ...其他预定义变量
];

// 获取表单字段映射（动态）
function getFormFieldMappings(fields: FlowFormFieldMeta[]): GroovyVariableMapping[] {
  return fields.map((field, index) => ({
    label: field.name,
    value: 'request.formData("' + field.code + '")',
    expression: 'request.getFormData("' + field.code + '")',
    tag: '表单字段（当前表单）',
    order: 100 + index
  }));
}
```

#### 6.2.2 界面显示 → Groovy语法转换

```typescript
function toGroovySyntax(content: string, mappings: GroovyVariableMapping[]): string {
  let result = content;
  for (const mapping of mappings) {
    // 将界面显示的label替换为Groovy expression
    result = result.split(mapping.label).join('${' + mapping.expression + '}');
  }
  // 添加 @TITLE 注释，并转换为Groovy字符串拼接
  return '// @TITLE\nreturn "' + result + '"';
}
```

#### 6.2.3 Groovy语法 → 界面显示转换

```typescript
function toLabelExpression(groovyCode: string, mappings: GroovyVariableMapping[]): string {
  let result = groovyCode;
  // 移除 return " 和 "
  result = result.replace(/^.*return\s+"([^"]*)".*$/, '$1');
  // 替换 + 拼接为直接量
  result = result.replace(/"\s*\+\s*"/g, '');
  // 将 ${expression} 替换为 label
  for (const mapping of mappings) {
    result = result.replaceAll('${' + mapping.expression + '}', mapping.label);
  }
  return result;
}
```

#### 6.2.4 变量选择器数据源

```typescript
// 合并预定义变量 + 动态表单字段
function getVariablePickerData(flowFromMeta?: FlowFromMeta): GroovyVariableMapping[] {
  const variables = [...VARIABLE_MAPPINGS];

  // 添加表单字段（如果有）
  if (flowFromMeta?.fields) {
    variables.push(...getFormFieldMappings(flowFromMeta.fields));
  }

  // 按tag和order排序
  return variables.sort((a, b) => {
    if (a.tag !== b.tag) return a.tag.localeCompare(b.tag);
    return a.order - b.order;
  });
}
```

### 6.3 数据流

```
┌─────────────────┐     ┌──────────────────────┐     ┌─────────────────┐
│  用户界面输入   │────▶│ GroovyVariableMapping │────▶│  Groovy脚本     │
│ 当前操作人     │     │       映射类         │     │ request.getXxx()│
└─────────────────┘     └──────────────────────┘     └─────────────────┘
                                   │
                                   ▼
                            ┌──────────────────┐
                            │   变量选择器     │
                            │   数据源        │
                            └──────────────────┘
                                   │
                                   ▼
                            ┌──────────────────┐
                            │   FlowFromMeta  │
                            │  动态获取字段    │
                            └──────────────────┘
```

---

## 7. 设计文件位置

`designs/title-expression-ui/`
