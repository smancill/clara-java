/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.examples;

import org.jlab.clara.msg.core.Actor;
import org.jlab.clara.msg.core.ActorUtils;
import org.jlab.clara.msg.core.Message;
import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.RegInfo;

/**
 * An example of a publisher that sync publishes data for ever.
 * It does not matter who is subscribing to the messages, but subscriber
 * should be able to detect that the received message is a sync request and
 * respond back.
 * This publisher uses the default proxy running on localhost.
 *
 * Published data is a hard-coded integer.
 */
public class SyncPublisher extends Actor {

    SyncPublisher() {
        super("test_sync_publisher");
    }

    public static void main(String[] args) {
        try (var publisher = new SyncPublisher()) {

            // build the publishing topic (hard-coded)
            final var domain = "test_domain";
            final var subject = "test_subject";
            final var type = "test_type";
            final var description = "test_description";
            var topic = Topic.build(domain, subject, type);

            // register this publisher
            publisher.register(RegInfo.publisher(topic, description));

            // create a simple message
            var msg = Message.createFrom(topic, 111);

            // connect to the local proxy
            try (var con = publisher.getConnection()) {
                int counter = 1;
                while (true) {
                    System.out.println("Publishing " + counter);
                    long t1 = System.nanoTime();

                    //sync publish data. Note this will block for up to 5sec for data to arrive.
                    /* Message recData = */ publisher.syncPublish(con, msg, 5000);

                    long t2 = System.nanoTime();
                    double delta = (t2 - t1) / 1000000.0;
                    System.out.printf("Received response in %.3f ms%n", delta);
                    counter++;
                    ActorUtils.sleep(100);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
