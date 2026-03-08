# 界面拓展机制

支持流程组件对各类业务层面内容的界面拓展机制。

## 实现方式

```
import {ViewBindPlugin} from "@flow-engine/flow-core";

// 界面视图
const MyView:React.FC<FormViewProps> = (props)=>{
    return (
        <></>
    )
}

// 注册，关键信息为key 和 界面ComponentType,传递的属性根据不同的界面对应查看
ViewBindPlugin.getInstance().register('MyView',MyView)
```

## 拓展界面

### 流程审核

* 表单渲染
```
export interface FormViewProps {
    /** 表单操控对象 */
    form: FormInstance;
    /** 表单数据更新事件 */
    onValuesChange?: (values: any) => void;
    /** 表单元数据对象 */
    meta: FlowForm;
    /** 是否预览模式 */
    review:boolean;
}

```
表单选择的key对应流程节点设置的view名称，流程引擎对default进行了模型的渲染支持。

* 流程操作 加签

```
export const VIEW_KEY = 'AddAuditViewPlugin';

export interface AddAuditViewPlugin {
    /** 返回用户 */
    onChange?: (value: string|string[]) => void;
    /** 当前用户 */
    value?: string|string[];
}
```
* 流程操作 委派
```
export const VIEW_KEY = 'DelegateViewPlugin';

export interface DelegateViewPlugin {
    /** 返回用户 */
    onChange?: (value: string|string[]) => void;
    /** 当前用户 */
    value?: string|string[];
}
```

* 流程操作 退回流程
```
export const VIEW_KEY = 'ReturnViewPlugin';

export interface ReturnViewPlugin {
    /** 返回用户 */
    onChange?: (value: string|string[]) => void;
    /** 当前用户 */
    value?: string|string[];
}
```

* 流程操作 提交时的获取签名界面
```
import {FlowOperator} from "@flow-engine/flow-types";

export const VIEW_KEY = 'SignKeyViewPlugin';

export interface SignKeyViewPlugin {
    /** 当前用户 */
    current: FlowOperator;
    /** 返回签名 */
    onChange?: (value: string) => void;
    /** 当前签名 */
    value?: string;
}
```

* 流程操作 转办操作
```
export const VIEW_KEY = 'TransferViewPlugin';

export interface TransferViewPlugin {
    /** 返回用户 */
    onChange?: (value: string|string[]) => void;
    /** 当前用户 */
    value?: string|string[];
}
```

### 流程设计-节点配置

* 流程条件控制界面
```
import {GroovyVariableMapping, ScriptType} from "@/components/script/typings";

export const VIEW_KEY = 'ConditionViewPlugin';

export interface ConditionViewPlugin {
    /** 脚本类型 */
    type: ScriptType;
    /** 当前脚本 */
    script: string;
    /** 变量映射列表 */
    variables: GroovyVariableMapping[];
    /** 确认回调 */
    onChange: (script: string) => void;
}
```

* 异常处理逻辑界面
```
import {GroovyVariableMapping, ScriptType} from "@/components/script/typings";

export const VIEW_KEY = 'ErrorTriggerViewPlugin';

export interface ErrorTriggerViewPlugin {
    /** 脚本类型 */
    type: ScriptType;
    /** 当前脚本 */
    script: string;
    /** 变量映射列表 */
    variables: GroovyVariableMapping[];
    /** 确认回调 */
    onChange: (script: string) => void;
}
```

* 自定义标题界面
```
import {GroovyVariableMapping, ScriptType} from "@/components/script/typings";

export const VIEW_KEY = 'NodeTitleViewPlugin';

export interface NodeTitleViewPlugin {
    /** 脚本类型 */
    type: ScriptType;
    /** 当前脚本 */
    script: string;
    /** 变量映射列表 */
    variables: GroovyVariableMapping[];
    /** 确认回调 */
    onChange: (script: string) => void;
}
```

* 设置发起人范围界面

```
export const VIEW_KEY = 'OperatorCreateViewPlugin';

export interface OperatorCreateViewPlugin {
    /** 当前脚本 */
    script: string;
    /** 确认回调 */
    onChange: (script: string) => void;
}
```

* 节点人员选择界面

```
export const VIEW_KEY = 'OperatorLoadViewPlugin';

export interface OperatorLoadViewPlugin {
    /** 当前脚本 */
    script: string;
    /** 确认回调 */
    onChange: (script: string) => void;
}
```
* 路由配置界面

```
import {GroovyVariableMapping, ScriptType} from "@/components/script/typings";

export const VIEW_KEY = 'RouterViewPlugin';

export interface RouterViewPlugin {
    /** 脚本类型 */
    type: ScriptType;
    /** 当前脚本 */
    script: string;
    /** 变量映射列表 */
    variables: GroovyVariableMapping[];
    /** 确认回调 */
    onChange: (script: string) => void;
}
```
* 子流程配置界面
```
import {GroovyVariableMapping, ScriptType} from "@/components/script/typings";

export const VIEW_KEY = 'SubProcessViewPlugin';

export interface SubProcessViewPlugin {
    /** 脚本类型 */
    type: ScriptType;
    /** 当前脚本 */
    script: string;
    /** 变量映射列表 */
    variables: GroovyVariableMapping[];
    /** 确认回调 */
    onChange: (script: string) => void;
}
```
* 触发流程界面
```
import {GroovyVariableMapping, ScriptType} from "@/components/script/typings";

export const VIEW_KEY = 'TriggerViewPlugin';

export interface TriggerViewPlugin {
    /** 脚本类型 */
    type: ScriptType;
    /** 当前脚本 */
    script: string;
    /** 变量映射列表 */
    variables: GroovyVariableMapping[];
    /** 确认回调 */
    onChange: (script: string) => void;
}
```

### 流程设计-动作配置

* 自定义按钮触发脚本界面
```
import {ActionSelectOption} from "@/components/script/typings";

export const VIEW_KEY = 'ConditionCustomViewPlugin';

export interface ConditionCustomViewPlugin {
    // 当前的脚本
    value?: string;
    // 脚本更改回掉
    onChange?: (value: string) => void;
    // 可选择的动作范围
    options:ActionSelectOption[];
}
```

* 拒绝动作界面
```
export const VIEW_KEY = 'ConditionRejectViewPlugin';

export interface ConditionRejectViewPlugin {
    // 当前节点id
    nodeId:string;
    // 当前的脚本
    value?: string;
    // 脚本更改回掉
    onChange?: (value: string) => void;
}
```

* 委派/转办/加签/界面 与（节点人员选择界面一致）
```
export const VIEW_KEY = 'OperatorLoadViewPlugin';

export interface OperatorLoadViewPlugin {
    /** 当前脚本 */
    script: string;
    /** 确认回调 */
    onChange: (script: string) => void;
}
```