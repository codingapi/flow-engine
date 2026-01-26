import React from "react";
import {Drawer as AntdDrawer} from "antd";
import {PanelBody} from "./body";

interface DrawerProps {
    open: boolean;
    onClose?: () => void;
    onSave?: () => void;
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
           <PanelBody onClose={props.onClose} />
        </AntdDrawer>
    )
}