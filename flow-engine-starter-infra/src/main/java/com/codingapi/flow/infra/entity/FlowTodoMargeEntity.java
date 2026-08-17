package com.codingapi.flow.infra.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "t_flow_todo_marge")
public class FlowTodoMargeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "flow_todo_marge_seq")
    @SequenceGenerator(name = "flow_todo_marge_seq", sequenceName = "t_flow_todo_marge_seq", allocationSize = 50)
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
