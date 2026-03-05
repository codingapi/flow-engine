import React from "react";
import {RouterViewPlugin, VIEW_KEY} from "@/components/script/plugins/router-view-type";
import {ViewBindPlugin} from "@flow-engine/flow-types";
import {SCRIPT_DEFAULT_ROUTER} from "@/components/script/default-script";
import {GroovyScriptConvertorUtil} from "@/components/script/utils/convertor";
import {Button, Form, Select, Space} from "antd";
import {CodeOutlined, ReloadOutlined} from "@ant-design/icons";
import {useNodeRouterManager} from "@/components/design-panel/hooks/use-node-router-manager";
import {RouterScriptUtils} from "@/components/script/services/utils/router";

/**
 * @param props
 * @constructor
 */
export const RouterPluginView: React.FC<RouterViewPlugin> = (props) => {
    const TriggerPluginViewComponent = ViewBindPlugin.getInstance().get(VIEW_KEY);
    const nodeRouterManager = useNodeRouterManager();

    if (TriggerPluginViewComponent) {
        return (
            <TriggerPluginViewComponent {...props} />
        );
    }

    return (
        <div>
            <Form>
                <Form.Item
                    name={"node"}
                    label={"跳转节点"}
                >
                    <Select
                        options={nodeRouterManager.getNodes()}
                        onChange={(value, option) => {
                            const script = RouterScriptUtils.goNode(option as any);
                            props.onChange(script);
                        }}
                    />
                </Form.Item>
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
                        props.onChange(SCRIPT_DEFAULT_ROUTER);
                    }}
                >
                    重置脚本
                </Button>
            </Space>
        </div>
    );
}
