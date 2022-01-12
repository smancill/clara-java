/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.core;

import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.ProxyAddress;
import org.jlab.clara.msg.sys.ConnectionFactory;
import org.jlab.clara.msg.sys.pubsub.ProxyDriverSetup;
import org.jlab.clara.msg.sys.pubsub.ProxyListener;
import org.zeromq.ZMsg;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;

class ResponseListener extends ProxyListener {

    private final ConnectionFactory factory;
    private final String topic;

    private final ConcurrentMap<String, Message> responses;

    ResponseListener(String id, ConnectionFactory factory) {
        super("poll-" + id, factory.getContext());
        this.factory = factory;
        this.topic = Topic.build("ret", id).toString();
        this.responses = new ConcurrentHashMap<>();
    }

    public void register(ProxyAddress address) throws ClaraMsgException {
        if (connections.get(address) == null) {
            var setup = ProxyDriverSetup.newBuilder().build();
            var connection = factory.createSubscriberConnection(address, setup);
            connection.subscribe(topic);
            if (!connection.checkSubscription(topic, setup.subscriptionTimeout())) {
                connection.close();
                throw new ClaraMsgException("could not subscribe to " + topic);
            }
            var prev = connections.putIfAbsent(address, connection);
            if (prev != null) {
                connection.unsubscribe(topic);
                connection.close();
            }
        }
    }

    public Message waitMessage(String topic, long timeout) throws TimeoutException {
        var t = 0;
        while (t < timeout) {
            var response = responses.remove(topic);
            if (response != null) {
                return response;
            }
            ActorUtils.sleep(1);
            t += 1;
        }
        throw new TimeoutException("no response for timeout = " + t);
    }

    @Override
    public void handle(ZMsg rawMsg) throws ClaraMsgException {
        var msg = new Message(rawMsg);
        responses.put(msg.getTopic().toString(), msg);
    }
}
