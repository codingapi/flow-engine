import { ParamRequest, Result } from "../table";
import { DataType, DesignListApi } from "./types";
import { list } from "@/api/workflow";

export class DesignListApiImpl implements DesignListApi {

    public request = (request: ParamRequest): Promise<Result<DataType>> => {
        return list(request) as Promise<Result<DataType>>;
    }

}