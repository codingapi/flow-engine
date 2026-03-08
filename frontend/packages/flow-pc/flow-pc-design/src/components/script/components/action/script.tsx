import React from "react";
import {GroovyCodeEditor} from "@/components/groovy-code";
import {Col, Form, FormInstance, Select, Space} from "antd";
import {GroovyScriptConvertorUtil} from "@flow-engine/flow-core";


interface CustomScriptViewProps{
    options: any[];
    form: FormInstance<any>;
}

export const CustomScriptView:React.FC<CustomScriptViewProps> = (props)=>{

    const trigger = props.form.getFieldValue("trigger") as string;

    const [currentTrigger,setCurrentTrigger] = React.useState(trigger);

    const handleChangeNodeType = (value: string) => {
        const script = props.form.getFieldValue('script') as string;
        let groovy;
        if (script) {
            const returnData = GroovyScriptConvertorUtil.getReturnScript(script).trim();
            groovy = script.replace(returnData, `'${value}'`);
            groovy = GroovyScriptConvertorUtil.updateScriptMeta(groovy,`{"trigger":"${value}"}`);
        } else {
            groovy = `// @CUSTOM_SCRIPT 自定义脚本，返回的数据为动作类型
            // @SCRIPT_META {"trigger":"${value}"}
            def run(request){
                return '${value}';
            }
            `
        }
        props.form.setFieldValue("script", GroovyScriptConvertorUtil.formatScript(groovy));
        setCurrentTrigger(value);
    }

    return (
        <Col span={24}>
            <Form.Item
                name={"script"}
                label={(
                    <Space>
                        自定义脚本
                        <Space.Compact size={"small"}>
                            <Space.Addon>触发动作：</Space.Addon>
                            <Select
                                style={{
                                    width: '100px'
                                }}
                                value={currentTrigger}
                                placeholder={"请选择触发动作类型"}
                                options={props.options}
                                onChange={handleChangeNodeType}
                            />
                        </Space.Compact>


                    </Space>
                )}
                required={true}
                help={"请先设置触发动作类型"}

                rules={[
                    {
                        required: true,
                        message: '自定义脚本不能为空'
                    }
                ]}
            >
                <GroovyCodeEditor
                    placeholder={"请输入自定义脚本"}
                    options={{
                        minHeight: 200
                    }}
                />
            </Form.Item>
        </Col>
    )
}