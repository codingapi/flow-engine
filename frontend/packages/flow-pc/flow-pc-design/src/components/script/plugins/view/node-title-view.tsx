import React from "react";
import {NodeTitleGroovyConvertor} from "@/components/script/services/convertor/node-title";
import {GroovyScriptConvertorUtil} from "@/components/script/utils/convertor";
import {GroovyScriptPreview} from "@/components/script/components/groovy-script-preview";
import {VariablePicker} from "@/components/script/components/variable-picker";
import {Button, Input, Space} from "antd";
import {CodeOutlined, ReloadOutlined} from "@ant-design/icons";
import {NodeTitleViewPlugin, VIEW_KEY} from "@/components/script/plugins/node-title-view-type";
import {ViewBindPlugin} from "@flow-engine/flow-types";

const {TextArea} = Input;


export const NodeTitlePluginView: React.FC<NodeTitleViewPlugin> = (props) => {
    const nodeTitleGroovyConvertor = new NodeTitleGroovyConvertor(props.script, props.variables);
    const NodeTitlePluginViewComponent = ViewBindPlugin.getInstance().get(VIEW_KEY);
    if(NodeTitlePluginViewComponent){
        return (
            <NodeTitlePluginViewComponent {...props} />
        );
    }

    const title = GroovyScriptConvertorUtil.getScriptTitle(props.script);

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
                内容
                <TextArea
                    value={title}
                    onChange={(e) => {
                        props.onChange(nodeTitleGroovyConvertor.resetExpression(e.target.value));
                    }}
                    placeholder="请输入标题配置脚本，支持使用变量"
                    autoSize={{minRows: 3, maxRows: 12}}
                />
            </div>

            <Space
                style={{
                    marginTop: 8
                }}
            >
                <VariablePicker
                    mappings={props.variables}
                    onSelect={(variable) => {
                        props.onChange(nodeTitleGroovyConvertor.addVariable(variable));
                    }}
                />
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
                        props.onChange(nodeTitleGroovyConvertor.getDefaultScript());
                    }}
                >
                    重置脚本
                </Button>
            </Space>
        </div>
    );
}
