import { describe, expect, it } from '@rstest/core';
import { GroovyVariableMapping } from '@flow-engine/flow-types';
import { GroovySyntaxConverter } from '@/components/design-editor/script/service/groovy-syntax-converter';
import { ScriptType } from '@/components/design-editor/typings/groovy-script';

describe('GroovySyntaxConverter', () => {
  const converter = new GroovySyntaxConverter();

  const mappings: GroovyVariableMapping[] = [
    { label: '当前操作人', value: 'request.operatorName', expression: 'request.getOperatorName()', tag: '操作人相关', order: 1 },
    { label: '请假天数', value: 'request.formData("days")', expression: 'request.getFormData("days")', tag: '表单字段', order: 100 },
  ];

  describe('toScript', () => {
    it('should convert simple text to groovy script', () => {
      const result = converter.toScript(ScriptType.TITLE, '你好', mappings);
      expect(result).toContain('return "你好"');
      expect(result).toContain('def run(request)');
    });

    it('should convert single variable', () => {
      const result = converter.toScript(ScriptType.TITLE, '${当前操作人}', mappings);
      expect(result).toContain('request.getOperatorName()');
    });

    it('should convert multiple variables', () => {
      const result = converter.toScript(ScriptType.TITLE, '你好，${当前操作人}，请假${请假天数}天', mappings);
      expect(result).toContain('request.getOperatorName()');
      expect(result).toContain('request.getFormData("days")');
    });

    it('should handle empty content', () => {
      const result = converter.toScript(ScriptType.TITLE, '', mappings);
      expect(result).toContain('return ""');
    });
  });

  describe('toExpression', () => {
    it('should return null when no adapter registered', () => {
      const groovy = '// @TITLE\nreturn "你好，" + request.getOperatorName() + "，请审批"';
      const result = converter.toExpression(ScriptType.TITLE, groovy, mappings);
      // Without adapter, default implementation returns null for custom comment
      expect(result).toBeNull();
    });

    it('should return null for non-title script', () => {
      const groovy = 'def run(request){return "custom"}';
      const result = converter.toExpression(ScriptType.TITLE, groovy, mappings);
      expect(result).toBeNull();
    });
  });

  describe('getDefaultTemplate', () => {
    it('should return default template for unknown type', () => {
      const result = converter.getDefaultTemplate(ScriptType.CUSTOM);
      expect(result).toContain('def run(request)');
      expect(result).toContain('return ""');
    });

    it('should return default template when no adapter registered', () => {
      const result = converter.getDefaultTemplate(ScriptType.TITLE);
      expect(result).toContain('def run(request)');
    });
  });

  describe('isAdvancedMode', () => {
    it('should return false for simple script', () => {
      const script = "def run(request){return 'test'}";
      expect(converter.isAdvancedMode(script)).toBe(false);
    });

    it('should return true for custom script with comment', () => {
      const script = 'def run(request){\n// @CUSTOM_SCRIPT\nreturn "test"\n}';
      expect(converter.isAdvancedMode(script)).toBe(true);
    });
  });

  describe('registerAdapter', () => {
    it('should register and use custom adapter', () => {
      const mockAdapter = {
        scriptType: ScriptType.CUSTOM,
        getSystemVariables: () => [],
        toScript: (content: string) => `custom:${content}`,
        toExpression: () => null,
        getDefaultTemplate: () => 'custom-template',
      };

      converter.registerAdapter(mockAdapter);

      const result = converter.toScript(ScriptType.CUSTOM, 'test', []);
      expect(result).toBe('custom:test');
    });
  });
});
