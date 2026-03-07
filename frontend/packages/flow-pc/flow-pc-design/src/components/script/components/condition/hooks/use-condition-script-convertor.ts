import {ConditionPanelProps} from "@/components/script/components/condition/typings";
import {useConditionContext} from "@/components/script/components/condition/hooks/use-condition-context";
import {ConditionGroovyConvertor} from "@/components/script/components/condition/convertor/groovy";

export const useConditionScriptConvertor = (props: ConditionPanelProps) => {

    const {state} = useConditionContext();

    if(state.relations) {
        const scriptConvertor = new ConditionGroovyConvertor(state);
        if (!scriptConvertor.isError()) {
            const script = scriptConvertor.getScript();
            props.onChange(script);
            console.log("convert script", script);
        }
    }
}