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
- **普通模式**：通过UI交互选择变量插入
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

### 2.2 可用变量列表

| 变量路径 | 说明 | 示例 |
|---------|------|------|
| `request.operatorName` | 当前操作人姓名 | "张三" |
| `request.operatorId` | 当前操作人ID | 1001 |
| `request.isFlowManager` | 是否流程管理员 | true/false |
| `request.workflowTitle` | 流程标题 | "请假审批" |
| `request.workflowCode` | 流程编码 | "LEAVE_001" |
| `request.nodeName` | 当前节点名称 | "部门审批" |
| `request.nodeType` | 当前节点类型 | "approval" |
| `request.creatorName` | 流程创建人 | "李四" |
| `request.formData("字段名")` | 表单字段值 | 任意 |
| `request.workCode` | 流程编号 | "WORK_20260226" |

---

## 3. 语法规范

### 3.1 用户界面显示语法（预览/输入）

使用 `${}` 包裹变量：

```
${request.operatorName}
${request.formData("amount")}
${request.workflowTitle}
```

**完整示例：**
```
你好，${request.operatorName}，有一笔 ${request.formData("amount")} 元的审批
```

### 3.2 实际Groovy脚本语法

```groovy
return "你好，" + request.getOperatorName() + "，有一笔 " + request.getFormData("amount") + " 元的审批"
```

### 3.3 语法转换规则

| 界面语法 | Groovy语法 |
|---------|-----------|
| `${request.operatorName}` | `request.getOperatorName()` |
| `${request.formData("key")}` | `request.getFormData("key")` |
| `${request.workflowTitle}` | `request.getWorkflowTitle()` |

### 3.4 通过注释区分标题配置

在Groovy脚本中添加固定注释来标识这是标题配置：

```groovy
// @TITLE
return "审批：" + request.getOperatorName()
```

系统通过检测 `// @TITLE` 注释来识别标题表达式配置。

---

## 4. 界面设计

### 4.1 界面清单

| 界面 | 用途 | 位置 |
|------|------|------|
| 节点属性面板 | 展示标题内容 + 编辑按钮 | 节点属性面板中 |
| 标题配置面板 | 点击编辑后的配置界面 | 弹框 |
| 变量选择器 | 插入request变量 | 弹框 |
| 高级配置 | Groovy代码编辑 | 弹框 |

### 4.2 节点属性面板

```
┌─────────────────────────────────────┐
│ 节点标题                            │
├─────────────────────────────────────┤
│ 当前：部门经理审批          [编辑] │
└─────────────────────────────────────┘
```

- 展示当前配置的标题内容
- 点击"编辑"按钮进入标题配置面板

### 4.3 标题配置面板

#### 普通模式
```
┌─────────────────────────────────────────────────────────┐
│ 标题配置                                               │
├─────────────────────────────────────────────────────────┤
│ 预览                                                    │
│ 你好，${request.operatorName}，有一笔${request.formData("amount")}元的审批
│ ─────────────────────────────────────────────────────  │
│ [插入变量]                        [高级配置]            │
│ ─────────────────────────────────────────────────────  │
│ 内容                                                    │
│ ┌─────────────────────────────────────────────────────┐│
│ │ 你好，${request.operatorName}，有一笔${request.formData("amount")}元的审批
│ └─────────────────────────────────────────────────────┘│
│ 使用 ${request.xxx} 插入变量，点击上方按钮插入          │
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

### 4.4 变量选择器（弹框）

```
┌─────────────────────────────┐
│ 选择变量                    │
├─────────────────────────────┤
│ ┌─────────────────────────┐│
│ │ 🔍 搜索变量...          ││
│ └─────────────────────────┘│
│ ▶ 操作人相关                │
│   ● request.operatorName   - 当前操作人姓名
│   ● request.operatorId    - 当前操作人ID
│   ● request.creatorName   - 流程创建人
│
│ ▶ 流程相关                  │
│   ● request.workflowTitle  - 流程标题
│   ● request.workflowCode  - 流程编码
│   ● request.nodeName      - 当前节点名称
│   ● request.nodeType      - 当前节点类型
│
│ ▶ 表单函数                  │
│   ● request.formData("key") - 获取表单字段值
│
│ ▶ 流程编号                  │
│   ● request.workCode      - 流程编号
└─────────────────────────────┘
```

---

## 5. 实现方案

### 5.1 涉及文件

**后端（Java）：**
- 创建 `TitleRequest` 对象封装所需数据

**前端（frontend）：**
- `packages/flow-pc/flow-pc-design/src/components/design-editor/node-components/strategy/node-title.tsx` - 修改现有组件
- 新增 `VariablePicker.tsx` - 变量选择器

### 5.2 核心逻辑

1. **界面显示**：使用 `${request.xxx}` 语法显示和输入
2. **保存转换**：将 `${request.xxx}` 转换为 `request.getXxx()` 或 `request.getFormData("xxx")`
3. **加载识别**：通过 `// @TITLE` 注释识别标题配置
4. **预览**：直接替换 `${request.xxx}` 为实际值（在有数据时）

---

## 6. 设计文件位置

`designs/title-expression-ui/`
