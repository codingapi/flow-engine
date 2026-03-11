import {VersionApi, WorkflowVersion} from "./types";
import {Dispatch} from "@flow-engine/flow-core";

export class VersionPresenter {

    private readonly workId: string;
    private versions: WorkflowVersion[];
    private readonly dispatch: Dispatch<WorkflowVersion[]>;
    private readonly api: VersionApi;

    constructor(workId: string, versions: WorkflowVersion[], dispatch: Dispatch<WorkflowVersion[]>, api: VersionApi) {
        this.workId = workId;
        this.versions = versions;
        this.dispatch = dispatch;
        this.api = api;
    }

    public syncState(versions: WorkflowVersion[]): void {
        this.versions = versions;
    }

    public async initState() {
        const versions = await this.api.loadVersions(this.workId);
        this.versions = versions;
        this.dispatch(versions);
    }


    public async updateVersionName(id: number, name: string) {
        await this.api.updateVersionName(id, name);
        await this.initState();
    }

    public async changeVersion(id: number) {
        await this.api.changeVersion(id);
        await this.initState();
    }

}