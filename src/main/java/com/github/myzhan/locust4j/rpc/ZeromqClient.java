package com.github.myzhan.locust4j.rpc;

import java.io.IOException;

import com.github.myzhan.locust4j.Log;
import com.github.myzhan.locust4j.message.Message;
import org.zeromq.ZMQ;

/**
 * Locust used to support both plain-socket and zeromq.
 * Since Locust 0.8, it removes the plain-socket implementation.
 *
 * Locust4j only supports zeromq.
 *
 * @author myzhan
 */
public class ZeromqClient implements Client {

    private ZMQ.Context context = ZMQ.context(2);
    private ZMQ.Socket pushSocket;
    private ZMQ.Socket pullSocket;

    public ZeromqClient(String host, int port) {
        pushSocket = context.socket(ZMQ.PUSH);
        pushSocket.connect(String.format("tcp://%s:%d", host, port));
        pullSocket = context.socket(ZMQ.PULL);
        pullSocket.connect(String.format("tcp://%s:%d", host, port + 1));

        Log.debug(String.format("Locust4j is connected to master(%s:%d|%d)", host, port, port + 1));
    }

    @Override
    public Message recv() throws IOException {
        byte[] bytes = this.pullSocket.recv();
        return new Message(bytes);
    }

    @Override
    public void send(Message message) throws IOException {
        byte[] bytes = message.getBytes();
        this.pushSocket.send(bytes);
    }

    @Override
    public void close() {
        pullSocket.close();
        pushSocket.close();
    }
}

