import React from "react";
import {FlowEditor, FlowEditorAction} from "@/components/editor";
import {useDesignContext} from "@/pages/design-panel/hooks/use-design-context";

export const TabFlow = () => {

    const {state, context} = useDesignContext();
    const formActionContext = context.getPresenter().getFormActionContext();

    const actionRef = React.useRef<FlowEditorAction | null>(null);

    // 注册form行为
    React.useEffect(() => {
        formActionContext.addAction({
            save() {
                const data = actionRef.current?.getData();
                return {
                    nodes: data?.nodes || []
                }
            },
            key(): string {
                return 'flow';
            }
        });

        if (state.workflow.nodes) {
            const nodes = state.workflow.nodes;
            actionRef.current?.resetData({
                nodes
            } as any);
        }

        return () => {
            formActionContext.removeAction('flow');
        }
    }, []);

    React.useEffect(() => {
        const nodes = state.workflow.nodes;
        console.log('reset flow data:', nodes);
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