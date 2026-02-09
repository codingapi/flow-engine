export type JsonSchemaType =
  | 'string'
  | 'number'
  | 'integer'
  | 'boolean'
  | 'object'
  | 'array'
  | 'null';

export type JsonSchema = {
  type?: JsonSchemaType;
  title?: string;
  description?: string;
  default?: any;
  enum?: any[];
  const?: any;
  properties?: Record<string, JsonSchema>;
  required?: string[];
  items?: JsonSchema;
  additionalProperties?: boolean | JsonSchema;
  $ref?: string;
  [key: string]: any;
};
