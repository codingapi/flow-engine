package com.codingapi.example.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "t_node_view_javascript")
public class NodeViewJavaScriptEntity {

    /**
     * 代码唯一标识
     */
    @Id
    private String code;
    /**
     * 代码内容
     */
    @Lob
    private String script;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 更新时间
     */
    private Long updateTime;
}
