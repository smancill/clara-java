/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.examples;

import org.jlab.clara.msg.core.Actor;
import org.jlab.clara.msg.core.Message;
import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.RegInfo;

/**
 * An example of a publisher that publishes data forever.
 * It does not matter who is subscribing to the messages.
 * This publisher uses the default proxy running on localhost.
 * <p>
 * Published data is a byte array with a specified size.
 */
public class Publisher extends Actor {

    Publisher() {
        super("test_publisher");
    }

    public static void main(String[] args) {

        if (args.length != 1) {
            System.err.println("Usage: publisher <data_size_bytes>");
            System.exit(1);
        }

        // create a publisher object
        try (var publisher = new Publisher()) {

            // build the publishing topic (hard-coded)
            var domain = "test_domain";
            var subject = "test_subject";
            var type = "test_type";
            var description = "test_description";
            var topic = Topic.build(domain, subject, type);

            // register this publisher
            publisher.register(RegInfo.publisher(topic, description));

            // get the data size to be sent periodically
            var dataSize = Integer.parseInt(args[0]);
            System.out.printf("Byte array size = %d%n", dataSize);

            // create a byte array the required size and set it in a new message
            var b = new byte[dataSize];

            // create the data message to the specified topic
            var msg = new Message(topic, "data/binary", b);

            System.out.printf("Publishing to = %s%n", topic);

            // connect to the local proxy
            try (var con = publisher.getConnection()) {
                // publish data forever
                while (true) {
                    publisher.publish(con, msg);
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Parameter must be an integer (the data size in bytes)!!");
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
