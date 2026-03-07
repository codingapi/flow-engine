import React from "react";
import {RelationPanel} from "@/components/script/components/condition/components/condition-relation";
import {Group} from "@/components/script/components/condition/components/condition-group";
import {GroovyScriptConvertorUtil} from "@flow-engine/flow-core";
import {SCRIPT_DEFAULT_CONDITION} from "@/components/script/default-script";
import {Button, Space} from "antd";
import {CodeOutlined, ReloadOutlined} from "@ant-design/icons";
import {useConditionContext} from "@/components/script/components/condition/hooks/use-condition-context";
import {ConditionPanelProps} from "@/components/script/components/condition/typings";
import {
    useConditionScriptConvertor
} from "@/components/script/components/condition/hooks/use-condition-script-convertor";


export const ConditionPanel: React.FC<ConditionPanelProps> = (props) => {
    const {context} = useConditionContext();
    const presenter = context.getPresenter();
    useConditionScriptConvertor(props);

    return (
        <div>
            <div>
                关系
                <RelationPanel/>
            </div>
            <div>
                条件
                <Group/>
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
                        presenter.clearState();
                        props.onChange(SCRIPT_DEFAULT_CONDITION);
                    }}
                >
                    重置脚本
                </Button>
            </Space>
        </div>
    )
}