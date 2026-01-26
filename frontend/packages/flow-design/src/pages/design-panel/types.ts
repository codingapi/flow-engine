
export type PanelTabType = 'base' | 'form' | 'flow' | 'setting';

export interface DesignPanelProps {
    open:boolean;
    onClose?:() => void;
}


export interface State{
    value:number;
    panelTab:PanelTabType;
}

export const initStateData: State = {
    value:100,
    panelTab:'base',
}


export interface DesignPanelApi{

}
