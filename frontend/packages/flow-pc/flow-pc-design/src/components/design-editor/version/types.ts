export interface WorkflowVersion {
    id: number;
    versionName: string;
    current: boolean;
    updatedTime: number;
}


export interface VersionApi {

     loadVersions(workId: string): Promise<WorkflowVersion[]>;

     updateVersionName(id:number, versionName: string): Promise<void>;

    changeVersion(id:number): Promise<void>;

}