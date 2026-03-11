import React from "react";
import {WorkflowVersion} from "@/components/design-editor/version/types";
import {VersionPresenter} from "@/components/design-editor/version/presenter";
import {VersionItem} from "./version-item";


interface VersionContentProps {
    versions: WorkflowVersion[];
    presenter: VersionPresenter;
}

export const VersionContent: React.FC<VersionContentProps> = (props) => {
    return (
        <div style={{
            padding: "10px",
        }}>
            {props.versions.map((version: WorkflowVersion) => {
                return (
                    <VersionItem
                        version={version}
                        onUpdateVersionName={async (id, name) => {
                            await props.presenter.updateVersionName(id, name);
                        }}
                        onVersionChange={async (versionId) => {
                            await props.presenter.changeVersion(versionId);
                        }}
                    />
                )
            })}
        </div>
    )
}