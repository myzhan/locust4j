package com.github.myzhan.locust4j.runtime;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.github.myzhan.locust4j.message.Message;
import com.github.myzhan.locust4j.rpc.Client;

/**
 * @author myzhan
 * @date 2018/12/06
 */
public class MockRPCClient implements Client {

    private BlockingQueue<Message> toServerQueue;
    private BlockingQueue<Message> fromServerQueue;

    public MockRPCClient() {
        toServerQueue = new LinkedBlockingQueue<Message>();
        fromServerQueue = new LinkedBlockingQueue<Message>();
    }

    public Message recv() {
        try {
            Message message = fromServerQueue.take();
            return message;
        } catch (Exception ex) {
            return null;
        }
    }

    public void send(Message message) {
        System.out.println("recv: " + message);
        toServerQueue.add(message);
    }

    public void close() {
        // null
    }

    public BlockingQueue<Message> getToServerQueue() {
        return this.toServerQueue;
    }

    public BlockingQueue<Message> getFromServerQueue() {
        return this.fromServerQueue;
    }
}
