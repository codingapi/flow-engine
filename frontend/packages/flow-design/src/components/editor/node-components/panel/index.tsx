import React from "react";
import {Flex} from "antd";

interface NodePanelProps {
    children?: React.ReactNode;
}

export const NodePanel: React.FC<NodePanelProps> = (props) => {
    return (
        <Flex
            style={{
                width: '100%',
                padding:3
            }}
            vertical={true}
            justify="center"
            align="center"
        >
            {props.children}
        </Flex>
    )
}