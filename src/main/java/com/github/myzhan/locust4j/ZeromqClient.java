package com.github.myzhan.locust4j;

import java.io.IOException;

import org.zeromq.ZMQ;

public class ZeromqClient implements Client {

    private ZMQ.Context context = ZMQ.context(2);
    private ZMQ.Socket pushSocket;
    private ZMQ.Socket pullSocket;

    protected ZeromqClient(String host, int port) {
        pushSocket = context.socket(ZMQ.PUSH);
        pushSocket.connect(String.format("tcp://%s:%d", host, port));

        pullSocket = context.socket(ZMQ.PULL);
        pullSocket.connect(String.format("tcp://%s:%d", host, port + 1));

        Locust.getInstance().submitToCoreThreadPool(new Sender(this));
        Locust.getInstance().submitToCoreThreadPool(new Receiver(this));

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
        if ("quit".equals(message.getType())) {
            Queues.DISCONNECTED_FROM_MASTER.offer(true);
        }
    }
}

