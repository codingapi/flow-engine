import React from "react";
import {Flex} from "antd";

interface TabBaseProps{
    children?:React.ReactNode;
}

export const TabBase:React.FC<TabBaseProps> = (props) => {

    return (
        <Flex
            justify="center"
            vertical={true}
            align={"center"}
            style={{
                width: "100%",
                padding: 8,
            }}
        >
            {props.children}
        </Flex>
    )
}