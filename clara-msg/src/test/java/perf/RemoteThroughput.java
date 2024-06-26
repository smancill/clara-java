/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package perf;

import org.jlab.clara.msg.core.Actor;
import org.jlab.clara.msg.core.Message;
import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.Context;
import org.jlab.clara.msg.net.ProxyAddress;

public final class RemoteThroughput {

    private RemoteThroughput() { }

    public static void main(String[] argv) {
        if (argv.length != 3) {
            printf("usage: remote_thr <bind-to> <message-size> <message-count>\n");
            System.exit(1);
        }

        final var bindTo = argv[0];
        final var messageSize = Integer.parseInt(argv[1]);
        final var messageCount = Long.parseLong(argv[2]);

        final var address = new ProxyAddress(bindTo);

        try (var publisher = new Actor("thr_publisher");
             var con = publisher.getConnection(address)) {
            System.out.println("Publishing messages...");
            var topic = Topic.wrap("thr_topic");
            var data = new byte[messageSize];
            for (int i = 0; i < messageCount; i++) {
                var msg = new Message(topic, "data/binary", data);
                publisher.publish(con, msg);
            }
        } catch (ClaraMsgException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // wait until all messages are published
        Context.getInstance().destroy();
        System.out.println("Done!");
    }


    private static void printf(String string) {
        System.out.println(string);
    }
}
