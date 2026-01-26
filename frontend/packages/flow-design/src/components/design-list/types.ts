import { ParamRequest, Result } from "../table";

export interface State {
}


export interface DataType {
    id: string,
    workCode: string,
    title: string,
    createTime: number
}


export interface DesignListApi {
    request: (request: ParamRequest) => Promise<Result<DataType>>;
}

export interface DesignListProps {


}