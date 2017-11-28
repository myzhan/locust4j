package com.github.myzhan.locust4j;

import java.io.IOException;

public interface Client {

    /**
     * receive message from master
     * @return Message
     * @throws IOException
     */
    Message recv() throws IOException;

    /**
     * send message to master
     * @param message
     * @throws IOException
     */
    void send(Message message) throws IOException;

}

class Receiver implements Runnable {

    private Client client;

    protected Receiver(Client client) {
        this.client = client;
    }

    @Override
    public void run() {

        String name = Thread.currentThread().getName();
        Thread.currentThread().setName(name + "receive-from-master");

        while (true) {
            try {
                Message message = client.recv();
                Queues.MESSAGE_FROM_MASTER.offer(message);
            } catch (IOException ex) {
                Log.error(ex);
            }
        }
    }

}

class Sender implements Runnable {

    private Client client;

    protected Sender(Client client) {
        this.client = client;
    }

    @Override
    public void run() {

        String name = Thread.currentThread().getName();
        Thread.currentThread().setName(name + "send-to-master");

        while (true) {
            try {
                Message message = Queues.MESSAGE_TO_MASTER.take();
                this.client.send(message);
            } catch (InterruptedException ex) {
                Log.error(ex);
            } catch (IOException ex) {
                Log.error(ex);
            }
        }
    }

}