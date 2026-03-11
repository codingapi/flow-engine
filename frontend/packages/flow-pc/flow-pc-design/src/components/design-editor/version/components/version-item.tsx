import React from "react";
import {Badge, Input, message, Popconfirm, Space, Typography} from "antd";
import {WorkflowVersion} from "@/components/design-editor/version/types";
import dayjs from "dayjs";
import {useDesignContext} from "@/components/design-panel/hooks/use-design-context";

const {Text} = Typography;

interface VersionItemProps {
    version: WorkflowVersion;
    onUpdateVersionName: (id: number, name: string) => Promise<void>;
    onVersionChange: (id: number) => Promise<void>;
}

export const VersionItem: React.FC<VersionItemProps> = (props) => {
    const {version} = props;

    const [editeVisible, setEditeVisible] = React.useState(false);
    const versionName = version.versionName;
    const versionDate = dayjs(version.updatedTime).format('YYYY-MM-DD HH:mm:ss');

    const [title, setTitle] = React.useState(versionName);
    const {context} = useDesignContext();

    return (
        <div
            style={{
                padding: 3,
                borderBottom: "1px solid #808080",
            }}
        >

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
                        <div
                            style={{
                                marginRight: 30,
                                width: 150
                            }}>
                            <Badge
                                count={version.current ? "当前" : undefined}
                                offset={[20, 5]}
                                style={{
                                    fontSize: 10,
                                }}
                                size="small"
                                color={'green'}
                            >
                                <Text>版本名称：{versionName}</Text>
                            </Badge>
                            <div style={{
                                fontSize: 12,
                                color: "gray",
                            }}>{versionDate}</div>
                        </div>
                        <a
                            onClick={() => {
                                setTitle(versionName);
                                setEditeVisible(true)
                            }}
                        >编辑</a>
                        {!version.current && (
                            <Popconfirm
                                title={"确认要切换到该版本吗？"}
                                onConfirm={async () => {
                                    props.onVersionChange(version.id).then(() => {
                                        context.initState();
                                    });
                                }}
                            >
                                <a>切换</a>
                            </Popconfirm>
                        )}
                    </Space>
                )}
            </Space>

        </div>
    )
}

