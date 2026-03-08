import React from "react";
import {ActionFormProps, ActionSelectOption} from "@/components/script/typings";
import {Col, Form, Row, Select} from "antd";
import {GroovyCodeEditor} from "@/components/groovy-code";
import {ActionCustomScriptUtils} from "@/components/script/services/action-custom";

interface CustomScriptProps {
    value?: string;
    onChange?: (value: string) => void;
    options:ActionSelectOption[];
}

const CustomScript: React.FC<CustomScriptProps> = (props) => {

    const trigger = React.useMemo(() => {
        if (props.value) {
            return ActionCustomScriptUtils.getTrigger(props.value);
        }
        return undefined;
    }, [props.value]);

    const handleChangeNodeType = (value: string) => {
        const script = props.value;
        if (script) {
            const groovy = ActionCustomScriptUtils.update(value, script);
            props.onChange?.(groovy);
        }
    }

    return (
        <div
            style={{
                marginTop: "8px",
                padding: "8px",
            }}
        >
            <div style={{
                display: "flex",
                justifyContent: "start",
                alignItems: "center",
                marginBottom: "8px",
            }}>
                <span>触发动作:</span>
                <Select
                    size={"small"}
                    style={{
                        width: '200px',
                        marginLeft: "10px",
                    }}
                    value={trigger}
                    placeholder={"请选择触发动作类型"}
                    onChange={handleChangeNodeType}
                    options={[
                        {
                            label: '通过',
                            value: 'pass'
                        },
                        {
                            label: '拒绝',
                            value: 'reject'
                        }
                    ]}
                />
            </div>

            <GroovyCodeEditor
                value={props.value}
                onChange={props.onChange}
                placeholder={"请输入自定义脚本"}
                options={{
                    minHeight: 200
                }}
            />
        </div>
    )
}

export const CustomActionForm: React.FC<ActionFormProps> = (props) => {

    const actionOptionTypes = props.manager.getCurrentNodeActionOptions();

    return (
        <Row>
            <Col span={24}>
                <Form.Item
                    name={"script"}
                    label={"自定义脚本"}
                    required={true}
                    help={"请先设置触发动作类型"}
                    rules={[
                        {
                            required: true,
                            message: '自定义脚本不能为空'
                        }
                    ]}
                >
                    <CustomScript
                        options={actionOptionTypes}
                    />
                </Form.Item>
            </Col>
        </Row>
    )
}