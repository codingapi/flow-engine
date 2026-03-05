import React from "react";
import {GroovyScriptPreview} from "@/components/script/components/groovy-script-preview";
import {ConditionRelation} from "@/components/script/components/condition/components/condition-relation";
import {ConditionGroup} from "@/components/script/components/condition/components/condition-group";
import {GroovyScriptConvertorUtil} from "@/components/script/utils/convertor";
import {SCRIPT_DEFAULT_CONDITION} from "@/components/script/default-script";
import {Button, Space} from "antd";
import {CodeOutlined, ReloadOutlined} from "@ant-design/icons";

interface ConditionPanelProps {
    script:string;
    onChange:(value:string)=>void;
}

export const ConditionPanel:React.FC<ConditionPanelProps> = (props)=>{

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
    )
}