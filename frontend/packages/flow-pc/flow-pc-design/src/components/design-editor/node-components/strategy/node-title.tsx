import React from 'react';
import {Button, Form, Space} from 'antd';
import {EditOutlined} from '@ant-design/icons';
import {Field, FieldRenderProps} from '@flowgram.ai/fixed-layout-editor';
import {GroovyScriptPreview} from "@/components/script/components/groovy-script-preview";
import {NodeTitleConfigModal} from "@/components/script/modal/node-title-config-modal";
import {useScriptVariables} from "@/components/design-editor/hooks/use-script-variables";

/**
 * 节点标题策略配置
 */
export const NodeTitleStrategy: React.FC = () => {
    const [showConfigModal, setShowConfigModal] = React.useState(false);
    const scriptVariables = useScriptVariables();

    return (
        <>
            <Form style={{width: '100%'}} layout="vertical">
                <Form.Item label="节点标题">
                    <Field
                        name="NodeTitleStrategy.script"
                        render={(props: FieldRenderProps<any>) => {
                            const {value, onChange} = props.field;
                            return (
                                <Space.Compact style={{width: '100%'}}>
                                    <GroovyScriptPreview
                                        script={value}
                                    />
                                    <Button
                                        icon={<EditOutlined/>}
                                        onClick={() => {
                                            setShowConfigModal(true);
                                        }}
                                        style={{borderRadius: '0 6px 6px 0'}}
                                    >
                                        编辑
                                    </Button>

                                    <NodeTitleConfigModal
                                        open={showConfigModal}
                                        script={value}
                                        variables={scriptVariables}
                                        onCancel={() => setShowConfigModal(false)}
                                        onConfirm={(script) => {
                                            onChange(script);
                                        }}
                                    />
                                </Space.Compact>
                            );
                        }}
                    />
                </Form.Item>
            </Form>
        </>
    );
};
