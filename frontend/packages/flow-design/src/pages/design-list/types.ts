import { ParamRequest, Result } from "@/components/table";

export interface State {
    pageVersion: number;
    editable: boolean;
    currentId: string;
}


export interface DataType {
    id: string,
    workCode: string,
    title: string,
    createTime: number
}


export interface DesignListApi {

    request: (request: ParamRequest) => Promise<Result<DataType>>;

    delete: (id: string) => Promise<void>;

}

export interface DesignListProps {


}