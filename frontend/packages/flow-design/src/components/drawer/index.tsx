import React from "react";
import {Drawer as AntdDrawer,DrawerProps as AntdDrawerProps} from "antd";

interface DrawerProps extends AntdDrawerProps {
    open?: boolean;
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
            destroyOnHidden={true}
            onClose={props.onClose}
            {...props}
        >
            {props.children}
        </AntdDrawer>
    )
}