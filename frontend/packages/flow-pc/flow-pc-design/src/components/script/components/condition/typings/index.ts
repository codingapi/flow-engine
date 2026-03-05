/**
 *  数据类型
 */
export type DataType = 'string' | 'number' | 'boolean' | 'date';

/**
 *  条件类型
 */
export type RelationType = 'equal' | 'greater_than' | 'greater_equal' | 'less_than' | 'less_equal' | 'not_equal';

/**
 *  逻辑关系
 */
export type LogicalType = 'and' | 'or' | 'group' | 'condition' | 'action';

/**
 *  条件状态
 */
export interface ConditionState {
    // 条件组
    groups: ConditionGroup[];
    // 关系定义
    relations: ConditionRelation[];
}

/**
 *  初始化状态数据
 */
export const initStateData: ConditionState = {
    groups: [],
    relations: [],
}

/**
 *  条件组件数据
 */
export interface ConditionViewProps {
    script: string;
    onChange: (value: string) => void;
}

/**
 *  条件接口
 */
export interface ConditionApi{

}

/**
 *  条件关系
 */
export interface ConditionRelation {
    // 条件名称
    label: string;
    // 类型
    type: LogicalType;
    // 条件组Id
    groupId?: string;
    // 子条件
    children?: ConditionRelation[];
}


/**
 *  条件关系
 */
export const relationTypeOptions = [
    {
        label: '等于',
        value: 'equals',
    },
    {
        label: '大于等于',
        value: 'greater_equal',
    },
    {
        label: '大于',
        value: 'greater_than',
    },
    {
        label: '小于等于',
        value: 'less_equal',
    },
    {
        label: '小于',
        value: 'less_than',
    },
    {
        label: '不等于',
        value: 'not_equal',
    },
]


/**
 * 条件参数
 */
export interface Condition {
    // 变量展示名称
    label: string;
    // 变量实际表达式
    value: string;
    // 参数的数据类型
    type: DataType;
}


/**
 *  条件组
 */
export interface ConditionGroup {
    // 分组编号
    id: string;
    // 左侧参数
    left: Condition;
    // 右侧参数
    right: Condition;
    // 参数关系
    type: RelationType;
}