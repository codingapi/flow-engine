// 确保适配器初始化 - 这个导入会触发 adapters.ts 中的自动初始化
import '@/components/design-editor/script/service/adapters';

export { ScriptEditor } from './components/script-editor';
export { ScriptConfigModal } from './components/script-config-modal';
export type { ScriptConfigModalProps } from './components/script-config-modal';
export { NodeTitleConfigModal } from './node-title-config-modal';
export type { NodeTitleConfigModalProps } from './node-title-config-modal';
export { ConditionConfigModal } from './condition-config-modal';
export type { ConditionConfigModalProps } from './condition-config-modal';
export { groovyVariableService } from '@/components/design-editor/script/service/groovy-variable-service';
export { groovySyntaxConverter } from '@/components/design-editor/script/service/groovy-syntax-converter';
