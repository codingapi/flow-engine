import {GroovyScriptConvertorUtil} from "@flow-engine/flow-core";

export const useScriptMetaData = (script:string) => {
    const meta = GroovyScriptConvertorUtil.getScriptMeta(script);
    if(meta){
        return JSON.parse(meta);
    }else {
        return {};
    }
}