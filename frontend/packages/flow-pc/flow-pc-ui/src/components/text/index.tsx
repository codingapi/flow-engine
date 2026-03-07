import React from "react";
import {Typography} from "antd";

interface TextProps {
    suffixCount: number;
    children: string;
}

export const Text: React.FC<TextProps> = (props) => {
    const {children, suffixCount} = props;
    const start = children.slice(0, children.length - suffixCount);
    const suffix = children.slice(-suffixCount).trim();
    return (
        <Typography.Text
            style={{maxWidth: '100%'}}
            ellipsis={{suffix, tooltip: children}}
        >
            {start}
        </Typography.Text>
    );
};