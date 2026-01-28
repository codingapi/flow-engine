import {usePlaygroundTools} from '@flowgram.ai/fixed-layout-editor';
import {SelectZoom} from '../styles';
import {Dropdown, MenuProps} from "antd";

export const ZoomSelect = () => {
    const tools = usePlaygroundTools({ maxZoom: 2, minZoom: 0.25 });

    const items: MenuProps['items'] = [
        {
            label: 'Zoomin',
            key: 'Zoomin',
            onClick:()=>{
                tools.zoomin()
            }
        },
        {
            label: 'Zoomout',
            key: 'Zoomout',
            onClick:()=>{
                tools.zoomout()
            }
        },
        {
            type: 'divider',
        },
        {
            label: '50%',
            key: '50',
            onClick:()=>{
                tools.updateZoom(0.5)
            }
        },
        {
            label: '100%',
            key: '100',
            onClick:()=>{
                tools.updateZoom(1)
            }
        },
        {
            label: '150%',
            key: '150',
            onClick:()=>{
                tools.updateZoom(1.5)
            }
        },
        {
            label: '200%',
            key: '200',
            onClick:()=>{
                tools.updateZoom(2)
            }
        },
    ];
    return (
        <Dropdown menu={{ items }} trigger={['click']}>
            <a onClick={(e) => e.preventDefault()}>
                <SelectZoom>{Math.floor(tools.zoom * 100)}%</SelectZoom>
            </a>
        </Dropdown>
    );
};
