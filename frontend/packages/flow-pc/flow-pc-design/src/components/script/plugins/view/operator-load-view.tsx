import React from "react";
import {OperatorLoadViewPlugin, VIEW_KEY} from "@/components/script/plugins/operator-load-view-type";
import {Button, Form, Select, Space} from "antd";
import {ViewBindPlugin} from "@flow-engine/flow-types";
import {SCRIPT_DEFAULT_OPERATOR_LOAD} from "@/components/script/default-script";
import {GroovyScriptConvertorUtil} from "@/components/script/utils/convertor";
import {CodeOutlined, ReloadOutlined} from "@ant-design/icons";

export const OperatorLoadPluginView: React.FC<OperatorLoadViewPlugin> = (props) => {

    const OperatorLoadPluginViewComponent = ViewBindPlugin.getInstance().get(VIEW_KEY);

    if (OperatorLoadPluginViewComponent) {
        return (
            <OperatorLoadPluginViewComponent {...props} />
        );
    }

    return (
        <Form>
            <Form.Item
                name={"type"}
                label={"类型"}
                tooltip={"选择人员类型"}
            >
                <Select
                    options={[
                        {
                            label: '流程创建者',
                            value: '1',
                        },
                    ]}
                    placeholder={"请选择人员类型"}
                    onChange={(value) => {
                        props.onChange(SCRIPT_DEFAULT_OPERATOR_LOAD);
                    }}
                />
            </Form.Item>
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
                        props.onChange(SCRIPT_DEFAULT_OPERATOR_LOAD);
                    }}
                >
                    重置脚本
                </Button>
            </Space>
        </Form>
    )
}