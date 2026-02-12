package com.codingapi.flow.record;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 *  存储待办合并的记录，仅当开启流程记录合并时才会产生合并记录数据
 * {@link FlowRecord#isMergeable()} 为true时才会产生
 */
@Getter
@AllArgsConstructor
public class FlowTodoMarge {

    @Setter
    private long id;
    /**
     * 待办id
     */
    private long todoId;
    /**
     * 待办记录id
     */
    private long recordId;
    /**
     * 创建时间
     */
    private long createTime;

    public FlowTodoMarge(FlowTodoRecord margeRecord){
        this.todoId = margeRecord.getId();
        this.recordId = margeRecord.getRecordId();
        this.createTime = margeRecord.getCreateTime();
    }


    public boolean isRecord(long recordId) {
        return this.recordId == recordId;
    }
}
