import React from "react";
import {EditorRenderer, FixedLayoutEditorProvider } from "@flowgram.ai/fixed-layout-editor";
import { useEditorProps } from './hooks/use-editor-props';
import {EditorTools} from "@/components/editor/tools";
import { FlowNodeRegistries } from './nodes';

export const FlowEditor = ()=>{
    const editorProps = useEditorProps(FlowNodeRegistries);
    return (
        <FixedLayoutEditorProvider
            {...editorProps}
        >
            <EditorRenderer />
            <EditorTools/>
        </FixedLayoutEditorProvider>
    )
}