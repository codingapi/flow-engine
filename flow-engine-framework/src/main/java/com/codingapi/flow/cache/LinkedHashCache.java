package com.codingapi.flow.cache;

import java.util.LinkedHashMap;
import java.util.Map;

public  class LinkedHashCache<KEY, VALUE> {

    private final Map<KEY, VALUE> cache;

    public LinkedHashCache(int maxCacheSize) {
        this.cache = new LinkedHashMap<>(16,0.75f,true){
            @Override
            protected boolean removeEldestEntry(Map.Entry<KEY, VALUE> eldest) {
                return size() > maxCacheSize;
            }
        };
    }

    public int size(){
        return this.cache.size();
    }

    public void put(KEY key,VALUE value){
        this.cache.put(key, value);
    }

    public VALUE get(KEY key){
        return this.cache.get(key);
    }

    public void remove(KEY key){
        this.cache.remove(key);
    }

    public void clear(){
        this.cache.clear();
    }

}
