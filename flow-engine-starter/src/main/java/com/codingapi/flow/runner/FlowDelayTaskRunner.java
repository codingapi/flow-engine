package com.codingapi.flow.runner;

import com.codingapi.flow.domain.DelayTaskManager;
import com.codingapi.flow.session.IRepositoryHolder;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

@AllArgsConstructor
public class FlowDelayTaskRunner implements ApplicationRunner, DisposableBean {

    private final IRepositoryHolder repositoryHolder;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        DelayTaskManager.getInstance().start(repositoryHolder);
    }

    @Override
    public void destroy() throws Exception {
        DelayTaskManager.getInstance().close();
    }
}
