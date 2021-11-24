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
    protected long occurrences;

    protected StatsError(String name, String method, String error) {
        this.name = name;
        this.method = method;
        this.error = error;
    }

    protected void occured() {
        this.occurrences++;
    }

    protected Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>(5);
        m.put("name", this.name);
        m.put("method", this.method);
        m.put("error", this.error);
        m.put("occurrences", this.occurrences);
        return m;
    }

    private void reset() {
        this.occurrences = 0;
    }

    protected Map<String, Object> getStrippedReport() {
        Map<String, Object> report = this.toMap();
        this.reset();
        return report;
    }
}
