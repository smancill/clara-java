/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.core;

import org.jlab.clara.msg.data.RegInfo;
import org.jlab.clara.msg.data.RegQuery;
import org.jlab.clara.msg.data.RegRecord;
import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.RegAddress;
import org.jlab.clara.msg.sys.ProxyWrapper;
import org.jlab.clara.msg.sys.RegistrarWrapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


/**
 * Tests multi-thread sync-publication of messages to multiple registered
 * subscribers, distributed on multiple nodes (when running from command-line).
 * Uses N cores to concurrently send M messages to each subscriber.
 * <p>
 * The number of subscribers will be obtained from the registration database.
 * The publisher will use N threads in parallel, and each thread will be sending
 * a subset of M/N messages to each subscriber. A subscriber must receive and
 * respond all M messages.
 * <p>
 * The messages are a unique sequence of integers from 0 to M-1.
 * Their sum is used to check that all messages were delivered.
 *
 * @see SubscriptionsTest
 */
@Tag("integration")
public final class SyncPublishTest {

    private static final String TOPIC = "sync_pub_test";
    private static final int TIME_OUT = 1000;

    private static Actor listener(int poolSize, String name, RegAddress regAddress)
            throws ClaraMsgException {
        var actor = new Actor(name, regAddress, poolSize);
        try {
            var topic = Topic.build(TOPIC, name);
            actor.register(RegInfo.subscriber(topic, "test subscriber"));
            System.out.printf("Registered %s with %s%n", topic, regAddress);
            actor.subscribe(topic, msg -> {
                try {
                    actor.publish(Message.createResponse(msg));
                } catch (ClaraMsgException e) {
                    e.printStackTrace();
                }
            });
            System.out.printf("Using %d cores to reply requests...%n", poolSize);
        } catch (ClaraMsgException e) {
            actor.close();
            throw e;
        }
        return actor;
    }

    private static Result publisher(int cores, int numMessages, RegAddress regAddress)
            throws Exception {
        var threadPool = ActorUtils.newThreadPool(cores, "sync-pub-");

        try (var actor = new Actor("sync_tester", regAddress)) {
            var query = RegQuery.subscribers().withPrefix(TOPIC);
            var regListeners = actor.discover(query);
            var numListeners = regListeners.size();
            if (numListeners == 0) {
                throw new RuntimeException("no subscribers registered on" + regAddress);
            }

            var results = new Result(cores, numListeners, numMessages);

            System.out.printf("Found %d subscribers registered on %s%n",
                              numListeners, regAddress);
            System.out.printf("Using %d cores to send %d messages to every subscriber...%n",
                              cores, results.totalMessages);

            results.startClock();
            for (int i = 0; i < cores; i++) {
                final int start = i * results.chunkSize;
                final int end = start + results.chunkSize;
                threadPool.submit(() -> {
                    try {
                        for (int j = start; j < end; j++) {
                            for (RegRecord reg : regListeners) {
                                try (var pubCon = actor.getConnection(reg.address())) {
                                    var msg = Message.createFrom(reg.topic(), j);
                                    var rMsg = actor.syncPublish(pubCon, msg, TIME_OUT);
                                    int rData = Message.parseData(rMsg, Integer.class);
                                    results.add(rData);
                                } catch (TimeoutException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } catch (ClaraMsgException e) {
                        e.printStackTrace();
                    }
                });
            }

            threadPool.shutdown();
            if (!threadPool.awaitTermination(5, TimeUnit.MINUTES)) {
                throw new RuntimeException("execution pool did not terminate");
            } else {
                results.stopClock();
            }

            return results;
        }
    }

    private static final class Result {

        final int numListeners;
        final int chunkSize;
        final int totalMessages;
        final long totalSum;

        long startTime;
        long endTime;

        final AtomicLong sum = new AtomicLong();

        private Result(int cores, int numListeners, int numMessages) {
            this.numListeners = numListeners;
            this.chunkSize = numMessages / cores;
            this.totalMessages = chunkSize * cores;
            this.totalSum = getTotalSum(totalMessages) * numListeners;
        }

        private long getTotalSum(int numMessages) {
            return LongStream.range(0, numMessages).sum();
        }

        public void startClock() {
            startTime = System.currentTimeMillis();
        }

        public void stopClock() {
            endTime = System.currentTimeMillis();
        }

        public void add(int value) {
            sum.addAndGet(value);
        }

        public boolean check() {
            if (sum.get() == totalSum) {
                System.out.println("OK: all messages received.");
                System.out.println("Total messages: " + totalMessages * numListeners);

                double duration = (endTime - startTime) / 1000.0;
                double average = 1.0 * (endTime - startTime) / (totalMessages * numListeners);
                System.out.printf("Total time: %.2f [s]%n", duration);
                System.out.printf("Average time: %.2f [ms]%n", average);
                return true;
            } else {
                System.out.printf("ERROR: expected = %d  received = %d%n", totalSum, sum.get());
                return false;
            }
        }
    }

    @Test
    @SuppressWarnings("unused")
    public void run() throws Exception {
        var address = new RegAddress();
        try (var registrar = new RegistrarWrapper();
             var proxy = new ProxyWrapper()) {
            try (var actor1 = listener(1, "foo", address);
                 var actor2 = listener(1, "bar", address)) {
                ActorUtils.sleep(100);
                Result results = publisher(1, 1000, address);
                assertThat(results.sum.get(), is(results.totalSum));
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("usage:");
            System.out.println(" sync_pub <fe_host> <pool_size> listener");
            System.out.println(" sync_pub <fe_host> <cores> <num_msg>");
            System.exit(1);
        }

        String frontEnd = args[0];
        String cores = args[1];
        String command = args[2];

        try {
            var address = new RegAddress(frontEnd);
            if (command.equals("listener")) {
                var poolSize = Integer.parseInt(cores);
                var subscriber = SyncPublishTest.listener(poolSize, "local", address);
                try (subscriber) {
                    ActorUtils.keepAlive();
                }
            } else {
                var pubThreads = Integer.parseInt(cores);
                var totalMessages = Integer.parseInt(command);
                var success = SyncPublishTest.publisher(pubThreads, totalMessages, address)
                                             .check();
                if (!success) {
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
