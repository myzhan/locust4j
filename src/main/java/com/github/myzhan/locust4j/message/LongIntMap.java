package com.github.myzhan.locust4j.message;

import java.util.HashMap;
import java.util.Map;

/**
 * @author vrajat
 * @date 2018/12/04
 */
public class LongIntMap {
    protected Map<Long, Integer> internalStore;

    public LongIntMap() {
        internalStore = new HashMap<Long, Integer>(16);
    }

    public Integer get(Long k) {
        return internalStore.get(k);
    }

    public void add(Long k) {
        if (internalStore.containsKey(k)) {
            internalStore.put(k, internalStore.get(k) + 1);
        } else {
            internalStore.put(k, 1);
        }
    }

    @Override
    public String toString() {
        return this.internalStore.toString();
    }
}
