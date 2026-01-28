import {type FC, useEffect, useState} from 'react';
import {useService} from '@flowgram.ai/fixed-layout-editor';
import {FlowDownloadFormat, FlowDownloadService} from '@flowgram.ai/export-plugin';
import {Button, Dropdown, MenuProps, message, Tooltip} from "antd";
import {DownloadOutlined} from "@ant-design/icons";


export const DownloadTool: FC = () => {
    const [downloading, setDownloading] = useState<boolean>(false);
    const [visible, setVisible] = useState<boolean>(false);
    const downloadService = useService(FlowDownloadService);

    const items: MenuProps['items'] = [
        {
            label: 'PNG',
            key: 'png',
            onClick: async () => {
                 await handleDownload(FlowDownloadFormat.PNG);
            }
        },
        {
            label: 'JPEG',
            key: 'jpeg',
            onClick: async () => {
                await handleDownload(FlowDownloadFormat.JPEG);
            }
        },
    ];

    const handleDownload = async (format: FlowDownloadFormat) => {
        await downloadService.download({
            format,
        });
        message.success(`Download ${format} successfully`);
    };


    useEffect(() => {
        const subscription = downloadService.onDownloadingChange((v) => {
            setDownloading(v);
        });

        return () => {
            subscription.dispose();
        };
    }, [downloadService]);

    return (
        <Dropdown
            menu={{items}}
            trigger={['click']}
            onOpenChange={(open)=>{
                setVisible(open);
            }}
        >
            <Tooltip
                title={visible?null:"Download"}
                trigger={"hover"}
                open={visible?false:undefined}
            >
                <Button
                    type="text"
                    icon={<DownloadOutlined />}
                    loading={downloading}
                />
            </Tooltip>
        </Dropdown>
    );
};
