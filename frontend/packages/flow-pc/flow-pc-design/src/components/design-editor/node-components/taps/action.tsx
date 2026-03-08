import React from "react";
import {Field, FieldRenderProps} from "@flowgram.ai/fixed-layout-editor";
import {ActionTable} from "@/components/design-editor/node-components/action";

export const TabAction: React.FC = () => {

    return (
        <Field
            name="actions"
            render={({field: {value, onChange}}: FieldRenderProps<any>) => {
                return (
                    <ActionTable
                        value={value}
                        onChange={onChange}
                    />
                );
            }}
        />
    )
}