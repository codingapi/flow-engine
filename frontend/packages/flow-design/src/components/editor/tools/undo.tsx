import { UndoOutlined } from "@ant-design/icons";
import {usePlayground, usePlaygroundTools } from "@flowgram.ai/fixed-layout-editor";
import {Button, Tooltip } from "antd";
import React from "react";


export const Undo = ()=>{

    const tools = usePlaygroundTools();
    const playground = usePlayground();

    return (
        <Tooltip title="Undo">
            <Button
                icon={<UndoOutlined />}
                disabled={!tools.canUndo || playground.config.readonly}
                onClick={() => tools.undo()}
            />
        </Tooltip>
    )
}