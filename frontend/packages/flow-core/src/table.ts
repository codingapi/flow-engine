export interface Result<T> {
    data:T[]
    total: number;
    success: boolean
}

export interface ActionType {
    reload(): void;
}

export interface ParamRequest {
    current: number;
    pageSize: number;
    [key: string]: any;
}