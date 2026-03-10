import React from "react";
import {VersionContainer, VersionSection} from "@/components/design-editor/styles";
import {useDesignContext} from "@/components/design-panel/hooks/use-design-context";
import {Badge, Button} from "antd";
import {versions as versionList} from "@/api/workflow";
import {HistoryOutlined} from "@ant-design/icons";

export const EditorVersion = () => {

    const {state} = useDesignContext();

    const [versions, setVersions] = React.useState<any[]>([]);

    React.useEffect(() => {
        versionList(state.workflow.id).then(res => {
            setVersions(res.data.list);
        })
    }, [state.workflow.id])

    return (
        <VersionContainer>
            <VersionSection>
                <Badge count={versions.length}>
                    <Button
                        type={"text"}
                        icon={<HistoryOutlined/>}
                        onClick={() => {
                            console.log(versions);
                        }}
                    >
                        版本
                    </Button>
                </Badge>
            </VersionSection>
        </VersionContainer>
    )
}