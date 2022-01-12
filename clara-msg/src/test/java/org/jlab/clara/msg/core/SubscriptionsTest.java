/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.core;

import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.sys.ProxyWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Simple pub-sub integration tests. Ensure that the subscription is running,
 * that a publisher can communicate with a subscriber through the same proxy,
 * and that all messages are published and received.
 * <p>
 * When counting published messages, each one contains an integer from a unique
 * sequence of integers, and their sum is used to check that all messages were
 * delivered.
 * <p>
 * For more complex cases see {@link PublishersTest} and {@link SyncPublishTest}.
 */
@Tag("integration")
public class SubscriptionsTest {

    private ProxyWrapper proxyThread;

    @BeforeEach
    public void setup() {
        proxyThread = new ProxyWrapper();
    }


    @AfterEach
    public void tearDown() {
        proxyThread.close();
    }


    @Test
    public void unsubscribeStopsThread() throws Exception {
        try (var actor = new Actor("test")) {
            var subscription = actor.subscribe(Topic.wrap("topic"), null);
            ActorUtils.sleep(1000);
            actor.unsubscribe(subscription);

            assertFalse(subscription.isAlive());
        }
    }


    @Test
    public void subscribeReceivesAllMessages() throws Exception {
        class Check {
            static final int N = 10000;
            static final long SUM_N = 49995000L;

            final AtomicInteger counter = new AtomicInteger();
            final AtomicLong sum = new AtomicLong();
        }

        final var check = new Check();

        var subThread = ActorUtils.newThread("sub-thread", () -> {
            try (var actor = new Actor("test_subscriber")) {
                var topic = Topic.wrap("test_topic");
                var sub = actor.subscribe(topic, msg -> {
                    int i = Message.parseData(msg, Integer.class);
                    check.counter.incrementAndGet();
                    check.sum.addAndGet(i);
                });
                var shutdownCounter = 0;
                while (check.counter.get() < Check.N && shutdownCounter < 100) {
                    shutdownCounter++;
                    ActorUtils.sleep(100);
                }
                actor.unsubscribe(sub);
            } catch (ClaraMsgException e) {
                e.printStackTrace();
            }
        });
        subThread.start();
        ActorUtils.sleep(100);

        var pubThread = ActorUtils.newThread("pub-thread", () -> {
            try (var actor = new Actor("test_publisher");
                 var con = actor.getConnection()) {
                var topic = Topic.wrap("test_topic");
                for (int i = 0; i < Check.N; i++) {
                    var msg = Message.createFrom(topic, i);
                    actor.publish(con, msg);
                }
            } catch (ClaraMsgException e) {
                e.printStackTrace();
            }
        });
        pubThread.start();

        subThread.join();
        pubThread.join();

        assertThat(check.counter.get(), is(Check.N));
        assertThat(check.sum.get(), is(Check.SUM_N));
    }


    @Test
    public void syncPublicationReceivesAllResponses() throws Exception {
        class Check {
            static final int N = 100;
            static final long SUM_N = 4950L;

            int counter = 0;
            long sum = 0;
        }

        final var check = new Check();

        var pubThread = ActorUtils.newThread("sync-pub-thread", () -> {
            try (var subActor = new Actor("test_subscriber");
                 var pubActor = new Actor("test_publisher")) {
                var subTopic = Topic.wrap("test_topic");
                subActor.subscribe(subTopic, msg -> {
                    try {
                        subActor.publish(Message.createResponse(msg));
                    } catch (ClaraMsgException e) {
                        e.printStackTrace();
                    }
                });
                ActorUtils.sleep(100);
                try (var pubCon = subActor.getConnection()) {
                    var pubTopic = Topic.wrap("test_topic");
                    for (int i = 0; i < Check.N; i++) {
                        var msg = Message.createFrom(pubTopic, i);
                        var rMsg = pubActor.syncPublish(pubCon, msg, 1000);
                        int rData = Message.parseData(rMsg, Integer.class);
                        check.sum += rData;
                        check.counter++;
                    }
                }
            } catch (ClaraMsgException | TimeoutException e) {
                e.printStackTrace();
            }
        });
        pubThread.start();
        pubThread.join();

        assertThat(check.counter, is(Check.N));
        assertThat(check.sum, is(Check.SUM_N));
    }


    @Test
    public void syncPublicationThrowsOnTimeout() throws Exception {
        class Check {
            boolean received = false;
            boolean timeout = false;
        }

        final var check = new Check();

        var pubThread = ActorUtils.newThread("sync-pub-thread", () -> {
            try (var subActor = new Actor("test_subscriber");
                 var pubActor = new Actor("test_publisher")) {
                var subTopic = Topic.wrap("test_topic");
                var sub = subActor.subscribe(subTopic, msg -> {
                    try {
                        check.received = true;
                        ActorUtils.sleep(1500);
                        subActor.publish(Message.createResponse(msg));
                    } catch (ClaraMsgException e) {
                        e.printStackTrace();
                    }
                });
                ActorUtils.sleep(100);
                try {
                    var pubTopic = Topic.wrap("test_topic");
                    var msg = Message.createFrom(pubTopic, 1);
                    pubActor.syncPublish(msg, 1000);
                } catch (TimeoutException e) {
                    check.timeout = true;
                }
                subActor.unsubscribe(sub);
            } catch (ClaraMsgException e) {
                e.printStackTrace();
            }
        });
        pubThread.start();
        pubThread.join();

        assertTrue(check.received, "Received message");
        assertTrue(check.timeout, "Timeout reached");
    }

    @Test
    public void multiTopicSubscriptionReceivesAllMessages() throws Exception {
        class Check {
            static final int N = 10000;
            static final long SUM_N = 49995000L;

            final AtomicInteger counter = new AtomicInteger();
            final AtomicLong sum = new AtomicLong();
        }

        final var check = new Check();

        var subThread = ActorUtils.newThread("sub-thread", () -> {
            try (var actor = new Actor("test_subscriber")) {
                var topics = new HashSet<Topic>();
                topics.add(Topic.wrap("1_test_topic"));
                topics.add(Topic.wrap("2_test_topic"));
                var sub = actor.subscribe(topics, msg -> {
                    int i = Message.parseData(msg, Integer.class);
                    check.counter.incrementAndGet();
                    check.sum.addAndGet(i);
                });
                var shutdownCounter = 0;
                while (check.counter.get() < Check.N && shutdownCounter < 100) {
                    shutdownCounter++;
                    ActorUtils.sleep(100);
                }
                actor.unsubscribe(sub);
            } catch (ClaraMsgException e) {
                e.printStackTrace();
            }
        });
        subThread.start();
        ActorUtils.sleep(100);

        var pubThread = ActorUtils.newThread("pub-thread", () -> {
            try (var actor = new Actor("test_publisher");
                 var con = actor.getConnection()) {
                var topics = new Topic[] {
                    Topic.wrap("1_test_topic"),
                    Topic.wrap("2_test_topic")
                };
                for (int i = 0; i < Check.N; i++) {
                    var msg = Message.createFrom(topics[i % 2], i);
                    actor.publish(con, msg);
                }
            } catch (ClaraMsgException e) {
                e.printStackTrace();
            }
        });
        pubThread.start();

        subThread.join();
        pubThread.join();

        assertThat(check.counter.get(), is(Check.N));
        assertThat(check.sum.get(), is(Check.SUM_N));
    }
}
