import React from "react";
import {VersionContainer, VersionSection} from "@/components/design-editor/styles";
import {Badge, Button, Input, message, Popconfirm, Popover, Space, Tag, Typography} from "antd";
import {HistoryOutlined} from "@ant-design/icons";
import {useVersionPresenter} from "@/components/design-editor/version/hooks/use-version-presenter";
import {WorkflowVersion} from "@/components/design-editor/version/types";
import {VersionPresenter} from "@/components/design-editor/version/presenter";
import dayjs from "dayjs";

const {Text} = Typography;


interface VersionItemProps {
    version: WorkflowVersion;
    onUpdateVersionName: (id: number, name: string) => Promise<void>;
    onVersionChange: (id: number) => void;
}

const VersionItem: React.FC<VersionItemProps> = (props) => {
    const {version} = props;

    const [editeVisible, setEditeVisible] = React.useState(false);
    const versionName = version.versionName;
    const versionDate = dayjs(version.updatedTime).format('YYYY-MM-DD');

    const [title, setTitle] = React.useState(versionName);

    return (
        <div style={{
            padding: 3,
            borderBottom: "1px solid #808080",
        }}>
            <Space>
                {editeVisible && (
                    <Space>
                        <Input
                            value={title}
                            onChange={(e) => {
                                setTitle(e.target.value);
                            }}
                            placeholder={"请输入版本名称"}
                        />
                        <a onClick={() => {
                            props.onUpdateVersionName(version.id, title).then(() => {
                                setEditeVisible(false);
                                message.success('保存成功')
                            });
                        }}>确定</a>
                        <a onClick={() => {
                            setEditeVisible(false)
                        }}>取消</a>
                    </Space>
                )}
                {!editeVisible && (
                    <Space>
                        <Text>{versionName}</Text>
                        <Text>({versionDate})</Text>
                        <a
                            onClick={() => {
                                setTitle(versionName);
                                setEditeVisible(true)
                            }}
                        >编辑</a>
                        {!version.current && (
                            <Popconfirm
                                title={"确认要切换到该版本吗？"}
                                onConfirm={() => {
                                    props.onVersionChange(version.id);
                                }}
                            >
                                <a>切换</a>
                            </Popconfirm>

                        )}

                        {version.current && (
                            <Tag color={'success'}>当前版本</Tag>
                        )}
                    </Space>
                )}
            </Space>
        </div>
    )
}


interface VersionContentProps {
    versions: WorkflowVersion[];
    presenter: VersionPresenter;
}

const VersionContent: React.FC<VersionContentProps> = (props) => {
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

export const EditorVersion = () => {

    const {state, presenter} = useVersionPresenter();

    return (
        <VersionContainer>
            <VersionSection>
                <Badge count={state.length}>
                    <Popover
                        content={<VersionContent versions={state} presenter={presenter}/>}
                        trigger="click"
                        placement="bottom"
                    >
                        <Button
                            type={"text"}
                            icon={<HistoryOutlined/>}
                            onClick={() => {
                                console.log(presenter);
                            }}
                        >
                            版本
                        </Button>
                    </Popover>
                </Badge>
            </VersionSection>
        </VersionContainer>
    )
}