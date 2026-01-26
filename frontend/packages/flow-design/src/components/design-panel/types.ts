
export type PanelTabType = 'base' | 'form' | 'flow' | 'setting';

export interface DesignPanelProps {
    open:boolean;
    onClose?:() => void;
}


export interface State{
    value:number;
}

export const initState: State = {
    value:100,
}


export interface DesignPanelApi{

}
