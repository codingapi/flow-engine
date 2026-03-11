package com.codingapi.example.handler;

import com.codingapi.flow.event.FlowRecordUrgeEvent;
import com.codingapi.springboot.framework.event.IHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MyFlowRecordUrgeEventHandler implements IHandler<FlowRecordUrgeEvent> {

    @Override
    public void handler(FlowRecordUrgeEvent event) {
        log.info("催办 event:{}",event);
    }
}
