export type PanelTabType = 'base' | 'form' | 'flow' | 'setting';

export const LayoutHeaderHeight = 50;

export interface DesignPanelProps {
    open:boolean;
    onClose?:() => void;
}

export interface State{
    panelTab:PanelTabType;
}

export const initStateData: State = {
    panelTab:'base',
}


export interface DesignPanelApi{

}
