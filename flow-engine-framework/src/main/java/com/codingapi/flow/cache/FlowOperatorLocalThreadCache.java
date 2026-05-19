package com.codingapi.flow.cache;

import com.codingapi.flow.operator.IFlowOperator;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class FlowOperatorLocalThreadCache {

    private final ThreadLocal<Map<Long, IFlowOperator>> cache;

    private FlowOperatorLocalThreadCache() {
        this.cache = new ThreadLocal<>();
    }

    @Getter
    private final static FlowOperatorLocalThreadCache instance = new FlowOperatorLocalThreadCache();


    public void clear() {
        this.cache.remove();
    }

    private Map<Long, IFlowOperator> getCache() {
        Map<Long, IFlowOperator> operatorCaches = this.cache.get();
        if (operatorCaches == null) {
            operatorCaches = new HashMap<>();
        }
        return operatorCaches;
    }

    public void sync(IFlowOperator flowOperator) {
        Map<Long, IFlowOperator> operatorCaches = this.getCache();
        operatorCaches.put(flowOperator.getUserId(), flowOperator);
        this.cache.set(operatorCaches);
    }

    public IFlowOperator get(long id, Supplier<IFlowOperator> operatorLoader) {
        Map<Long, IFlowOperator> operatorCaches = this.getCache();
        IFlowOperator operator = operatorCaches.get(id);
        if (operator != null) {
            return operator;
        }

        operator = operatorLoader.get();
        if (operator != null) {
            operatorCaches.put(operator.getUserId(), operator);
            this.cache.set(operatorCaches);
        }
        return operator;
    }


    public List<IFlowOperator> find(List<Long> ids, IFlowOperatorFinder flowOperatorFinder) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }

        ids = ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return null;
        }

        Map<Long, IFlowOperator> operatorCaches = this.getCache();

        List<IFlowOperator> operatorList = new ArrayList<>();
        for (long id : ids) {
            IFlowOperator operator = operatorCaches.get(id);
            if (operator != null) {
                operatorList.add(operator);
            }
        }
        List<Long> matchIds = ids.stream()
                .filter((id) -> !operatorCaches.containsKey(id))
                .toList();

        if (matchIds.isEmpty()) {
            return operatorList;
        }

        List<IFlowOperator> newOperatorList;
        if (operatorList.isEmpty()) {
            newOperatorList = flowOperatorFinder.find(ids);
        } else {
            List<Long> queryIds = ids.stream()
                    .filter((id) -> !operatorCaches.containsKey(id))
                    .toList();
            newOperatorList = flowOperatorFinder.find(queryIds);
        }

        if (newOperatorList != null && !newOperatorList.isEmpty()) {
            for (IFlowOperator operator : newOperatorList) {
                operatorCaches.put(operator.getUserId(), operator);
            }

            this.cache.set(operatorCaches);

            operatorList.addAll(newOperatorList);
        }

        return operatorList;
    }


    public interface IFlowOperatorFinder {

        List<IFlowOperator> find(List<Long> ids);

    }

}
