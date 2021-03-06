/*
 * Copyright (C) 2016. Jefferson Lab (JLAB). All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for governmental use, educational, research, and not-for-profit
 * purposes, without fee and without a signed licensing agreement.
 *
 * IN NO EVENT SHALL JLAB BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL,
 * INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF
 * THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JLAB HAS BEEN ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JLAB SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE. THE CLARA SOFTWARE AND ACCOMPANYING DOCUMENTATION, IF ANY, PROVIDED
 * HEREUNDER IS PROVIDED "AS IS". JLAB HAS NO OBLIGATION TO PROVIDE MAINTENANCE,
 * SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * This software was developed under the United States Government License.
 * For more information contact author at gurjyan@jlab.org
 * Department of Experimental Nuclear Physics, Jefferson Lab.
 */

package org.jlab.clara.msg.examples;

import org.jlab.clara.msg.core.Actor;
import org.jlab.clara.msg.core.ActorUtils;
import org.jlab.clara.msg.core.Connection;
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
        try (SyncPublisher publisher = new SyncPublisher()) {

            // build the publishing topic (hard-coded)
            final String domain = "test_domain";
            final String subject = "test_subject";
            final String type = "test_type";
            final String description = "test_description";
            Topic topic = Topic.build(domain, subject, type);

            // register this publisher
            publisher.register(RegInfo.publisher(topic, description));

            // create a simple message
            Message msg = Message.createFrom(topic, 111);

            // connect to the local proxy
            try (Connection con = publisher.getConnection()) {
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
