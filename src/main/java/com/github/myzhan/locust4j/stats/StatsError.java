package com.github.myzhan.locust4j.stats;

import java.util.HashMap;
import java.util.Map;

/**
 * @author myzhan
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
        Map<String, Object> m = new HashMap<>(5);
        m.put("name", this.name);
        m.put("method", this.method);
        m.put("error", this.error);

        // keep compatible with locust
        // https://github.com/locustio/locust/commit/f0a5f893734faeddb83860b2985010facc910d7d#diff-5d5f310549d6d596beaa43a1282ec49e
        m.put("occurences", this.occurences);
        m.put("occurrences", this.occurences);
        return m;
    }
}
