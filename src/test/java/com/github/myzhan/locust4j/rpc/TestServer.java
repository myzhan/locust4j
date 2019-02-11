package com.github.myzhan.locust4j.rpc;

import java.io.IOException;
import java.util.Arrays;

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
    private int bindPort;
    private ZMQ.Socket routerSocket;

    private Thread serverThread;

    public TestServer(String bindHost, int bindPort) {
        this.context = new ZContext();
        this.bindHost = bindHost;
        this.bindPort = bindPort;
    }

    public void start() {
        routerSocket = context.createSocket(ZMQ.ROUTER);
        routerSocket.bind(String.format("tcp://%s:%d", bindHost, bindPort));

        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        byte[] packet = routerSocket.recv();
                        if (Arrays.equals(packet, "testClient".getBytes())) {
                            routerSocket.sendMore(packet);
                            continue;
                        }
                        Message message = new Message(packet);
                        routerSocket.send(message.getBytes(), 0);
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
        routerSocket.close();
        context.close();
    }
}
