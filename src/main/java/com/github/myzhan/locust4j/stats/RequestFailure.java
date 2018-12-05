package com.github.myzhan.locust4j.stats;

/**
 * @author myzhan
 * @date 2018/12/05
 */
public class RequestFailure {
    private String requestType;
    private String name;
    private long responseTime;
    private String error;

    public RequestFailure() {

    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
