import React from "react";
import { UndoOutlined } from "@ant-design/icons";
import {usePlayground, usePlaygroundTools } from "@flowgram.ai/fixed-layout-editor";
import {Button, Tooltip } from "antd";

export const Undo = ()=>{

    const tools = usePlaygroundTools();
    const playground = usePlayground();

    return (
        <Tooltip title="Undo">
            <Button
                type="text"
                icon={<UndoOutlined />}
                disabled={!tools.canUndo || playground.config.readonly}
                onClick={() => tools.undo()}
            />
        </Tooltip>
    )
}