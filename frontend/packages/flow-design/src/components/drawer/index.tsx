import React from "react";
import {Drawer as AntdDrawer} from "antd";

interface DrawerProps {
    open: boolean;
    onClose?: () => void;
    children?:React.ReactNode;
}


export const Drawer: React.FC<DrawerProps> = (props) => {
    return (
        <AntdDrawer
            title={false}
            open={props.open}
            size={"100%"}
            closeIcon={false}
            onClose={props.onClose}
        >
            {props.children}
        </AntdDrawer>
    )
}