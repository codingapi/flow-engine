package com.codingapi.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "t_groovy_script_temp")
@NoArgsConstructor
public class TempGroovyScriptEntity {

    /**
     * 脚本唯一编码
     */
    @Id
    @Column(name = "id")
    private String key;
    /**
     * 脚本内容
     */
    @Lob
    private String script;
    /**
     * 脚本描述信息
     */
    @Lob
    private String description;
    /**
     * 脚本函数名称
     */
    private String method;
    /**
     * 返回数据类型
     */
    private String returnType;
    /**
     * 绑定数据类型
     */
    @Lob
    private String binds;
    /**
     * 请求参数对象
     */
    @Lob
    private String requests;

    /**
     * 一级类型
     */
    private String typeOne;

    /**
     * 二级类型
     */
    private String typeTwo;

    /**
     * 备注信息
     */
    private String remark;

    /**
     * 绑定数据
     */
    private String tag;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 更新时间
     */
    private Long updateTime;

    /**
     * 清理时间
     */
    private Long clearTime;


}
