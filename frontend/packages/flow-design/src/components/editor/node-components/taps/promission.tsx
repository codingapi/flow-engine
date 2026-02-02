import React from "react";
import {Field, FieldRenderProps} from "@flowgram.ai/fixed-layout-editor";
import {PromissionTable} from "@/components/editor/node-components/promission/table";

export const TabPromission: React.FC = () => {

    return (
        <Field
            name="FormFieldPermissionStrategy.fieldPermissions"
            render={({field: {value, onChange}}: FieldRenderProps<any>) => {
                return (
                    <PromissionTable
                        value={value}
                        onChange={onChange}
                    />
                );
            }}
        />
    )
}