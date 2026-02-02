import React, {useContext} from "react";
import {NodeRenderContext,NodeDisplayManager} from "@/components/editor/context";
import {useDesignContext} from "@/pages/design-panel/hooks/use-design-context";

export const useNodeDisplayManager = () => {
    const {node} = useContext(NodeRenderContext);
    const {state} = useDesignContext();

    return React.useMemo(() => {
        return new NodeDisplayManager(node.id, state.workflow.nodes || []);
    }, [state.workflow.nodes]);

}