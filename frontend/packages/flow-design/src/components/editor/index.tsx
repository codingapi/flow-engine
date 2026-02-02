import React from "react";
import {EditorRenderer, FixedLayoutEditorProvider, FixedLayoutPluginContext} from "@flowgram.ai/fixed-layout-editor";
import {useEditorProps} from './hooks/use-editor-props';
import {EditorTools} from "@/components/editor/tools";
import {initialData as defaultInitialData} from './initial-data';
import {FlowNodeRegistries} from './nodes';
import "./index.scss";
import {FlowDocumentJSON} from "@/components/editor/typings";


export interface FlowEditorAction {
    getData(): FlowDocumentJSON;

    resetData(data:FlowDocumentJSON): void;
}

interface FlowEditorProps {
    actionRef?: React.Ref<FlowEditorAction>;

    initialData?: FlowDocumentJSON;
}

export const FlowEditor: React.FC<FlowEditorProps> = (props) => {

    const ref = React.useRef<FixedLayoutPluginContext | null>(null);
    const initialData = props.initialData || defaultInitialData;
    const editorProps = useEditorProps(initialData, FlowNodeRegistries);

    React.useImperativeHandle(props.actionRef, () => ({
        getData: () => {
            if (ref.current) {
                return ref.current.document.toJSON();
            }
            return initialData;
        },
        resetData: (data:FlowDocumentJSON) => {
            if (ref.current) {
                if(data.nodes && data.nodes.length >0){
                    ref.current.document.fromJSON(data);
                }else {
                    ref.current.document.fromJSON(defaultInitialData);
                }
            }
        }
    }), [ref]);

    return (
        <FixedLayoutEditorProvider
            ref={ref}
            {...editorProps}
        >
            <EditorRenderer/>
            <EditorTools/>
        </FixedLayoutEditorProvider>
    )
}