import React from "react";
import {ActionCustomScriptUtils} from "@/components/script/services/action-custom";
import {GroovyCodeEditor} from "@/components/groovy-code";
import {Select} from "antd";
import {ConditionCustomViewPlugin, VIEW_KEY} from "@/components/script/plugins/action-custom-view-type";
import {ViewBindPlugin} from "@flow-engine/flow-core";


export const ConditionCustomView: React.FC<ConditionCustomViewPlugin> = (props) => {

    const ConditionCustomViewComponent = ViewBindPlugin.getInstance().get(VIEW_KEY);

    if (ConditionCustomViewComponent) {
        return (
            <ConditionCustomViewComponent {...props} />
        );
    }

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