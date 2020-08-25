package com.github.myzhan.locust4j.runtime;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.github.myzhan.locust4j.message.Message;
import com.github.myzhan.locust4j.rpc.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author myzhan
 */
public class MockRPCClient implements Client {

    private static final Logger logger = LoggerFactory.getLogger(MockRPCClient.class);

    private final BlockingQueue<Message> toServerQueue;
    private final BlockingQueue<Message> fromServerQueue;

    public MockRPCClient() {
        toServerQueue = new LinkedBlockingQueue<>();
        fromServerQueue = new LinkedBlockingQueue<>();
    }

    public Message recv() {
        try {
            return fromServerQueue.take();
        } catch (Exception ex) {
            return null;
        }
    }

    public void send(Message message) {
        logger.debug("recv: {}", message);
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
