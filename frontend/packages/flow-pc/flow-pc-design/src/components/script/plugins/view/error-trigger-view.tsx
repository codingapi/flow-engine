import React from "react";
import {GroovyScriptConvertorUtil} from "@flow-engine/flow-core";
import {Button, Form, Select, Space} from "antd";
import {CodeOutlined, ReloadOutlined} from "@ant-design/icons";
import {ErrorTriggerViewPlugin, VIEW_KEY} from "@/components/script/plugins/error-trigger-view-type";
import {ViewBindPlugin} from "@flow-engine/flow-core";
import {SCRIPT_DEFAULT_ERROR_TRIGGER} from "@/components/script/default-script";
import {useNodeRouterManager} from "@/components/design-panel/hooks/use-node-router-manager";
import {useNodeRenderContext} from "@/components/design-editor/hooks/use-node-render-context";
import {ErrorTriggerScriptUtils} from "@/components/script/services/error-trigger";
import {useScriptMetaData} from "@/components/script/hooks/use-script-meta-data";

/**
 *
 * @param props
 * @constructor
 */
export const ErrorTriggerPluginView: React.FC<ErrorTriggerViewPlugin> = (props) => {
    const ErrorTriggerPluginViewComponent = ViewBindPlugin.getInstance().get(VIEW_KEY);
    const [type, setType] = React.useState('node');

    const nodeRouterManager = useNodeRouterManager();
    const {node} = useNodeRenderContext();
    const data = useScriptMetaData(props.script);

    const mappingData = nodeRouterManager.mapping(data);

    if (ErrorTriggerPluginViewComponent) {
        return (
            <ErrorTriggerPluginViewComponent {...props} />
        );
    }

    return (
        <div>
            <Form
                initialValues={{
                    ...mappingData
                }}
            >
                <Form.Item
                    name={"type"}
                    label={"触发类型"}
                >
                    <Select
                        options={[
                            {
                                label: '跳转节点',
                                value: 'node'
                            },
                            {
                                label: '跳转用户',
                                value: 'user'
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
                            onChange={(value, option) => {
                                const script = ErrorTriggerScriptUtils.setNode(option as any);
                                props.onChange(script);
                            }}
                        />
                    </Form.Item>
                )}

                {type === "user" && (
                    <div>选择人员:暂不支持</div>
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
                        props.onChange(SCRIPT_DEFAULT_ERROR_TRIGGER);
                    }}
                >
                    重置脚本
                </Button>
            </Space>
        </div>
    );
}
