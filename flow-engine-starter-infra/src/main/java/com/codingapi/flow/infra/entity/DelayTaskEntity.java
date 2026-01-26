package com.codingapi.flow.infra.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "t_flow_delay_task")
@Data
public class DelayTaskEntity {

    @Id
    private  String id;
    private  Long createTime;
    private  Long triggerTime;
    private  Long currentRecordId;
    private  String workCode;
    private  String delayNodeId;
}
