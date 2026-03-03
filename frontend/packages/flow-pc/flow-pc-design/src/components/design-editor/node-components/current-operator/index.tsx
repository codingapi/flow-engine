import React from "react";
import {useNodeFormContext} from "@/components/design-editor/hooks/use-node-form";
import {GroovyScriptConvertorUtil} from "@/components/script/utils/convertor";

export const CurrentNodeOperator = () => {
    const data = useNodeFormContext();
    const script = data.form.getValueIn('OperatorLoadStrategy.script');
    return (
        <span>
            {GroovyScriptConvertorUtil.getScriptTitle(script)}
        </span>
    )
}