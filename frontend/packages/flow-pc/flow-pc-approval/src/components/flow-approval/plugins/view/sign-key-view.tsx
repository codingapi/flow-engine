import React from "react";
import {SignKeyViewPlugin, VIEW_KEY} from "../sign-key-type";
import {ViewBindPlugin} from "@flow-engine/flow-types";
import {Input} from "antd";

const {TextArea} = Input;

export const SignKeyView: React.FC<SignKeyViewPlugin> = (props) => {
    const SignKeyViewComponent = ViewBindPlugin.getInstance().get(VIEW_KEY);

    if (SignKeyViewComponent) {
        return (
            <SignKeyViewComponent {...props} />
        );
    }
    return (
        <TextArea
            value={props.value}
            placeholder={"请输入审批签名"}
            onChange={(event) => {
                const value = event.target.value;
                props.onChange?.(value);
            }}
        />
    )
}