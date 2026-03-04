import {useDesignContext} from "@/components/design-panel/hooks/use-design-context";
import {NodeRouterManager} from "@/components/design-panel/manager/node";

export const useNodeRouterManager = ()=>{
    const {state} = useDesignContext();
    const nodes = state.workflow.nodes || [];
    return  new NodeRouterManager(nodes);
}