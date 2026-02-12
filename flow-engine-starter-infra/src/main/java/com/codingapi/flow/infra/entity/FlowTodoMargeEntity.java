package com.codingapi.flow.infra.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "t_flow_todo_marge")
public class FlowTodoMargeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 待办id
     */
    private Long todoId;
    /**
     * 待办记录id
     */
    private Long recordId;
    /**
     * 创建时间
     */
    private Long createTime;
}
