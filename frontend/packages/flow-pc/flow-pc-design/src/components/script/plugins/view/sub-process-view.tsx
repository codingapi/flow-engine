import React from "react";
import {GroovyScriptConvertorUtil} from "@/components/script/utils/convertor";
import {GroovyScriptPreview} from "@/components/script/components/groovy-script-preview";
import {Button, Input, Space} from "antd";
import {CodeOutlined, ReloadOutlined} from "@ant-design/icons";
import {SubProcessViewPlugin, VIEW_KEY} from "@/components/script/plugins/sub-process-view-type";
import {ViewBindPlugin} from "@flow-engine/flow-types";
import {SCRIPT_DEFAULT_SUB_PROCESS} from "@/components/script/default-script";

const {TextArea} = Input;

/**
 * TODO 子流程配置界面
 * @param props
 * @constructor
 */
export const SubProcessPluginView: React.FC<SubProcessViewPlugin> = (props) => {
    const SubProcessPluginViewComponent = ViewBindPlugin.getInstance().get(VIEW_KEY);
    if(SubProcessPluginViewComponent){
        return (
            <SubProcessPluginViewComponent {...props} />
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
                        props.onChange(SCRIPT_DEFAULT_SUB_PROCESS);
                    }}
                >
                    重置脚本
                </Button>
            </Space>
        </div>
    );
}
