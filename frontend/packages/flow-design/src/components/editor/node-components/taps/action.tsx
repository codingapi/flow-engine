import React, {useContext} from "react";
import {Flex} from "antd";
import {NodeRenderContext} from "@/components/editor/context";
import {ActionManager} from "@/components/editor/node-components/action";

export const TabAction:React.FC = () => {
    const {node} = useContext(NodeRenderContext);
    const actions = node.getNodeRegistry()?.meta.actions || [];
    const actionManager = React.useCallback(()=>{
        return new ActionManager(actions);
    },[actions]);
    return (
        <Flex
            justify="center"
            vertical={true}
            align={"center"}
            style={{
                width: "100%",
            }}
        >
            {actionManager().render('SAVE')}
            {actionManager().render('PASS')}
            {actionManager().render('REJECT')}
            {actionManager().render('ADD_AUDIT')}
            {actionManager().render('RETURN')}
            {actionManager().render('TRANSFER')}
            {actionManager().render('DELEGATE')}
            {actionManager().render('CUSTOM')}
        </Flex>
    )
}