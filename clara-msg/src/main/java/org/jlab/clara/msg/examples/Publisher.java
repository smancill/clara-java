/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.examples;

import org.jlab.clara.msg.core.Actor;
import org.jlab.clara.msg.core.Connection;
import org.jlab.clara.msg.core.Message;
import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.RegInfo;

/**
 * An example of a publisher that publishes data for ever.
 * It does not matter who is subscribing to the messages.
 * This publisher uses the default proxy running on localhost.
 *
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
        try (Publisher publisher = new Publisher()) {

            // build the publishing topic (hard-coded)
            String domain = "test_domain";
            String subject = "test_subject";
            String type = "test_type";
            String description = "test_description";
            Topic topic = Topic.build(domain, subject, type);

            // register this publisher
            publisher.register(RegInfo.publisher(topic, description));

            // get the data size to be sent periodically
            int dataSize = Integer.parseInt(args[0]);
            System.out.printf("Byte array size = %d%n", dataSize);

            // create a byte array the required size and set it in a new message
            byte[] b = new byte[dataSize];

            // create the data message to the specified topic
            Message msg = new Message(topic, "data/binary", b);

            System.out.printf("Publishing to = %s%n", topic);

            // connect to the local proxy
            try (Connection con = publisher.getConnection()) {
                // publish data for ever
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
