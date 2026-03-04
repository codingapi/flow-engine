import React from "react";
import {ConditionViewPlugin, VIEW_KEY} from "@/components/script/plugins/condition-view-type";
import {ViewBindPlugin} from "@flow-engine/flow-types";
import {SCRIPT_DEFAULT_CONDITION} from "@/components/script/default-script";
import {GroovyScriptConvertorUtil} from "@/components/script/utils/convertor";
import {Button, Space} from "antd";
import {CodeOutlined, ReloadOutlined} from "@ant-design/icons";
import {GroovyScriptPreview} from "@/components/script/components/groovy-script-preview";
import {ConditionRelation} from "@/components/script/components/condition/condition-relation";
import {ConditionGroup} from "@/components/script/components/condition/condition-group";

/**
 * TODO 条件控制界面
 * @param props
 * @constructor
 */
export const ConditionPluginView: React.FC<ConditionViewPlugin> = (props) => {
    const ConditionPluginViewComponent = ViewBindPlugin.getInstance().get(VIEW_KEY);
    if(ConditionPluginViewComponent){
        return (
            <ConditionPluginViewComponent {...props} />
        );
    }

    return (
       <div>
           <div>
               预览
               <GroovyScriptPreview
                   script={props.script}
                   multiline={true}
               />
           </div>
           <div>
               关系
               <ConditionRelation/>
           </div>
           <div>
               条件
               <ConditionGroup/>
           </div>
           <Space
               style={{
                   marginTop: 8
               }}
           >
               <Button
                   icon={<CodeOutlined/>}
                   onClick={() => {
                       props.onChange(GroovyScriptConvertorUtil.toCustomScript(props.script));
                   }}
               >
                   高级配置
               </Button>
               <Button
                   icon={<ReloadOutlined/>}
                   danger={true}
                   onClick={() => {
                       props.onChange(SCRIPT_DEFAULT_CONDITION);
                   }}
               >
                   重置脚本
               </Button>
           </Space>
       </div>
    );
}
