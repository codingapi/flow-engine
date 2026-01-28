import { useEffect, useState, type FC } from 'react';
import { usePlayground, useService } from '@flowgram.ai/fixed-layout-editor';
import { FlowDownloadFormat, FlowDownloadService } from '@flowgram.ai/export-plugin';
import { Button, Dropdown, MenuProps, message, Tooltip } from "antd";
import { DownloadOutlined } from "@ant-design/icons";


export const DownloadTool: FC = () => {
    const [downloading, setDownloading] = useState<boolean>(false);
    const [visible, setVisible] = useState(false);
    const playground = usePlayground();
    const { readonly } = playground.config;
    const downloadService = useService(FlowDownloadService);


    const items: MenuProps['items'] = [
        {
            label: 'PNG',
            key: 'png',
            disabled:downloading || readonly,
            onClick: async () => {
                 await handleDownload(FlowDownloadFormat.PNG);
            }
        },
        {
            label: 'JPEG',
            key: 'jpeg',
            disabled:downloading || readonly,
            onClick: async () => {
                await handleDownload(FlowDownloadFormat.JPEG);
            }
        },
    ];

    const handleDownload = async (format: FlowDownloadFormat) => {
        setVisible(false);
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



    const button = (
        <Button
            className={visible ? '!coz-mg-secondary-pressed' : undefined}
            icon={<DownloadOutlined />}
            loading={downloading}
            onClick={() => setVisible(true)}
        />
    );

    return (
        <Dropdown
            menu={{items}}
            trigger={['click']}
        >
            {visible ? (
                button
            ) : (
                <div>
                    <Tooltip title="Download">{button}</Tooltip>
                </div>
            )}
        </Dropdown>
    );
};
