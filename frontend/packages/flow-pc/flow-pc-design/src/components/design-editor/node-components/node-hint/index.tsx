import React from "react";
import {GroovyScriptConvertorUtil} from "@flow-engine/flow-core";
import {Field, FieldRenderProps} from "@flowgram.ai/fixed-layout-editor";

interface NodeHintProps{
    fieldName:string;
}

export const NodeHint:React.FC<NodeHintProps> = (props) => {
    return (
        <span>
            <Field
                name={props.fieldName}
                render={({ field: { value, onChange } }: FieldRenderProps<any>) => (
                    <>
                        {GroovyScriptConvertorUtil.getScriptTitle(value)}
                    </>
                )}
            />
        </span>
    )
}