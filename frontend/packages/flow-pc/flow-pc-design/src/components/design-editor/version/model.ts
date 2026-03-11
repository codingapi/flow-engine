import {VersionApi, WorkflowVersion} from "./types";
import {updateVersionName as updateVersionNameApi, versions, changeVersion as changeVersionApi} from "@/api/workflow";

export class VersionApiImpl implements VersionApi {

    loadVersions = async (workId: string): Promise<WorkflowVersion[]> => {
        const result = await versions(workId);
        if (result.success) {
            return result.data.list;
        }
        return []
    }

    updateVersionName = async (id: number, versionName: string) => {
        await updateVersionNameApi({
            id,
            versionName
        })
    }

    changeVersion = async (id: number) => {
        await changeVersionApi(id);
    }

}