import {Flex} from "antd";
import React, {ReactNode} from "react";

interface PanelProps {
    children?: ReactNode;
}

export const Panel: React.FC<PanelProps> = (props) => {

    return (
        <Flex
            justify='center'
            style={{
                marginLeft: '20%',
                marginRight: '20%',
            }}
        >
            {props.children}
        </Flex>
    )
}