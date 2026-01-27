export type PanelTabType = 'base' | 'form' | 'flow' | 'setting';

// 布局顶部高度
export const LayoutHeaderHeight = 50;

// Form字段类型
export const FormFieldOptions = [
        {
            label: '整数',
            value: 'number'
        },
        {
            label: '字符串',
            value: 'string'
        },
        {
            label: '小数类型',
            value: 'float'
        },
        {
            label: '布尔类型',
            value: 'boolean'
        },
]


export interface DesignPanelProps {
    open: boolean;
    onClose?: () => void;
}

export interface State {
    panelTab: PanelTabType;
}

export const initStateData: State = {
    panelTab: 'base',
}


export interface DesignPanelApi {

}
