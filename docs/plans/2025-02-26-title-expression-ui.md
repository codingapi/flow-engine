# Title Expression UI Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build an interactive UI for node title expression configuration, replacing direct Groovy script editing with a user-friendly visual interface.

**Architecture:**
- Frontend maintains variable mappings (predefined + dynamic form fields)
- Backend provides `TitleGroovyRequest` for script execution
- Syntax conversion between display format (`${label}`) and Groovy code
- Two modes: Normal (visual) and Advanced (raw Groovy)

**Tech Stack:**
- Backend: Java 17, Spring Boot 3.5.9, Groovy ScriptRuntimeContext
- Frontend: React, TypeScript, Ant Design 6.x, @flowgram.ai/fixed-layout-editor

---

## Phase 1: Backend Foundation

### Task 1: Create TitleGroovyRequest

**Files:**
- Create: `flow-engine-framework/src/main/java/com/codingapi/flow/script/runtime/TitleGroovyRequest.java`
- Test: `flow-engine-framework/src/test/java/com/codingapi/flow/script/runtime/TitleGroovyRequestTest.java`

**Step 1: Write the failing test**

```java
package com.codingapi.flow.script.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TitleGroovyRequestTest {

    @Test
    void testGetOperatorName() {
        TitleGroovyRequest request = new TitleGroovyRequest();
        request.setOperatorName("张三");
        assertEquals("张三", request.getOperatorName());
    }

    @Test
    void testGetFormData() {
        TitleGroovyRequest request = new TitleGroovyRequest();
        Map<String, Object> formData = new HashMap<>();
        formData.put("days", 5);
        request.setFormData(formData);
        assertEquals(5, request.getFormData("days"));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=TitleGroovyRequestTest -pl flow-engine-framework`
Expected: FAIL with "TitleGroovyRequest not found"

**Step 3: Write minimal implementation**

```java
package com.codingapi.flow.script.runtime;

import lombok.Data;
import java.util.Map;

/**
 * 标题表达式Groovy脚本请求对象
 * 提供给NodeTitleScript使用的上下文数据
 */
@Data
public class TitleGroovyRequest {

    // ========== 操作人信息 ==========
    /**
     * 当前操作人姓名
     */
    private String operatorName;

    /**
     * 当前操作人ID
     */
    private Integer operatorId;

    /**
     * 是否流程管理员
     */
    private Boolean isFlowManager;

    // ========== 流程信息 ==========
    /**
     * 流程标题
     */
    private String workflowTitle;

    /**
     * 流程编码
     */
    private String workflowCode;

    /**
     * 当前节点名称
     */
    private String nodeName;

    /**
     * 当前节点类型
     */
    private String nodeType;

    // ========== 创建人信息 ==========
    /**
     * 流程创建人姓名
     */
    private String creatorName;

    // ========== 表单数据 ==========
    /**
     * 表单字段值
     */
    private Map<String, Object> formData;

    // ========== 流程编号 ==========
    /**
     * 流程编号
     */
    private String workCode;

    /**
     * 获取表单字段值（Groovy脚本调用）
     */
    public Object getFormData(String key) {
        if (formData == null) {
            return null;
        }
        return formData.get(key);
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=TitleGroovyRequestTest -pl flow-engine-framework`
Expected: PASS

**Step 5: Commit**

```bash
git add flow-engine-framework/src/main/java/com/codingapi/flow/script/runtime/TitleGroovyRequest.java
git add flow-engine-framework/src/test/java/com/codingapi/flow/script/runtime/TitleGroovyRequestTest.java
git commit -m "feat: add TitleGroovyRequest for node title expression"
```

---

### Task 2: Create GroovyVariableMapping DTO

**Files:**
- Create: `flow-engine-starter/src/main/java/com/codingapi/flow/web/dto/GroovyVariableMapping.java`

**Step 1: Create the DTO class**

```java
package com.codingapi.flow.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Groovy变量映射DTO
 * 用于前后端变量映射统一
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroovyVariableMapping {

    /**
     * 中文显示名称：如"当前操作人"
     */
    private String label;

    /**
     * 变量展示名：如"request.operatorName"
     */
    private String value;

    /**
     * Groovy表达式：如"request.getOperatorName()"
     */
    private String expression;

    /**
     * 分组标签：如"操作人相关"
     */
    private String tag;

    /**
     * 排序序号
     */
    private Integer order;
}
```

**Step 2: Commit**

```bash
git add flow-engine-starter/src/main/java/com/codingapi/flow/web/dto/GroovyVariableMapping.java
git commit -m "feat: add GroovyVariableMapping DTO"
```

---

### Task 3: Update NodeTitleScript to use TitleGroovyRequest

**Files:**
- Modify: `flow-engine-framework/src/main/java/com/codingapi/flow/script/node/NodeTitleScript.java`
- Test: `flow-engine-framework/src/test/java/com/codingapi/flow/script/node/NodeTitleScriptTest.java`

**Step 1: Write the failing test**

```java
package com.codingapi.flow.script.node;

import com.codingapi.flow.script.runtime.TitleGroovyRequest;
import com.codingapi.flow.session.FlowSession;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NodeTitleScriptTest {

    @Test
    void testExecuteWithSimpleScript() {
        NodeTitleScript script = new NodeTitleScript("def run(request){return '你有一条待办'}");
        FlowSession session = new FlowSession();
        String result = script.execute(session);
        assertEquals("你有一条待办", result);
    }

    @Test
    void testExecuteWithVariableScript() {
        String script = "def run(request){return request.getOperatorName() + '的审批'}";
        NodeTitleScript titleScript = new NodeTitleScript(script);

        FlowSession session = new FlowSession();
        // Setup session with required data
        // TODO: Add test data to session

        String result = titleScript.execute(session);
        assertNotNull(result);
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=NodeTitleScriptTest -pl flow-engine-framework`
Expected: Current tests may pass, new test needs session setup

**Step 3: Update FlowSession to include title request data**

Review: `flow-engine-framework/src/main/java/com/codingapi/flow/session/FlowSession.java`

Add method to FlowSession for creating TitleGroovyRequest:

```java
/**
 * 创建标题请求对象
 * 从当前session构建TitleGroovyRequest
 */
public TitleGroovyRequest createTitleRequest() {
    TitleGroovyRequest request = new TitleGroovyRequest();

    // 操作人信息
    if (currentOperator != null) {
        request.setOperatorName(currentOperator.getName());
        request.setOperatorId(currentOperator.getId());
    }

    // 流程信息
    if (workflow != null) {
        request.setWorkflowTitle(workflow.getTitle());
        request.setWorkflowCode(workflow.getCode());
    }

    // 节点信息
    if (currentNode != null) {
        request.setNodeName(currentNode.getName());
        request.setNodeType(currentNode.getType().name());
    }

    // 表单数据
    request.setFormData(formData);

    // 流程编号（从record获取）
    if (currentRecord != null) {
        request.setWorkCode(currentRecord.getWorkCode());
    }

    return request;
}
```

**Step 4: Update NodeTitleScript to use TitleGroovyRequest**

```java
public String execute(FlowSession session) {
    TitleGroovyRequest request = session.createTitleRequest();
    return ScriptRuntimeContext.getInstance().run(script, String.class, request);
}
```

**Step 5: Run tests to verify they pass**

Run: `./mvnw test -Dtest=NodeTitleScriptTest -pl flow-engine-framework`
Expected: PASS

**Step 6: Commit**

```bash
git add flow-engine-framework/src/main/java/com/codingapi/flow/session/FlowSession.java
git add flow-engine-framework/src/main/java/com/codingapi/flow/script/node/NodeTitleScript.java
git add flow-engine-framework/src/test/java/com/codingapi/flow/script/node/NodeTitleScriptTest.java
git commit -m "feat: update NodeTitleScript to use TitleGroovyRequest"
```

---

## Phase 2: Frontend Foundation

### Task 4: Create TypeScript types for variable mapping

**Files:**
- Create: `frontend/packages/flow-types/src/pages/design-panel/groovy-variable.ts`

**Step 1: Create the type definitions**

```typescript
/**
 * Groovy变量映射接口
 * 用于前后端变量映射统一
 */
export interface GroovyVariableMapping {
  /** 中文显示名称：如"当前操作人" */
  label: string;

  /** 变量展示名：如"request.operatorName" */
  value: string;

  /** Groovy表达式：如"request.getOperatorName()" */
  expression: string;

  /** 分组标签：如"操作人相关" */
  tag: string;

  /** 排序序号 */
  order: number;
}

/** 变量分组标签枚举 */
export const enum VariableTag {
  OPERATOR = '操作人相关',
  WORKFLOW = '流程相关',
  FORM_FIELD = '表单字段（当前表单）',
  WORK_CODE = '流程编号',
}

/** 标题表达式类型 */
export type TitleExpressionMode = 'normal' | 'advanced';
```

**Step 2: Export from flow-types index**

Modify: `frontend/packages/flow-types/src/pages/design-panel/index.ts`

```typescript
export * from './groovy-variable';
```

**Step 3: Build flow-types**

Run: `pnpm run build:flow-types`

**Step 4: Commit**

```bash
git add frontend/packages/flow-types/src/pages/design-panel/groovy-variable.ts
git add frontend/packages/flow-types/src/pages/design-panel/index.ts
git commit -m "feat(flow-types): add GroovyVariableMapping types"
```

---

### Task 5: Create predefined variable mappings service

**Files:**
- Create: `frontend/packages/flow-design/src/services/groovy-variable-service.ts`

**Step 1: Create the variable service**

```typescript
import { GroovyVariableMapping, VariableTag } from '@flow-engine/flow-types';

/**
 * Groovy变量预定义映射服务
 */
export class GroovyVariableService {
  /**
   * 获取预定义变量映射
   */
  static getPredefinedMappings(): GroovyVariableMapping[] {
    return [
      // ========== 操作人相关 ==========
      {
        label: '当前操作人',
        value: 'request.operatorName',
        expression: 'request.getOperatorName()',
        tag: VariableTag.OPERATOR,
        order: 1,
      },
      {
        label: '当前操作人ID',
        value: 'request.operatorId',
        expression: 'request.getOperatorId()',
        tag: VariableTag.OPERATOR,
        order: 2,
      },
      {
        label: '是否管理员',
        value: 'request.isFlowManager',
        expression: 'request.getIsFlowManager()',
        tag: VariableTag.OPERATOR,
        order: 3,
      },
      {
        label: '流程创建人',
        value: 'request.creatorName',
        expression: 'request.getCreatorName()',
        tag: VariableTag.OPERATOR,
        order: 4,
      },

      // ========== 流程相关 ==========
      {
        label: '流程标题',
        value: 'request.workflowTitle',
        expression: 'request.getWorkflowTitle()',
        tag: VariableTag.WORKFLOW,
        order: 10,
      },
      {
        label: '流程编码',
        value: 'request.workflowCode',
        expression: 'request.getWorkflowCode()',
        tag: VariableTag.WORKFLOW,
        order: 11,
      },
      {
        label: '当前节点',
        value: 'request.nodeName',
        expression: 'request.getNodeName()',
        tag: VariableTag.WORKFLOW,
        order: 12,
      },
      {
        label: '节点类型',
        value: 'request.nodeType',
        expression: 'request.getNodeType()',
        tag: VariableTag.WORKFLOW,
        order: 13,
      },

      // ========== 流程编号 ==========
      {
        label: '流程编号',
        value: 'request.workCode',
        expression: 'request.getWorkCode()',
        tag: VariableTag.WORK_CODE,
        order: 20,
      },
    ];
  }

  /**
   * 获取表单字段映射（动态生成）
   */
  static getFormFieldMappings(fields: Array<{ name: string; code: string }>): GroovyVariableMapping[] {
    return fields.map((field, index) => ({
      label: field.name,
      value: `request.formData("${field.code}")`,
      expression: `request.getFormData("${field.code}")`,
      tag: VariableTag.FORM_FIELD,
      order: 100 + index,
    }));
  }

  /**
   * 获取所有变量映射（预定义 + 表单字段）
   */
  static getAllMappings(formFields?: Array<{ name: string; code: string }>): GroovyVariableMapping[] {
    const mappings = [...this.getPredefinedMappings()];

    if (formFields && formFields.length > 0) {
      mappings.push(...this.getFormFieldMappings(formFields));
    }

    // 按tag和order排序
    return mappings.sort((a, b) => {
      if (a.tag !== b.tag) return a.tag.localeCompare(b.tag);
      return a.order - b.order;
    });
  }

  /**
   * 按tag分组变量映射
   */
  static groupByTag(mappings: GroovyVariableMapping[]): Map<string, GroovyVariableMapping[]> {
    const groups = new Map<string, GroovyVariableMapping[]>();

    for (const mapping of mappings) {
      const existing = groups.get(mapping.tag) || [];
      existing.push(mapping);
      groups.set(mapping.tag, existing);
    }

    return groups;
  }

  /**
   * 通过label查找映射
   */
  static findByLabel(label: string, mappings: GroovyVariableMapping[]): GroovyVariableMapping | undefined {
    return mappings.find(m => m.label === label);
  }

  /**
   * 检查是否为高级模式（无法解析回可视化）
   * 通过检测 // @TITLE 注释
   */
  static isAdvancedMode(script: string): boolean {
    return script.includes('// @TITLE');
  }
}
```

**Step 2: Export from services index**

Modify: `frontend/packages/flow-design/src/services/index.ts`

```typescript
export * from './groovy-variable-service';
```

**Step 3: Commit**

```bash
git add frontend/packages/flow-design/src/services/groovy-variable-service.ts
git add frontend/packages/flow-design/src/services/index.ts
git commit -m "feat(flow-design): add GroovyVariableService for variable mappings"
```

---

### Task 6: Create syntax conversion utilities

**Files:**
- Create: `frontend/packages/flow-design/src/utils/title-syntax-converter.ts`

**Step 1: Create the converter utility**

```typescript
import { GroovyVariableMapping } from '@flow-engine/flow-types';
import { GroovyVariableService } from '@/services/groovy-variable-service';

const TITLE_COMMENT = '// @TITLE';

/**
 * 标题表达式语法转换工具
 */
export class TitleSyntaxConverter {
  /**
   * 用户界面显示语法 → Groovy脚本语法
   * 输入: "你好，${当前操作人}"
   * 输出: "// @TITLE\nreturn \"你好，\" + request.getOperatorName()"
   */
  static toGroovySyntax(
    content: string,
    mappings: GroovyVariableMapping[]
  ): string {
    let result = content;

    // 按label长度降序排序，避免短label替换长label的一部分
    const sortedMappings = [...mappings].sort((a, b) => b.label.length - a.label.length);

    // 将 ${label} 替换为 " + expression + "
    for (const mapping of sortedMappings) {
      const labelPattern = `\${${mapping.label}}`;
      result = result.split(labelPattern).join(`" + ${mapping.expression} + "`);
    }

    // 清理多余的空字符串拼接: "" +  或  + ""
    result = result.replace(/"\s*\+\s*/g, '');
    result = result.replace(/\s*\+\s*"/g, '');

    // 添加 return 和引号
    result = `return "${result}"`;

    // 添加标题注释
    return `${TITLE_COMMENT}\n${result}`;
  }

  /**
   * Groovy脚本语法 → 用户界面显示语法
   * 输入: "// @TITLE\nreturn \"你好，\" + request.getOperatorName()"
   * 输出: "你好，${当前操作人}"
   */
  static toLabelExpression(
    groovyCode: string,
    mappings: GroovyVariableMapping[]
  ): string | null {
    try {
      let result = groovyCode.trim();

      // 检查是否有 @TITLE 注释
      if (!result.includes(TITLE_COMMENT)) {
        return null; // 无法解析
      }

      // 移除 @TITLE 注释
      result = result.replace(TITLE_COMMENT, '').trim();

      // 提取 return "..." 中的内容
      const returnMatch = result.match(/return\s+"([^"]*(?:\\"[^"]*)*)"/);
      if (!returnMatch) {
        return null; // 无法解析
      }

      result = returnMatch[1];

      // 替换转义字符
      result = result.replace(/\\"/g, '"');

      // 按expression长度降序排序
      const sortedMappings = [...mappings].sort(
        (a, b) => b.expression.length - a.expression.length
      );

      // 将 expression 替换为 ${label}
      for (const mapping of sortedMappings) {
        // 匹配 " + expression + " 或 expression + " 或 " + expression
        const patterns = [
          new RegExp(`"\\s*\\+\\s*${this.escapeRegex(mapping.expression)}\\s*\\+\\s*"`, 'g'),
          new RegExp(`${this.escapeRegex(mapping.expression)}\\s*\\+\\s*"`, 'g'),
          new RegExp(`"\\s*\\+\\s*${this.escapeRegex(mapping.expression)}`, 'g'),
        ];

        for (const pattern of patterns) {
          result = result.replace(pattern, `\${${mapping.label}}`);
        }
      }

      return result;
    } catch (e) {
      console.error('Failed to parse label expression:', e);
      return null;
    }
  }

  /**
   * 转义正则表达式特殊字符
   */
  private static escapeRegex(str: string): string {
    return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }

  /**
   * 解析脚本模式
   */
  static parseMode(script: string): 'normal' | 'advanced' {
    if (GroovyVariableService.isAdvancedMode(script)) {
      // 尝试解析为正常模式
      const mappings = GroovyVariableService.getPredefinedMappings();
      const labelExpr = this.toLabelExpression(script, mappings);
      return labelExpr !== null ? 'normal' : 'advanced';
    }
    // 旧脚本或无注释脚本，视为高级模式
    return 'advanced';
  }

  /**
   * 验证Groovy脚本语法（基础检查）
   */
  static validateGroovySyntax(script: string): { valid: boolean; error?: string } {
    if (!script || script.trim().length === 0) {
      return { valid: false, error: '脚本不能为空' };
    }

    // 基础语法检查
    if (!script.includes('return')) {
      return { valid: false, error: '脚本必须包含 return 语句' };
    }

    // 检查括号匹配
    const openBraces = (script.match(/\{/g) || []).length;
    const closeBraces = (script.match(/\}/g) || []).length;
    if (openBraces !== closeBraces) {
      return { valid: false, error: '大括号不匹配' };
    }

    const openParens = (script.match(/\(/g) || []).length;
    const closeParens = (script.match(/\)/g) || []).length;
    if (openParens !== closeParens) {
      return { valid: false, error: '圆括号不匹配' };
    }

    return { valid: true };
  }
}
```

**Step 2: Export from utils index**

Modify: `frontend/packages/flow-design/src/utils/index.ts`

```typescript
export * from './title-syntax-converter';
```

**Step 3: Commit**

```bash
git add frontend/packages/flow-design/src/utils/title-syntax-converter.ts
git add frontend/packages/flow-design/src/utils/index.ts
git commit -m "feat(flow-design): add TitleSyntaxConverter for expression conversion"
```

---

## Phase 3: UI Components

### Task 7: Create VariablePicker component

**Files:**
- Create: `frontend/packages/flow-design/src/components/design-editor/node-components/strategy/VariablePicker.tsx`
- Create: `frontend/packages/flow-design/src/components/design-editor/node-components/strategy/VariablePicker.module.less`

**Step 1: Write tests for VariablePicker**

```typescript
// VariablePicker.test.tsx
import { render, screen } from '@testing-library/react';
import { VariablePicker } from './VariablePicker';

describe('VariablePicker', () => {
  const mockMappings = [
    { label: '当前操作人', value: 'request.operatorName', expression: 'request.getOperatorName()', tag: '操作人相关', order: 1 },
    { label: '流程标题', value: 'request.workflowTitle', expression: 'request.getWorkflowTitle()', tag: '流程相关', order: 10 },
  ];

  it('should render variable groups', () => {
    render(<VariablePicker mappings={mockMappings} onSelect={jest.fn()} />);
    expect(screen.getByText('操作人相关')).toBeInTheDocument();
    expect(screen.getByText('流程相关')).toBeInTheDocument();
  });

  it('should call onSelect when variable is clicked', () => {
    const onSelect = jest.fn();
    render(<VariablePicker mappings={mockMappings} onSelect={onSelect} />);

    // Click on a variable
    // fireEvent.click(screen.getByText('当前操作人'));
    // expect(onSelect).toHaveBeenCalledWith(mockMappings[0]);
  });
});
```

**Step 2: Create VariablePicker component**

```typescript
// VariablePicker.tsx
import React, { useMemo, useState } from 'react';
import { Modal, Input, Empty } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { GroovyVariableMapping } from '@flow-engine/flow-types';
import { GroovyVariableService } from '@/services/groovy-variable-service';
import styles from './VariablePicker.module.less';

export interface VariablePickerProps {
  /** 变量映射列表 */
  mappings: GroovyVariableMapping[];
  /** 选中变量回调 */
  onSelect: (mapping: GroovyVariableMapping) => void;
  /** 是否显示 */
  visible: boolean;
  /** 关闭回调 */
  onClose: () => void;
}

/**
 * 变量选择器组件
 * 用于在标题表达式中插入变量
 */
export const VariablePicker: React.FC<VariablePickerProps> = ({
  mappings,
  onSelect,
  visible,
  onClose,
}) => {
  const [searchText, setSearchText] = useState('');

  // 过滤变量
  const filteredMappings = useMemo(() => {
    if (!searchText) {
      return mappings;
    }
    const lowerSearch = searchText.toLowerCase();
    return mappings.filter(
      m =>
        m.label.toLowerCase().includes(lowerSearch) ||
        m.value.toLowerCase().includes(lowerSearch)
    );
  }, [mappings, searchText]);

  // 按tag分组
  const groupedMappings = useMemo(() => {
    return GroovyVariableService.groupByTag(filteredMappings);
  }, [filteredMappings]);

  const handleVariableClick = (mapping: GroovyVariableMapping) => {
    onSelect(mapping);
    onClose();
    setSearchText('');
  };

  return (
    <Modal
      title="选择变量"
      open={visible}
      onCancel={onClose}
      footer={null}
      width={600}
      className={styles.variablePicker}
    >
      <div className={styles.searchWrapper}>
        <Input
          placeholder="搜索变量..."
          prefix={<SearchOutlined />}
          value={searchText}
          onChange={e => setSearchText(e.target.value)}
          allowClear
        />
      </div>

      <div className={styles.variableList}>
        {groupedMappings.size === 0 ? (
          <Empty description="未找到匹配的变量" />
        ) : (
          Array.from(groupedMappings.entries()).map(([tag, variables]) => (
            <div key={tag} className={styles.variableGroup}>
              <div className={styles.groupTitle}>{tag}</div>
              <div className={styles.variableItems}>
                {variables.map(variable => (
                  <div
                    key={variable.label}
                    className={styles.variableItem}
                    onClick={() => handleVariableClick(variable)}
                  >
                    <div className={styles.variableLabel}>{variable.label}</div>
                    <div className={styles.variableValue}>{variable.value}</div>
                  </div>
                ))}
              </div>
            </div>
          ))
        )}
      </div>
    </Modal>
  );
};
```

**Step 3: Create styles**

```less
// VariablePicker.module.less
.variablePicker {
  :global {
    .ant-modal-body {
      max-height: 500px;
      overflow-y: auto;
    }
  }
}

.searchWrapper {
  margin-bottom: 16px;
}

.variableList {
  max-height: 400px;
  overflow-y: auto;
}

.variableGroup {
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }
}

.groupTitle {
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
  margin-bottom: 8px;
  padding-left: 8px;
}

.variableItems {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.variableItem {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;

  &:hover {
    background-color: rgba(0, 0, 0, 0.04);
  }
}

.variableLabel {
  color: rgba(0, 0, 0, 0.85);
}

.variableValue {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
  font-family: 'Courier New', monospace;
}
```

**Step 3: Run tests**

```bash
cd frontend/packages/flow-design
pnpm run test VariablePicker.test.tsx
```

**Step 4: Commit**

```bash
git add frontend/packages/flow-design/src/components/design-editor/node-components/strategy/VariablePicker.tsx
git add frontend/packages/flow-design/src/components/design-editor/node-components/strategy/VariablePicker.test.tsx
git add frontend/packages/flow-design/src/components/design-editor/node-components/strategy/VariablePicker.module.less
git commit -m "feat(flow-design): add VariablePicker component"
```

---

### Task 8: Update NodeTitleStrategy component

**Files:**
- Modify: `frontend/packages/flow-pc/flow-pc-design/src/components/design-editor/node-components/strategy/node-title.tsx`
- Create: `frontend/packages/flow-pc/flow-pc-design/src/components/design-editor/node-components/strategy/TitleConfigModal.tsx`

**Step 1: Create TitleConfigModal component**

```typescript
// TitleConfigModal.tsx
import React, { useState, useEffect } from 'react';
import { Modal, Input, Alert, Button, Space, message } from 'antd';
import { EditOutlined, CodeOutlined, RollbackOutlined } from '@ant-design/icons';
import { GroovyVariableMapping } from '@flow-engine/flow-types';
import { GroovyVariableService } from '@flow-design/services';
import { TitleSyntaxConverter } from '@flow-design/utils';
import { VariablePicker } from './VariablePicker';

const { TextArea } = Input;

export interface TitleConfigModalProps {
  /** 当前脚本 */
  script: string;
  /** 表单字段（用于动态生成变量） */
  formFields?: Array<{ name: string; code: string }>;
  /** 确认回调 */
  onConfirm: (script: string) => void;
  /** 取消回调 */
  onCancel: () => void;
}

/**
 * 标题配置弹框
 * 支持普通模式和高级模式
 */
export const TitleConfigModal: React.FC<TitleConfigModalProps> = ({
  script,
  formFields,
  onConfirm,
  onCancel,
}) => {
  const [mode, setMode] = useState<'normal' | 'advanced'>('normal');
  const [content, setContent] = useState('');
  const [showVariablePicker, setShowVariablePicker] = useState(false);

  // 获取变量映射
  const mappings = GroovyVariableService.getAllMappings(formFields);

  useEffect(() => {
    // 解析当前脚本模式
    const parsedMode = TitleSyntaxConverter.parseMode(script);

    if (parsedMode === 'normal') {
      // 尝试解析为标签表达式
      const labelExpr = TitleSyntaxConverter.toLabelExpression(script, mappings);
      setContent(labelExpr || '');
    } else {
      // 高级模式，直接使用原脚本
      setContent(script);
    }

    setMode(parsedMode);
  }, [script, mappings]);

  // 插入变量
  const handleInsertVariable = (mapping: GroovyVariableMapping) => {
    setContent(prev => prev + `\${${mapping.label}}`);
  };

  // 切换到高级模式
  const handleSwitchToAdvanced = () => {
    if (mode === 'normal') {
      // 转换为Groovy脚本
      const groovyScript = TitleSyntaxConverter.toGroovySyntax(content, mappings);
      setContent(groovyScript);
    }
    setMode('advanced');
  };

  // 重置为普通模式
  const handleResetToNormal = () => {
    setContent('');
    setMode('normal');
  };

  // 确认
  const handleConfirm = () => {
    let finalScript = content;

    if (mode === 'normal') {
      if (!content.trim()) {
        message.error('请输入标题内容');
        return;
      }
      // 转换为Groovy脚本
      finalScript = TitleSyntaxConverter.toGroovySyntax(content, mappings);
    } else {
      // 验证Groovy语法
      const validation = TitleSyntaxConverter.validateGroovySyntax(content);
      if (!validation.valid) {
        message.error(`语法错误: ${validation.error}`);
        return;
      }
    }

    onConfirm(finalScript);
  };

  // 预览内容
  const renderPreview = () => {
    if (mode === 'normal') {
      return <div className={styles.preview}>{content || '（空）'}</div>;
    } else {
      // 尝试解析高级脚本
      const labelExpr = TitleSyntaxConverter.toLabelExpression(content, mappings);
      if (labelExpr !== null) {
        return <div className={styles.preview}>{labelExpr}</div>;
      }
      return (
        <Alert
          message="用户自定义配置，无法预览"
          type="warning"
          showIcon
          className={styles.previewWarning}
        />
      );
    }
  };

  return (
    <>
      <Modal
        title="标题配置"
        open={true}
        onCancel={onCancel}
        width={600}
        footer={
          <Space>
            <Button onClick={onCancel}>取消</Button>
            <Button type="primary" onClick={handleConfirm}>
              确定
            </Button>
          </Space>
        }
      >
        <div className={styles.container}>
          {/* 预览区 */}
          <div className={styles.section}>
            <div className={styles.sectionLabel}>预览</div>
            {renderPreview()}
          </div>

          {/* 操作按钮 */}
          <div className={styles.section}>
            {mode === 'normal' ? (
              <Space>
                <Button
                  icon={<EditOutlined />}
                  onClick={() => setShowVariablePicker(true)}
                >
                  插入变量
                </Button>
                <Button
                  icon={<CodeOutlined />}
                  onClick={handleSwitchToAdvanced}
                >
                  高级配置
                </Button>
              </Space>
            ) : (
              <Button
                icon={<RollbackOutlined />}
                onClick={handleResetToNormal}
              >
                重置
              </Button>
            )}
          </div>

          {/* 内容编辑区 */}
          <div className={styles.section}>
            <div className={styles.sectionLabel}>内容</div>
            {mode === 'normal' ? (
              <>
                <TextArea
                  value={content}
                  onChange={e => setContent(e.target.value)}
                  placeholder="点击上方按钮插入变量，或直接输入文字内容"
                  autoSize={{ minRows: 3, maxRows: 6 }}
                />
                <div className={styles.hint}>
                  示例：你好，${{当前操作人}}，有一笔${{请假天数}}元的审批
                </div>
              </>
            ) : (
              <TextArea
                value={content}
                onChange={e => setContent(e.target.value)}
                placeholder="// @TITLE&#10;return &quot;审批：&quot; + request.getOperatorName()"
                autoSize={{ minRows: 6, maxRows: 10 }}
                style={{ fontFamily: 'Courier New, monospace' }}
              />
            )}
          </div>
        </div>
      </Modal>

      <VariablePicker
        mappings={mappings}
        onSelect={handleInsertVariable}
        visible={showVariablePicker}
        onClose={() => setShowVariablePicker(false)}
      />
    </>
  );
};
```

**Step 2: Create styles**

```less
// title-config-modal.module.less
.container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sectionLabel {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.preview {
  padding: 12px;
  background-color: rgba(0, 0, 0, 0.02);
  border-radius: 4px;
  min-height: 40px;
  color: rgba(0, 0, 0, 0.65);
}

.previewWarning {
  margin: 0;
}

.hint {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.25);
}
```

**Step 3: Update node-title.tsx**

```typescript
// node-title.tsx
import React, { useState, useMemo } from 'react';
import { Form, Button, Space } from 'antd';
import { EditOutlined } from '@ant-design/icons';
import { Field, FieldRenderProps } from '@flowgram.ai/fixed-layout-editor';
import { GroovyVariableService } from '@flow-design/services';
import { TitleSyntaxConverter } from '@flow-design/utils';
import { TitleConfigModal } from './TitleConfigModal';

/**
 * 节点标题策略配置
 */
export const NodeTitleStrategy: React.FC = () => {
  const [form] = Form.useForm();
  const [showConfigModal, setShowConfigModal] = useState(false);
  const [currentScript, setCurrentScript] = useState('');

  // 获取表单字段（从context获取）
  const formFields = useMemo(() => {
    // TODO: 从 design context 获取当前流程的表单字段
    return [];
  }, []);

  // 获取变量映射
  const mappings = GroovyVariableService.getAllMappings(formFields);

  // 渲染预览内容
  const renderPreview = (script: string) => {
    if (!script) {
      return '（未配置）';
    }

    const mode = TitleSyntaxConverter.parseMode(script);
    if (mode === 'normal') {
      const labelExpr = TitleSyntaxConverter.toLabelExpression(script, mappings);
      return labelExpr || script;
    }

    // 尝试解析高级脚本
    const labelExpr = TitleSyntaxConverter.toLabelExpression(script, mappings);
    if (labelExpr !== null) {
      return labelExpr;
    }

    return '（自定义配置）';
  };

  const handleOpenConfig = () => {
    const script = form.getFieldValue(['NodeTitleStrategy', 'script']) || '';
    setCurrentScript(script);
    setShowConfigModal(true);
  };

  const handleConfirm = (script: string) => {
    form.setFieldValue(['NodeTitleStrategy', 'script'], script);
    setShowConfigModal(false);
  };

  return (
    <>
      <Form form={form} style={{ width: '100%' }} layout="vertical">
        <Form.Item label="节点标题" name="NodeTitleStrategy.script" initialValue="def run(request){return '你有一条待办'}">
          <Field
            name="NodeTitleStrategy.script"
            render={({ field: { value } }: FieldRenderProps<any>) => (
              <Space.Compact style={{ width: '100%' }}>
                <div
                  style={{
                    flex: 1,
                    padding: '4px 11px',
                    backgroundColor: value ? '#fff' : '#fafafa',
                    border: '1px solid #d9d9d9',
                    borderRadius: '6px 0 0 6px',
                    color: value ? 'rgba(0,0,0,0.88)' : 'rgba(0,0,0,0.25)',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                  }}
                >
                  {renderPreview(value)}
                </div>
                <Button
                  icon={<EditOutlined />}
                  onClick={handleOpenConfig}
                  style={{ borderRadius: '0 6px 6px 0' }}
                >
                  编辑
                </Button>
              </Space.Compact>
            )}
          />
        </Form.Item>
      </Form>

      {showConfigModal && (
        <TitleConfigModal
          script={currentScript}
          formFields={formFields}
          onConfirm={handleConfirm}
          onCancel={() => setShowConfigModal(false)}
        />
      )}
    </>
  );
};
```

**Step 4: Commit**

```bash
git add frontend/packages/flow-pc/flow-pc-design/src/components/design-editor/node-components/strategy/TitleConfigModal.tsx
git add frontend/packages/flow-pc/flow-pc-design/src/components/design-editor/node-components/strategy/TitleConfigModal.module.less
git add frontend/packages/flow-pc/flow-pc-design/src/components/design-editor/node-components/strategy/node-title.tsx
git commit -m "feat(flow-pc-design): update NodeTitleStrategy with interactive UI"
```

---

## Phase 4: Testing & Documentation

### Task 9: Write integration tests

**Files:**
- Create: `frontend/packages/flow-design/src/utils/__tests__/title-syntax-converter.test.ts`
- Test: `flow-engine-framework/src/test/java/com/codingapi/flow/integration/NodeTitleIntegrationTest.java`

**Step 1: Write frontend converter tests**

```typescript
import { GroovyVariableMapping } from '@flow-engine/flow-types';
import { TitleSyntaxConverter } from '../title-syntax-converter';

describe('TitleSyntaxConverter', () => {
  const mappings: GroovyVariableMapping[] = [
    { label: '当前操作人', value: 'request.operatorName', expression: 'request.getOperatorName()', tag: '操作人相关', order: 1 },
    { label: '请假天数', value: 'request.formData("days")', expression: 'request.getFormData("days")', tag: '表单字段', order: 100 },
  ];

  describe('toGroovySyntax', () => {
    it('should convert simple text', () => {
      const result = TitleSyntaxConverter.toGroovySyntax('你好', mappings);
      expect(result).toContain('return "你好"');
    });

    it('should convert single variable', () => {
      const result = TitleSyntaxConverter.toGroovySyntax('${当前操作人}', mappings);
      expect(result).toContain('request.getOperatorName()');
    });

    it('should convert multiple variables', () => {
      const result = TitleSyntaxConverter.toGroovySyntax('你好，${当前操作人}，请假${请假天数}天', mappings);
      expect(result).toContain('request.getOperatorName()');
      expect(result).toContain('request.getFormData("days")');
    });
  });

  describe('toLabelExpression', () => {
    it('should parse groovy to label', () => {
      const groovy = '// @TITLE\nreturn "你好，" + request.getOperatorName() + "，请审批"';
      const result = TitleSyntaxConverter.toLabelExpression(groovy, mappings);
      expect(result).toBe('你好，${当前操作人}，请审批');
    });

    it('should return null for non-title script', () => {
      const groovy = 'def run(request){return "custom"}';
      const result = TitleSyntaxConverter.toLabelExpression(groovy, mappings);
      expect(result).toBeNull();
    });
  });

  describe('parseMode', () => {
    it('should detect normal mode', () => {
      const script = '// @TITLE\nreturn "你好，" + request.getOperatorName()';
      const mode = TitleSyntaxConverter.parseMode(script);
      expect(mode).toBe('normal');
    });

    it('should detect advanced mode', () => {
      const script = 'def run(request){return "custom"}';
      const mode = TitleSyntaxConverter.parseMode(script);
      expect(mode).toBe('advanced');
    });
  });
});
```

**Step 2: Write backend integration test**

```java
package com.codingapi.flow.integration;

import com.codingapi.flow.script.node.NodeTitleScript;
import com.codingapi.flow.script.runtime.TitleGroovyRequest;
import com.codingapi.flow.session.FlowSession;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NodeTitleIntegrationTest {

    @Test
    void testTitleGenerationWithOperatorName() {
        NodeTitleScript script = new NodeTitleScript(
            "// @TITLE\nreturn \"审批人：\" + request.getOperatorName()"
        );

        TitleGroovyRequest request = new TitleGroovyRequest();
        request.setOperatorName("张三");

        String result = script.execute(request);
        assertEquals("审批人：张三", result);
    }

    @Test
    void testTitleGenerationWithFormData() {
        NodeTitleScript script = new NodeTitleScript(
            "// @TITLE\nreturn \"请假\" + request.getFormData(\"days\") + \"天\""
        );

        TitleGroovyRequest request = new TitleGroovyRequest();
        Map<String, Object> formData = new HashMap<>();
        formData.put("days", 5);
        request.setFormData(formData);

        String result = script.execute(request);
        assertEquals("请假5天", result);
    }
}
```

**Step 3: Run all tests**

```bash
# Backend
./mvnw test -pl flow-engine-framework

# Frontend
cd frontend/packages/flow-design
pnpm run test
```

**Step 4: Commit**

```bash
git add frontend/packages/flow-design/src/utils/__tests__/title-syntax-converter.test.ts
git add flow-engine-framework/src/test/java/com/codingapi/flow/integration/NodeTitleIntegrationTest.java
git commit -m "test: add integration tests for title expression"
```

---

### Task 10: Update documentation

**Files:**
- Modify: `CLAUDE.md`
- Modify: `frontend/CLAUDE.md`
- Update: `designs/title-expression-ui/README.md`

**Step 1: Update backend CLAUDE.md**

Add section about TitleGroovyRequest in Script Layer:

```markdown
### Script Layer

...

**TitleGroovyRequest**: Context object for title expression scripts, providing access to:
- Operator information (operatorName, operatorId, isFlowManager)
- Workflow information (workflowTitle, workflowCode, nodeName, nodeType)
- Creator information (creatorName)
- Form data (formData with getFormData(key) method)
- Work code (workCode)
```

**Step 2: Update frontend CLAUDE.md**

Add section about title expression UI:

```markdown
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
- `GroovyVariableService` - Variable mappings management
- `TitleSyntaxConverter` - Syntax conversion utilities
```

**Step 3: Mark design doc as implemented**

Update: `designs/title-expression-ui/README.md`

Add at top:

```markdown
> **Status**: ✅ Implemented (2025-02-26)
> **Plan**: `docs/plans/2025-02-26-title-expression-ui.md`
```

**Step 4: Commit**

```bash
git add CLAUDE.md frontend/CLAUDE.md designs/title-expression-ui/README.md
git commit -m "docs: update documentation for title expression feature"
```

---

## Phase 5: Build & Verification

### Task 11: Build and verify

**Step 1: Build backend**

```bash
./mvnw clean install -DskipTests
```

Expected: BUILD SUCCESS

**Step 2: Build frontend**

```bash
cd frontend
pnpm install
pnpm run build
```

Expected: All packages build successfully

**Step 3: Run application**

```bash
# Backend
cd flow-engine-example
./mvnw spring-boot:run

# Frontend (new terminal)
cd frontend
pnpm run dev:app-pc
```

**Step 4: Manual verification checklist**

- [ ] Open node properties panel
- [ ] Click "Edit" button on node title
- [ ] Verify variable picker shows predefined variables
- [ ] Insert a variable and verify preview updates
- [ ] Switch to advanced mode
- [ ] Write custom Groovy script
- [ ] Verify preview shows warning
- [ ] Reset to normal mode
- [ ] Save and verify title is persisted
- [ ] Test with workflow execution

**Step 5: Final commit**

```bash
git add .
git commit -m "feat: complete title expression UI implementation"
```

---

## Summary

This plan implements the title expression UI feature in 11 tasks:

1. **Backend Foundation (Tasks 1-3)**: Create TitleGroovyRequest, GroovyVariableMapping DTO, update NodeTitleScript
2. **Frontend Foundation (Tasks 4-6)**: Create types, variable service, syntax converter utilities
3. **UI Components (Tasks 7-8)**: Create VariablePicker and update NodeTitleStrategy component
4. **Testing (Task 9)**: Integration tests for both frontend and backend
5. **Documentation (Task 10)**: Update CLAUDE.md files
6. **Verification (Task 11)**: Build and manual verification

**Key Design Decisions:**
- **Frontend maintains variable mappings**: Faster, simpler, sufficient for design-time
- **Backend provides TitleGroovyRequest**: For runtime script execution
- **Two-mode UI**: Normal (visual) and Advanced (raw Groovy)
- **Syntax conversion**: Between `${label}` display and Groovy string concatenation
