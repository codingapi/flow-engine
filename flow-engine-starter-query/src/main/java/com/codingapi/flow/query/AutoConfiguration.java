package com.codingapi.flow.query;

import com.codingapi.flow.infra.jpa.FlowRecordEntityRepository;
import com.codingapi.flow.infra.jpa.FlowTodoRecordEntityRepository;
import com.codingapi.flow.query.service.FlowRecordQueryServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.codingapi.flow.query")
public class AutoConfiguration {
    

    @Bean
    public FlowRecordQueryService flowRecordQueryService(FlowRecordEntityRepository flowRecordEntityRepository, FlowTodoRecordEntityRepository flowTodoRecordEntityRepository){
        return new FlowRecordQueryServiceImpl(flowRecordEntityRepository,flowTodoRecordEntityRepository);
    }
}
