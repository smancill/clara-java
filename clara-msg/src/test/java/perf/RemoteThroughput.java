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

package perf;

import org.jlab.clara.msg.core.Actor;
import org.jlab.clara.msg.core.Connection;
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

        final String bindTo = argv[0];
        final int messageSize = Integer.parseInt(argv[1]);
        final long messageCount = Long.parseLong(argv[2]);

        final ProxyAddress address = new ProxyAddress(bindTo);

        try (Actor publisher = new Actor("thr_publisher");
             Connection con = publisher.getConnection(address)) {
            System.out.println("Publishing messages...");
            Topic topic = Topic.wrap("thr_topic");
            byte[] data = new byte[messageSize];
            for (int i = 0; i < messageCount; i++) {
                Message msg = new Message(topic, "data/binary", data);
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
