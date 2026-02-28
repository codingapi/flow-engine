import { describe, expect, it } from '@rstest/core';
import { GroovyVariableService } from '@/components/design-editor/script/service/groovy-variable-service';
import { ScriptType } from '@/components/design-editor/typings/groovy-script';

describe('GroovyVariableService', () => {
  const service = new GroovyVariableService();

  describe('getSystemVariables', () => {
    it('should return predefined variables for TITLE type', () => {
      const variables = service.getSystemVariables(ScriptType.TITLE);

      expect(variables.length).toBeGreaterThan(0);
      expect(variables.some(v => v.label === '当前操作人')).toBe(true);
      expect(variables.some(v => v.label === '流程标题')).toBe(true);
    });

    it('should return predefined variables for unknown type', () => {
      const variables = service.getSystemVariables(ScriptType.CUSTOM);

      expect(variables.length).toBeGreaterThan(0);
    });
  });

  describe('getVariables', () => {
    it('should return variables without form fields', () => {
      const variables = service.getVariables(ScriptType.TITLE);

      expect(variables.length).toBeGreaterThan(0);
    });

    it('should include form fields when provided', () => {
      const formFields = [
        { name: '请假天数', code: 'days' },
        { name: '请假人', code: 'name' },
      ];

      const variables = service.getVariables(ScriptType.TITLE, { formFields });

      expect(variables.length).toBeGreaterThan(2);
      expect(variables.some(v => v.label === '请假天数')).toBe(true);
      expect(variables.some(v => v.label === '请假人')).toBe(true);
    });
  });

  describe('getFormFieldVariables', () => {
    it('should convert form fields to variable mappings', () => {
      const fields = [
        { name: '请假天数', code: 'days' },
        { name: '请假人', code: 'name' },
      ];

      const variables = service.getFormFieldVariables(fields);

      expect(variables.length).toBe(2);
      expect(variables[0].label).toBe('请假天数');
      expect(variables[0].expression).toBe('request.getFormData("days")');
      expect(variables[1].label).toBe('请假人');
      expect(variables[1].expression).toBe('request.getFormData("name")');
    });
  });

  describe('groupByTag', () => {
    it('should group variables by tag', () => {
      const variables = service.getSystemVariables(ScriptType.TITLE);

      const groups = service.groupByTag(variables);

      expect(groups.size).toBeGreaterThan(0);
      expect(groups.has('操作人相关')).toBe(true);
      expect(groups.has('流程相关')).toBe(true);
    });

    it('should return empty map for empty input', () => {
      const groups = service.groupByTag([]);

      expect(groups.size).toBe(0);
    });
  });

  describe('registerAdapter', () => {
    it('should register and use custom adapter', () => {
      const customVariables = [
        { label: '自定义变量', value: 'custom', expression: 'getCustom()', tag: '自定义', order: 1 },
      ];

      const mockAdapter = {
        scriptType: ScriptType.CUSTOM,
        getSystemVariables: () => customVariables,
        toScript: () => '',
        toExpression: () => null,
        getDefaultTemplate: () => '',
      };

      service.registerAdapter(mockAdapter);

      const variables = service.getSystemVariables(ScriptType.CUSTOM);

      expect(variables).toEqual(customVariables);
    });
  });
});
