import {Button} from "antd";
import React from "react";
import {DisplayStyle, FlowActionDisplay} from "@flow-engine/flow-types";


interface ActionButtonProps {
    onClick: () => void;
    title: string;
    display: FlowActionDisplay;
}

/**
 *  展示样式
 */
export interface DisplayStyle {
    // 边框颜色
    borderColor?: string;
    // 背景颜色
    backgroundColor?: string;
    // 边框大小
    borderSize?: string;
    // 边框圆角
    borderRadius?: string;
}

export const ActionButton: React.FC<ActionButtonProps> = (props) => {

    const display = props.display;
    const title = display.title || props.title;

    const style = React.useMemo(() => {
        if (display) {
            const data = JSON.parse(display.style) as DisplayStyle | undefined;
            if (data) {
                let style = {}
                if (data.backgroundColor) {
                    style = Object.assign(data, {backgroundColor: `#${data.backgroundColor}`});
                }
                if (data.borderColor) {
                    style = Object.assign(data, {borderColor: `#${data.borderColor}`});
                }
                if (data.borderRadius) {
                    style = Object.assign(data, {borderRadius: Number.parseInt(data.borderRadius)});
                }
                if (data.borderSize) {
                    style = Object.assign(data, {borderWidth: Number.parseInt(data.borderSize)});
                }
                return style;
            }
        }
        return {}
    }, [display]);


    return (
        <Button
            onClick={props.onClick}
            style={style}
        >
            {title}
        </Button>
    )
}