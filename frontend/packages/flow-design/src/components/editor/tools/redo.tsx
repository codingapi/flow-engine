import { RedoOutlined } from "@ant-design/icons";
import {usePlayground, usePlaygroundTools } from "@flowgram.ai/fixed-layout-editor";
import {Button, Tooltip } from "antd";
import React from "react";


export const Redo = ()=>{

    const tools = usePlaygroundTools();
    const playground = usePlayground();

    return (
        <Tooltip title="Redo">
            <Button
                type="text"
                icon={<RedoOutlined />}
                disabled={!tools.canRedo || playground.config.readonly}
                onClick={() => tools.redo()}
            />
        </Tooltip>
    )
}