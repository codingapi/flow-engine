import {
    NodeTitleGroovyConvertor
} from "@/components/script/services/convertor/node-title";
import {
    GroovyScriptConverter,
    GroovyVariableMapping,
    ScriptType
} from "@/components/script/typings";


/**
 * Groovy脚本转换器构造函数类型
 */
export type GroovyScriptConverterConstructor = new (script: string, variables: GroovyVariableMapping[]) => GroovyScriptConverter;


/**
 * Groovy脚本转换器上下文，负责管理不同类型脚本的转换器
 */
export class GroovyScriptConverterContext {
    private converterMap: Map<ScriptType, GroovyScriptConverterConstructor>;

    private register(scriptType: ScriptType, converter: GroovyScriptConverterConstructor) {
        this.converterMap.set(scriptType, converter);
    }

    public createConverter(scriptType: ScriptType, script: string, variables: GroovyVariableMapping[]): GroovyScriptConverter | undefined {
        const constructor = this.converterMap.get(scriptType);
        if (constructor) {
            return new constructor(script, variables);
        }
        return undefined;
    }

    private constructor() {
        this.converterMap = new Map();
        this.initializeDefaultConverters();
    }

    private initializeDefaultConverters() {
        try {
            this.register(ScriptType.TITLE, NodeTitleGroovyConvertor);
        } catch (e) {
            console.error('Failed to register default converters:', e);
        }
    }

    private static readonly instance = new GroovyScriptConverterContext();

    public static getInstance(): GroovyScriptConverterContext {
        return GroovyScriptConverterContext.instance;
    }
}
