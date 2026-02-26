import {ParamRequest, Result} from "@flow-engine/flow-pc-ui";
import {DataType, DesignListApi} from "./types";
import {list, remove,changeState} from "@/api/workflow";

export class DesignListApiImpl implements DesignListApi {

    public async request(request: ParamRequest) {
        const result = await list(request);
        return result as Result<DataType>;
    }

    public async delete(id: string) {
        await remove(id);
    }

    public async changeState(id: string) {
        await changeState(id);
    }

}