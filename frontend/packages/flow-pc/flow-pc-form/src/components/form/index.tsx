import React from "react";
import {FormViewProps} from "@flow-engine/flow-types";
import {Form} from "antd";
import {ObjectUtils} from "@flow-engine/flow-core";
import {FormItemFactory} from "@/components/factory/form-item-factory";

export const FlowFormView: React.FC<FormViewProps> = (props) => {

    const [values, setValues] = React.useState<any>({});

    const form = props.form;

    const meta = props.meta;

    const fields = meta.fields || [];

    const review = props.review;

    return (
        <Form
            form={form as any}
            layout={"vertical"}
            onBlur={() => {
                const latestValues = form.getFieldsValue();
                if (ObjectUtils.isEqual(values, latestValues)) {
                    return;
                }
                setValues(latestValues);
                props.onValuesChange?.(latestValues);
            }}
        >
            {fields.map((field, i) => {
                const FormItem = FormItemFactory.getInstance().createFrom(field.type);
                if (FormItem) {
                    return (
                        <FormItem
                            key={field.id}
                            {...field}
                            readOnly={review}
                        />
                    );
                }
            })}
        </Form>
    )
};