import { useNodeRender, FlowNodeEntity } from '@flowgram.ai/fixed-layout-editor';

export const NodeRender = ({ node }: { node: FlowNodeEntity }) => {
    const { onMouseEnter, onMouseLeave, startDrag, form, dragging, activated } = useNodeRender();
    return (
        <div
            onMouseEnter={onMouseEnter}
            onMouseLeave={onMouseLeave}
            onMouseDown={(e) => { startDrag(e); e.stopPropagation(); }}
            style={{ width: 280, minHeight: 88, background: '#fff', borderRadius: 8, opacity: dragging ? 0.3 : 1, /* ... */ }}
        >
            {form?.render()}
        </div>
    );
};