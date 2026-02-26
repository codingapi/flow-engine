import React from "react";
import {FlowEditor, FlowEditorAction} from "@/components/design-editor";
import {useDesignContext} from "@/components/design-panel/hooks/use-design-context";
import {NodeManager} from "@/components/design-panel/manager/node";

export const TabFlow = () => {

    const {state, context} = useDesignContext();
    const formActionContext = context.getPresenter().getFormActionContext();

    const actionRef = React.useRef<FlowEditorAction | null>(null);

    const nodeManager = new NodeManager();

    // 注册form行为
    React.useEffect(() => {
        formActionContext.addAction({
            save() {
                const data = actionRef.current?.getData();
                return {
                    nodes: nodeManager.toData(data?.nodes || []),
                }
            },
            key(): string {
                return 'flow';
            }
        });

        if (state.workflow.nodes) {
            const nodes = nodeManager.toRender(state.workflow.nodes || []);
            actionRef.current?.resetData({
                nodes
            } as any);
        }

        return () => {
            formActionContext.removeAction('flow');
        }
    }, []);

    React.useEffect(() => {
        const nodes = nodeManager.toRender(state.workflow.nodes || []);
        actionRef.current?.resetData({
            nodes
        } as any);
    }, [state.workflow.nodes]);

    return (
        <div style={{height: 'calc(100vh - 100px)', width: '100%', position: 'relative'}}>
            <FlowEditor
                actionRef={actionRef}
            />
        </div>
    )
}