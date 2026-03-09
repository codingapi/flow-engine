import {Flex} from "antd";
import React, {ReactNode} from "react";

interface PanelProps {
    children?: ReactNode;
}

export const Panel: React.FC<PanelProps> = (props) => {

    return (
        <Flex
            justify='center'
            vertical={true}
            style={{
                marginLeft: '10%',
                marginRight: '10%',
            }}
        >
            {props.children}
        </Flex>
    )
}