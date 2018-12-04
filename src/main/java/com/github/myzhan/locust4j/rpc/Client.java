package com.github.myzhan.locust4j.rpc;

import java.io.IOException;

import com.github.myzhan.locust4j.message.Message;

public interface Client {

    /**
     * receive message from master
     *
     * @return Message
     * @throws IOException
     */
    Message recv() throws IOException;

    /**
     * send message to master
     *
     * @param message
     * @throws IOException
     */
    void send(Message message) throws IOException;

    /**
     * close client
     */
    void close();

}