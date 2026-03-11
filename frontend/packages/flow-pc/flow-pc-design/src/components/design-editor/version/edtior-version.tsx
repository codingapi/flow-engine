import React from "react";
import {VersionContainer, VersionSection} from "@/components/design-editor/styles";
import {Button, Popover} from "antd";
import {HistoryOutlined} from "@ant-design/icons";
import {useVersionPresenter} from "@/components/design-editor/version/hooks/use-version-presenter";
import {VersionContent} from "./components/version-content";

export const EditorVersion = () => {

    const {state, presenter} = useVersionPresenter();

    return (
        <VersionContainer>
            <VersionSection>

                <Popover
                    content={<VersionContent versions={state} presenter={presenter}/>}
                    trigger="click"
                    placement="bottom"
                >
                    <Button
                        type={"text"}
                        icon={<HistoryOutlined/>}
                    >
                        版本共{state.length}条
                    </Button>
                </Popover>

            </VersionSection>
        </VersionContainer>
    )
}