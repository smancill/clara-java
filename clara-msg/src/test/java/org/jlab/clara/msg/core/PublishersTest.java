/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.core;

import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.sys.ProxyWrapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


/**
 * Tests multi-thread publication of messages to a single subscriber.
 * Uses N cores to concurrently send a total of M messages.
 * <p>
 * It can be a single actor using N threads in parallel, or N parallel actors
 * using one thread each. Each thread will be sending a subset of M/N messages.
 * The subscriber must receive all M messages.
 * <p>
 * The messages are a unique sequence of integers from 0 to M-1.
 * Their sum is used to check that all messages were delivered.
 *
 * @see SubscriptionsTest
 */
@Tag("integration")
public class PublishersTest {

    @Test
    public void subscribeReceivesAllMessages() throws Exception {
        try (TestRunner test = new AsyncRunner(false)) {
            test.run(100_000, 8);
        }
    }

    @Test
    public void syncSubscribeReceivesAllMessages() throws Exception {
        try (TestRunner test = new SyncRunner(false)) {
            test.run(1000, 4);
        }
    }

    @Test
    public void subscribeReceivesAllMessagesSinglePublisher() throws Exception {
        try (TestRunner test = new AsyncRunner(true)) {
            test.run(100_000, 1);
        }
    }

    @Test
    public void syncSubscribeReceivesAllMessagesSinglePublisher() throws Exception {
        try (TestRunner test = new SyncRunner(true)) {
            test.run(1000, 1);
        }
    }


    private abstract static class TestRunner implements AutoCloseable {

        final String rawTopic = "test_topic";
        final ProxyWrapper proxyThread;
        final Actor pubActor;

        TestRunner(boolean singlePubActor) {
            this.proxyThread = new ProxyWrapper();
            this.pubActor = singlePubActor ? new Actor("test_publisher") : null;
        }

        abstract void receive(Actor actor, Message msg, Check check) throws Exception;

        abstract void publish(Actor actor, Message msg, Check check) throws Exception;

        void run(int totalMessages, int numPublishers) throws Exception {

            final var check = new Check(totalMessages);
            final var subReady = new CountDownLatch(1);

            var subThread = ActorUtils.newThread("sub-thread", () -> {
                try (var actor = new Actor("test_subscriber")) {
                    var topic = Topic.wrap(rawTopic);
                    var sub = actor.subscribe(topic, msg -> {
                        try {
                            receive(actor, msg, check);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    subReady.countDown();
                    wait(check);
                    actor.unsubscribe(sub);
                } catch (ClaraMsgException e) {
                    e.printStackTrace();
                }
            });
            subThread.start();
            subReady.await();

            final int numMessages = check.n / numPublishers;
            List<Thread> pubThreads = new ArrayList<>(numPublishers);

            for (int i = 0; i < numPublishers; i++) {
                final int start = i * numMessages;
                final int end = start + numMessages;

                var pubThread = ActorUtils.newThread("pub-" + start, () -> {
                    var actor = pubActor;
                    try {
                        if (actor == null) {
                            actor = new Actor("test_publisher_" + start);
                        }
                        var topic = Topic.build(rawTopic, Integer.toString(start));
                        for (int j = start; j < end; j++) {
                            var msg = Message.createFrom(topic, j);
                            publish(actor, msg, check);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (actor != pubActor) {
                            actor.destroy();
                        }
                    }
                });
                pubThreads.add(pubThread);
                pubThread.start();
            }

            subThread.join();
            for (var pubThread : pubThreads) {
                pubThread.join();
            }

            assertThat(check.counter.get(), is(check.n));
            assertThat(check.sum.get(), is(check.total));
        }

        private void wait(Check check) {
            var counter = 0;
            while (check.counter.get() < check.n && counter < 20_000) {
                ActorUtils.sleep(100);
                counter += 100;
            }
        }

        @Override
        public void close() {
            try {
                if (pubActor != null) {
                    pubActor.destroy();
                }
                proxyThread.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private static class AsyncRunner extends TestRunner {

        AsyncRunner(boolean singlePubActor) {
            super(singlePubActor);
        }

        @Override
        void receive(Actor actor, Message msg, Check check) {
            int i = Message.parseData(msg, Integer.class);
            check.increment(i);
        }

        @Override
        void publish(Actor actor, Message msg, Check check) throws Exception {
            actor.publish(msg);
        }
    }


    private static class SyncRunner extends TestRunner {

        SyncRunner(boolean singlePubActor) {
            super(singlePubActor);
        }

        @Override
        void receive(Actor actor, Message msg, Check check) throws Exception {
            actor.publish(Message.createResponse(msg));
        }

        @Override
        void publish(Actor actor, Message msg, Check check) throws Exception {
            var rMsg = actor.syncPublish(msg, 1000);
            int rData = Message.parseData(rMsg, Integer.class);
            check.increment(rData);
        }
    }


    private static class Check {
        final int n;
        final long total;

        final AtomicInteger counter = new AtomicInteger();
        final AtomicLong sum = new AtomicLong();

        Check(int n) {
            this.n = n;
            this.total = LongStream.range(0, n).sum();
        }

        void increment(int i) {
            counter.incrementAndGet();
            sum.addAndGet(i);
        }
    }
}
