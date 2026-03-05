import React from "react";
import {OperatorCreateViewPlugin, VIEW_KEY} from "@/components/script/plugins/operator-create-view-type";
import {Button, Form, Select, Space} from "antd";
import {ViewBindPlugin} from "@flow-engine/flow-types";
import {SCRIPT_DEFAULT_OPERATOR_CREATE, SCRIPT_DEFAULT_OPERATOR_LOAD} from "@/components/script/default-script";
import {GroovyScriptConvertorUtil} from "@/components/script/utils/convertor";
import {CodeOutlined, ReloadOutlined} from "@ant-design/icons";
import {useScriptMetaData} from "@/components/script/hooks/use-script-meta-data";

export const OperatorCreatePluginView: React.FC<OperatorCreateViewPlugin> = (props) => {

    const OperatorCreatePluginViewComponent = ViewBindPlugin.getInstance().get(VIEW_KEY);
    const data = useScriptMetaData(props.script);

    if (OperatorCreatePluginViewComponent) {
        return (
            <OperatorCreatePluginViewComponent {...props} />
        );
    }

    return (
        <Form
            initialValues={{
                ...data
            }}
        >
            <Form.Item
                name={"type"}
                label={"类型"}
                tooltip={"选择人员类型"}
            >
                <Select
                    options={[
                        {
                            label: '任意用户',
                            value: 'any',
                        }
                    ]}
                    placeholder={"请选择人员类型"}
                    onChange={(value) => {
                        props.onChange(SCRIPT_DEFAULT_OPERATOR_CREATE);
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