import {GroovyScriptConvertorUtil} from "@/components/script/utils/convertor";

export const useScriptMetaData = (script:string) => {
    const meta = GroovyScriptConvertorUtil.getScriptMeta(script);
    console.log('meta:',meta);
    if(meta){
        return JSON.parse(meta);
    }else {
        return {};
    }
}