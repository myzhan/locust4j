package com.github.myzhan.locust4j.rpc;

import java.io.IOException;

import com.github.myzhan.locust4j.message.Message;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

/**
 * @author myzhan
 * @date 2018/12/05
 */
public class TestServer {

    private ZContext context;
    private String bindHost;
    private int pushPort;
    private int pullPort;
    private ZMQ.Socket pushSocket;
    private ZMQ.Socket pullSocket;
    private Thread serverThread;

    public TestServer(String bindHost, int pushPort, int pullPort) {
        this.context = new ZContext();
        this.bindHost = bindHost;
        this.pushPort = pushPort;
        this.pullPort = pullPort;
    }

    public void start() {
        pushSocket = context.createSocket(ZMQ.PUSH);
        pushSocket.bind(String.format("tcp://%s:%d", bindHost, pushPort));
        pullSocket = context.createSocket(ZMQ.PULL);
        pullSocket.bind(String.format("tcp://%s:%d", bindHost, pullPort));

        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        byte[] packet = pullSocket.recv();
                        Message message = new Message(packet);
                        pushSocket.send(message.getBytes());
                    }
                } catch (ZMQException ex) {
                    // ignore ZMQException, it may be interrupted.
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        serverThread.start();
    }

    public void stop() {
        serverThread.interrupt();
        pushSocket.close();
        pullSocket.close();
        context.close();
    }
}
