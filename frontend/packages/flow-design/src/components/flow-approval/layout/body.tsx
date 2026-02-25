import React from "react";
import {Col, Row} from "antd";
import {FormViewComponent} from "@/components/flow-approval/components/form-view-component";
import {FlowNodeHistory, FlowNodeHistoryAction} from "@/components/flow-approval/components/flow-node-history";

export const Body = () => {
    const flowNodeHistoryAction = React.useRef<FlowNodeHistoryAction>(null);

    const handleValuesChange = (values:any) => {
        flowNodeHistoryAction.current?.refresh();
    }

    return (
        <Row>
            <Col span={18}>
                表单详情
                <FormViewComponent
                    onValuesChange={handleValuesChange}
                />
            </Col>
            <Col span={6}>
                <FlowNodeHistory actionRef={flowNodeHistoryAction}/>
            </Col>
        </Row>
    )
}