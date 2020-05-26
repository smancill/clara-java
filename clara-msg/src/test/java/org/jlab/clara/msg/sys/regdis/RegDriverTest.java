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

package org.jlab.clara.msg.sys.regdis;

import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.RegDataProto.RegData;
import org.jlab.clara.msg.data.RegDataProto.RegData.Builder;
import org.jlab.clara.msg.data.RegQuery;
import org.jlab.clara.msg.net.RegAddress;
import org.jlab.clara.msg.net.SocketFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.jlab.clara.msg.sys.regdis.RegDataFactory.newRegistration;

import static org.jlab.clara.msg.data.RegDataProto.RegData.OwnerType.PUBLISHER;
import static org.jlab.clara.msg.data.RegDataProto.RegData.OwnerType.SUBSCRIBER;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class RegDriverTest {

    private RegDriver driver;
    private String sender = "testSender";

    private final String topic = "writer:scifi:books";


    @BeforeEach
    public void setup() throws Exception {
        RegAddress address = new RegAddress("10.2.9.1");
        SocketFactory factory = mock(SocketFactory.class);
        driver = spy(new RegDriver(address, factory));
        setResponse(new RegResponse("", ""));
    }


    @Test
    public void sendPublisherRegistration() throws Exception {
        Builder publisher = newRegistration("bradbury_pub", PUBLISHER, topic);
        publisher.setDescription("bradbury books");

        driver.addRegistration(sender, publisher.build());

        assertRequest(publisher.build(),
                      RegConstants.REGISTER_PUBLISHER,
                      RegConstants.REGISTRATION_TIMEOUT);
    }


    @Test
    public void sendSubscriberRegistration() throws Exception {
        Builder subscriber = newRegistration("bradbury_sub", SUBSCRIBER, topic);
        subscriber.setDescription("bradbury books");

        driver.addRegistration(sender, subscriber.build());

        assertRequest(subscriber.build(),
                      RegConstants.REGISTER_SUBSCRIBER,
                      RegConstants.REGISTRATION_TIMEOUT);
    }


    @Test
    public void sendPublisherRemoval() throws Exception {
        Builder publisher = newRegistration("bradbury_pub", PUBLISHER, topic);

        driver.removeRegistration(sender, publisher.build());

        assertRequest(publisher.build(),
                      RegConstants.REMOVE_PUBLISHER,
                      RegConstants.REGISTRATION_TIMEOUT);
    }


    @Test
    public void sendSubscriberRemoval() throws Exception {
        Builder subscriber = newRegistration("bradbury_sub", SUBSCRIBER, topic);

        driver.removeRegistration(sender, subscriber.build());

        assertRequest(subscriber.build(),
                      RegConstants.REMOVE_SUBSCRIBER,
                      RegConstants.REGISTRATION_TIMEOUT);
    }


    @Test
    public void sendHostRemoval() throws Exception {
        driver.removeAllRegistration(sender, "10.2.9.1");

        assertRequest("10.2.9.1",
                      RegConstants.REMOVE_ALL_REGISTRATION,
                      RegConstants.REGISTRATION_TIMEOUT);
    }


    @Test
    public void sendPublisherFind() throws Exception {
        Builder data = RegQuery.publishers().matching(Topic.wrap(topic)).data();

        driver.findRegistration(sender, data.build());

        assertRequest(data.build(),
                      RegConstants.FIND_PUBLISHER,
                      RegConstants.DISCOVERY_TIMEOUT);
    }


    @Test
    public void sendSubscriberFind() throws Exception {
        Builder data = RegQuery.subscribers().matching(Topic.wrap(topic)).data();

        driver.findRegistration(sender, data.build());

        assertRequest(data.build(),
                      RegConstants.FIND_SUBSCRIBER,
                      RegConstants.DISCOVERY_TIMEOUT);
    }


    @Test
    public void sendPublisherFilter() throws Exception {
        Builder data = RegQuery.publishers().withDomain("domain").data();

        driver.filterRegistration(sender, data.build());

        assertRequest(data.build(),
                      RegConstants.FILTER_PUBLISHER,
                      RegConstants.DISCOVERY_TIMEOUT);
    }


    @Test
    public void sendSubscriberFilter() throws Exception {
        Builder data = RegQuery.subscribers().withDomain("domain").data();

        driver.filterRegistration(sender, data.build());

        assertRequest(data.build(),
                      RegConstants.FILTER_SUBSCRIBER,
                      RegConstants.DISCOVERY_TIMEOUT);
    }


    @Test
    public void sendPublisherAll() throws Exception {
        Builder data = RegQuery.publishers().all().data();

        driver.allRegistration(sender, data.build());

        assertRequest(data.build(),
                      RegConstants.ALL_PUBLISHER,
                      RegConstants.DISCOVERY_TIMEOUT);
    }


    @Test
    public void sendSubscriberAll() throws Exception {
        Builder data = RegQuery.subscribers().all().data();

        driver.allRegistration(sender, data.build());

        assertRequest(data.build(),
                      RegConstants.ALL_SUBSCRIBER,
                      RegConstants.DISCOVERY_TIMEOUT);
    }


    @Test
    public void getRegistration() throws Exception {
        Builder data = RegQuery.publishers(Topic.wrap(topic)).data();

        Builder pub1 = newRegistration("bradbury1", PUBLISHER, topic);
        Builder pub2 = newRegistration("bradbury2", PUBLISHER, topic);
        Set<RegData> regData = new HashSet<>(Arrays.asList(pub1.build(), pub2.build()));

        setResponse(new RegResponse("", "", regData));

        Set<RegData> regRes = driver.findRegistration(sender, data.build());

        assertThat(regRes, is(regData));
    }



    private void assertRequest(RegData data, String topic, long timeout)
            throws Exception {
        RegRequest request = new RegRequest(topic, sender, data);
        verify(driver).request(request, timeout);
    }


    private void assertRequest(String data, String topic, long timeout)
            throws Exception {
        RegRequest request = new RegRequest(topic, sender, data);
        verify(driver).request(request, timeout);
    }


    private void setResponse(RegResponse response) throws Exception {
        doReturn(response).when(driver).request(any(RegRequest.class), anyLong());
    }
}
