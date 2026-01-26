package com.codingapi.flow.infra.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "t_flow_parallel_control")
@Data
public class ParallelControlEntity {

    @Id
    private String id;
    private Integer count;
}
