import React from "react";
import {GroovyScriptConvertorUtil} from "@/components/script/utils/convertor";
import {Button, Form, Select, Space} from "antd";
import {CodeOutlined, ReloadOutlined} from "@ant-design/icons";
import {ErrorTriggerViewPlugin, VIEW_KEY} from "@/components/script/plugins/error-trigger-view-type";
import {ViewBindPlugin} from "@flow-engine/flow-types";
import {DEFAULT_ERROR_TRIGGER_SCRIPT} from "@/components/script/default-script";
import {useNodeRouterManager} from "@/components/design-panel/hooks/use-node-router-manager";
import {useNodeRenderContext} from "@/components/design-editor/hooks/use-node-render-context";
import {ErrorTriggerConvertor} from "@/components/script/services/convertor/error-trigger";


/**
 * TODO 异常配置界面
 * @param props
 * @constructor
 */
export const ErrorTriggerPluginView: React.FC<ErrorTriggerViewPlugin> = (props) => {
    const ErrorTriggerPluginViewComponent = ViewBindPlugin.getInstance().get(VIEW_KEY);
    const [type,setType] = React.useState('node');

    const nodeRouterManager = useNodeRouterManager();
    const { node } = useNodeRenderContext();

    if(ErrorTriggerPluginViewComponent){
        return (
            <ErrorTriggerPluginViewComponent {...props} />
        );
    }

    return (
        <div>
            <Form
                initialValues={{
                    type: type,
                }}
            >
                <Form.Item
                    name={"type"}
                    label={"触发类型"}
                >
                    <Select
                        options={[
                            {
                                label:'跳转节点',
                                value:'node'
                            },
                            {
                                label:'跳转用户',
                                value:'user'
                            }
                        ]}
                        onChange={(value) => {
                            setType(value);
                        }}
                    />

                </Form.Item>

                {type === "node" && (
                    <Form.Item
                        name={"node"}
                        label={"指定节点"}
                    >
                        <Select
                            options={nodeRouterManager.getBackNodes(node.id)}
                            onChange={(value,option) => {
                                const script = ErrorTriggerConvertor.goNode(option as any);
                                props.onChange(script);
                            }}
                        />
                    </Form.Item>
                )}

                {type === "user" && (
                    <div>选择人员</div>
                )}
            </Form>

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
                        props.onChange(DEFAULT_ERROR_TRIGGER_SCRIPT);
                    }}
                >
                    重置脚本
                </Button>
            </Space>
        </div>
    );
}
