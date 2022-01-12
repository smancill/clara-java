/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.examples;

import com.google.protobuf.InvalidProtocolBufferException;
import org.jlab.clara.msg.core.Actor;
import org.jlab.clara.msg.core.ActorUtils;
import org.jlab.clara.msg.core.Callback;
import org.jlab.clara.msg.core.Message;
import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.MimeType;
import org.jlab.clara.msg.data.PlainDataProto.PlainData;
import org.jlab.clara.msg.data.RegInfo;
import org.jlab.clara.msg.errors.ClaraMsgException;

import java.util.List;

/**
 * An example of a subscriber. It will receive any message of the given topic
 * published by existing or new publishers.
 * It also includes an inner class presenting the callback to be executed at
 * every arrival of the data.
 */
public class Subscriber extends Actor {

    Subscriber() {
        super("test_subscriber", 1);
    }

    /**
     * Subscribes to a hard-coded topic on the local proxy,
     * and registers with the local registrar.
     */
    void start() throws ClaraMsgException {
        // build the subscribing topic (hard-coded)
        var domain = "test_domain";
        var subject = "test_subject";
        var type = "test_type";
        var description = "test_description";
        var topic = Topic.build(domain, subject, type);

        // subscribe to default local proxy
        subscribe(topic, new MyCallBack());

        // register with the local registrar
        register(RegInfo.subscriber(topic, description));

        System.out.printf("Subscribed to = %s%n", topic);
    }

    public static void main(String[] args) {
        try (var subscriber = new Subscriber()) {
            subscriber.start();
            ActorUtils.keepAlive();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Publishes a message using the same proxy used to receive the message.
     * This method is used in case the request (the publisher)
     * publishes data in sync ( required a response back).
     *
     * @param msg a received message
     */
    private void respondBack(Message msg, Object data) {
        try {
            publish(Message.createResponse(msg, data));
        } catch (ClaraMsgException e) {
            e.printStackTrace();
        }
    }

    /**
     * Private callback class.
     */
    private class MyCallBack implements Callback {

        // variables for naive benchmarking
        long nr = 0;
        long t1;
        long t2;

        @Override
        public void callback(Message msg) {
            if (!msg.getMetaData().hasReplyTo()) {
                // we get the data, but will not do anything with it for
                // communication benchmarking purposes.
                /* List<Integer> data = */ parseData(msg);

                if (nr == 0) {
                    t1 = System.currentTimeMillis();
                }
                nr = nr + 1;
                if (nr >= 10000) {
                    t2 = System.currentTimeMillis();
                    long dt = t2 - t1;
                    double pt = (double) dt / (double) nr;
                    long pr = (nr * 1000) / dt;
                    System.out.println();
                    System.out.printf("transfer time = %.3f [ms]%n", pt);
                    System.out.printf("transfer rate = %d [Hz]%n", pr);
                    nr = 0;
                }
            } else {
                // sends back "Done" string
                respondBack(msg, "Done");
            }
        }

        /**
         * De-serializes received message and retrieves List of integers
         * Note this method is not checking the metadata for the mimeType.
         *
         * @param msg a received message
         * @return data of the message, otherwise null
         */
        private List<Integer> parseData(Message msg) {
            try {
                var metadata = msg.getMetaData();
                if (metadata.getDataType().equals(MimeType.ARRAY_SFIXED32)) {
                    var data = PlainData.parseFrom(msg.getData());
                    return data.getFLSINT32AList();
                }
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
