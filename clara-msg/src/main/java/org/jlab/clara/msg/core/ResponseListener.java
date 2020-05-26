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

package org.jlab.clara.msg.core;

import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.ProxyAddress;
import org.jlab.clara.msg.sys.ConnectionFactory;
import org.jlab.clara.msg.sys.pubsub.ProxyDriver;
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
        if (items.get(address) == null) {
            ProxyDriverSetup setup = ProxyDriverSetup.newBuilder().build();
            ProxyDriver connection = factory.createSubscriberConnection(address, setup);
            connection.subscribe(topic);
            if (!connection.checkSubscription(topic, setup.subscriptionTimeout())) {
                connection.close();
                throw new ClaraMsgException("could not subscribe to " + topic);
            }
            ProxyDriver value = items.putIfAbsent(address, connection);
            if (value != null) {
                connection.unsubscribe(topic);
                connection.close();
            }
        }
    }

    public Message waitMessage(String topic, long timeout) throws TimeoutException {
        int t = 0;
        while (t < timeout) {
            Message repMsg = responses.remove(topic);
            if (repMsg != null) {
                return repMsg;
            }
            ActorUtils.sleep(1);
            t += 1;
        }
        throw new TimeoutException("no response for timeout = " + t);
    }

    @Override
    public void handle(ZMsg rawMsg) throws ClaraMsgException {
        Message msg = new Message(rawMsg);
        responses.put(msg.getTopic().toString(), msg);
    }
}
