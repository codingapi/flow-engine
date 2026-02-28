import { describe, expect, it } from '@rstest/core';
import { GroovyVariableMapping } from '@flow-engine/flow-types';
import { TitleSyntaxConverter } from '@/components/design-editor/script/service/title-syntax-converter';

describe('TitleSyntaxConverter', () => {
  const mappings: GroovyVariableMapping[] = [
    { label: '当前操作人', value: 'request.operatorName', expression: 'request.getOperatorName()', tag: '操作人相关', order: 1 },
    { label: '请假天数', value: 'request.formData("days")', expression: 'request.getFormData("days")', tag: '表单字段', order: 100 },
  ];

  describe('toGroovySyntax', () => {
    it('should convert simple text', () => {
      const result = TitleSyntaxConverter.toGroovySyntax('你好', mappings);
      expect(result).toContain('return "你好"');
    });

    it('should convert single variable', () => {
      const result = TitleSyntaxConverter.toGroovySyntax('${当前操作人}', mappings);
      expect(result).toContain('request.getOperatorName()');
    });

    it('should convert multiple variables', () => {
      const result = TitleSyntaxConverter.toGroovySyntax('你好，${当前操作人}，请假${请假天数}天', mappings);
      expect(result).toContain('request.getOperatorName()');
      expect(result).toContain('request.getFormData("days")');
    });
  });

  describe('toLabelExpression', () => {
    it('should parse groovy to label', () => {
      const groovy = '// @TITLE\nreturn "你好，" + request.getOperatorName() + "，请审批"';
      const result = TitleSyntaxConverter.toLabelExpression(groovy, mappings);
      expect(result).toBe('你好，${当前操作人}，请审批');
    });

    it('should return null for non-title script', () => {
      const groovy = 'def run(request){return "custom"}';
      const result = TitleSyntaxConverter.toLabelExpression(groovy, mappings);
      expect(result).toBeNull();
    });
  });

  describe('parseMode', () => {
    it('should detect normal mode', () => {
      const script = '// @TITLE\nreturn "你好，" + request.getOperatorName()';
      const mode = TitleSyntaxConverter.parseMode(script);
      expect(mode).toBe('normal');
    });

    it('should detect advanced mode', () => {
      const script = 'def run(request){return "custom"}';
      const mode = TitleSyntaxConverter.parseMode(script);
      expect(mode).toBe('advanced');
    });
  });
});
