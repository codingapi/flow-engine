import { ParamRequest, Result } from "@/components/table";
import { DataType, DesignListApi } from "./types";
import { list ,remove} from "@/api/workflow";

export class DesignListApiImpl implements DesignListApi {

    public async request  (request: ParamRequest) {
        const result = await list(request);
        return result as Result<DataType>;
    }

    public async delete(id: string) {
        await remove(id);
    }



}