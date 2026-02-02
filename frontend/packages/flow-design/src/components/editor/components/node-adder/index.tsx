import React, {useState} from 'react';
import {Popover} from "antd";
import {type FlowNodeEntity, useClientContext} from '@flowgram.ai/fixed-layout-editor';
import {generateNodeId} from "@/components/editor/utils";

import {NodeList} from '../node-list';
import {Wrap} from './styles';
import {PlusCircleOutlined} from "@ant-design/icons";


const generateNewIdForChildren = (n: FlowNodeEntity): FlowNodeEntity => {
    if (n.blocks) {
        return {
            ...n,
            id: generateNodeId(n),
            blocks: n.blocks.map((b) => generateNewIdForChildren(b)),
        } as FlowNodeEntity;
    } else {
        return {
            ...n,
            id: generateNodeId(n),
        } as FlowNodeEntity;
    }
};

interface AdderProps {
    from: FlowNodeEntity;
    to?: FlowNodeEntity;
    hoverActivated: boolean;
}

export const Adder: React.FC<AdderProps> = (props) => {
    const {from} = props;
    const [visible, setVisible] = useState(false);
    const {playground, operation, clipboard} = useClientContext();

    const add = (addProps: any) => {
        const blocks = addProps.blocks ? addProps.blocks : undefined;
        const block = operation.addFromNode(from, {
            ...addProps,
            blocks,
        });
        setTimeout(() => {
            playground.scrollToView({
                bounds: block.bounds,
                scrollToCenter: true,
            });
        }, 10);
        setVisible(false);
    };

    if (playground.config.readonly) return null;

    return (
        <Popover
            open={visible}
            onOpenChange={setVisible}
            content={<NodeList onSelect={add} from={from}/>}
            placement="right"
            trigger="click"
        >
            <Wrap
                style={
                    props.hoverActivated
                        ? {
                            width: 15,
                            height: 15,
                        }
                        : {}
                }
                onMouseDown={(e: any) => e.stopPropagation()}
            >
                {props.hoverActivated ? (
                    <PlusCircleOutlined
                        onClick={() => {
                            setVisible(true);
                        }}
                        style={{
                            backgroundColor: '#fff',
                            color: '#3370ff',
                            borderRadius: 15,
                        }}
                    />
                ) : (
                    ''
                )}
            </Wrap>
        </Popover>
    );
}
