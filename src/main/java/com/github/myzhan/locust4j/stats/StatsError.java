package com.github.myzhan.locust4j.stats;

import java.util.HashMap;
import java.util.Map;

/**
 * @author myzhan
 * @date 2018/12/05
 */
public class StatsError {
    protected String name;
    protected String method;
    protected String error;
    protected long occurences;

    protected StatsError(String name, String method, String error) {
        this.name = name;
        this.method = method;
        this.error = error;
    }

    protected void occured() {
        this.occurences++;
    }

    protected Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<String, Object>(4);
        m.put("name", this.name);
        m.put("method", this.method);
        m.put("error", this.error);
        m.put("occurences", this.occurences);
        return m;
    }
}
