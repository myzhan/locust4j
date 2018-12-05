package com.github.myzhan.locust4j.stats;

/**
 * @author myzhan
 * @date 2018/12/05
 */
public class RequestSuccess {
    private String requestType;
    private String name;
    private long responseTime;
    private long contentLength;

    public RequestSuccess() {

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

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }
}
