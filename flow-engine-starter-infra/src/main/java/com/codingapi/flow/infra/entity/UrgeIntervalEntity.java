package com.codingapi.flow.infra.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "t_flow_urge_interval")
@Data
public class UrgeIntervalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String processId;
    private Long recordId;
    private Long createTime;

}
