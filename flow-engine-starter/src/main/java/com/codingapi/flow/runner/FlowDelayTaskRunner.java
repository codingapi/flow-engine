package com.codingapi.flow.runner;

import com.codingapi.flow.domain.DelayTaskManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

public class FlowDelayTaskRunner implements ApplicationRunner, DisposableBean {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        DelayTaskManager.getInstance().start();
    }

    @Override
    public void destroy() throws Exception {
        DelayTaskManager.getInstance().close();
    }
}
