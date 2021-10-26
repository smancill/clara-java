/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package perf;

import java.util.concurrent.CountDownLatch;

import org.jlab.clara.msg.core.Actor;
import org.jlab.clara.msg.core.Subscription;
import org.jlab.clara.msg.core.Topic;

import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.ProxyAddress;

public final class LocalThroughput {

    private LocalThroughput() { }

    public static void main(String[] argv) {
        class Timer {
            int nr;
            long watch;
            long elapsed;
        }

        if (argv.length != 3) {
            printf("usage: local_thr <bind-to> <message-size> <message-count>\n");
            System.exit(1);
        }

        final String bindTo = argv[0];
        final int messageSize = Integer.parseInt(argv[1]);
        final long messageCount = Long.parseLong(argv[2]);

        final CountDownLatch finished = new CountDownLatch(1);
        final Timer timer = new Timer();

        try (Actor subscriber = new Actor("throughput_subscriber", 1)) {

            ProxyAddress address = new ProxyAddress(bindTo);
            Topic topic = Topic.wrap("thr_topic");

            Subscription sub = subscriber.subscribe(address, topic, msg -> {
                int size = msg.getDataSize();
                if (size != messageSize) {
                    printf("Message of incorrect size received " + size);
                    System.exit(1);
                }
                int nr = ++timer.nr;
                if (nr == 1) {
                    timer.watch = startClock();
                } else if (nr == messageCount) {
                    timer.elapsed = stopClock(timer.watch);
                    finished.countDown();
                }
            });

            System.out.println("Waiting for messages...");
            finished.await();

            if (timer.elapsed == 0) {
                timer.elapsed = 1;
            }

            long throughput = (long) (messageCount / (double) timer.elapsed * 1000000L);
            double megabits = (double) (throughput * messageSize * 8) / 1000000;
            double latency = (double) timer.elapsed / (messageCount);

            printf("Message elapsed: %.3f [s]%n", (double) timer.elapsed / 1000000L);
            printf("Message size: %d [B]%n", messageSize);
            printf("Message count: %d%n", (int) messageCount);
            printf("Mean transfer time: %.3f [us]%n", latency);
            printf("Mean transfer rate: %d [msg/s]%n", (int) throughput);
            printf("Mean throughput: %.3f [Mb/s]%n", megabits);

            subscriber.unsubscribe(sub);

        } catch (ClaraMsgException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }


    private static void printf(String string) {
        System.out.print(string);
    }


    private static void printf(String str, Object... args) {
        System.out.printf(str, args);
    }

    public static long startClock() {
        return System.nanoTime();
    }

    public static long stopClock(long watch) {
        return (System.nanoTime() - watch) / 1000;
    }
}
