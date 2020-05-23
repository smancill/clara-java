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

import org.jlab.clara.msg.data.RegDataProto.RegData;
import org.jlab.clara.msg.data.RegInfo;
import org.jlab.clara.msg.data.RegQuery;
import org.jlab.clara.msg.net.RegAddress;
import org.jlab.clara.msg.sys.ConnectionFactory;
import org.jlab.clara.msg.sys.regdis.RegDataFactory;
import org.jlab.clara.msg.sys.regdis.RegDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.jlab.clara.msg.data.RegDataProto.RegData.OwnerType.PUBLISHER;
import static org.jlab.clara.msg.data.RegDataProto.RegData.OwnerType.SUBSCRIBER;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ActorTest {

    private RegDriver driver;
    private Actor core;

    private final String name = "asimov";
    private final Topic topic = Topic.wrap("writer:scifi:book");
    private final RegAddress regAddr = new RegAddress();

    @BeforeEach
    public void setup() throws Exception {
        ConnectionFactory factory = mock(ConnectionFactory.class);
        ActorSetup setup = ActorSetup.newBuilder()
                                     .withRegistrar(regAddr)
                                     .withPoolSize(1)
                                     .build();

        driver = mock(RegDriver.class);
        core = new Actor(name, setup, factory);

        doReturn(new RegAddress()).when(driver).getAddress();
        doReturn(driver).when(factory).createRegistrarConnection(any(RegAddress.class));
    }


    @Test
    public void registerPublisher() throws Exception {
        core.register(RegInfo.publisher(topic, "test pub"), regAddr, 1000);

        RegData.Builder expected = createRegistration(PUBLISHER, topic);
        expected.setDescription("test pub");

        verify(driver).addRegistration(eq(name), eq(expected.build()), eq(1000L));
    }


    @Test
    public void registerSubscriber() throws Exception {
        core.register(RegInfo.subscriber(topic, "test sub"), regAddr, 1000);

        RegData.Builder expected = createRegistration(SUBSCRIBER, topic);
        expected.setDescription("test sub");

        verify(driver).addRegistration(eq(name), eq(expected.build()), eq(1000L));
    }


    @Test
    public void removePublisher() throws Exception {
        core.deregister(RegInfo.publisher(topic), regAddr, 1500);

        RegData.Builder expected = createRegistration(PUBLISHER, topic);

        verify(driver).removeRegistration(eq(name), eq(expected.build()), eq(1500L));
    }


    @Test
    public void removeSubscriber() throws Exception {
        core.deregister(RegInfo.subscriber(topic), regAddr, 1500);

        RegData.Builder expected = createRegistration(SUBSCRIBER, topic);

        verify(driver).removeRegistration(eq(name), eq(expected.build()), eq(1500L));
    }


    @Test
    public void findPublishers() throws Exception {
        RegQuery query = RegQuery.publishers(topic);

        core.discover(query, regAddr, 2000);

        verify(driver).findRegistration(eq(name), eq(query.data().build()), eq(2000L));
    }


    @Test
    public void findSubscribers() throws Exception {
        RegQuery query = RegQuery.subscribers(topic);

        core.discover(query, regAddr, 2000);

        verify(driver).findRegistration(eq(name), eq(query.data().build()), eq(2000L));
    }


    @Test
    public void filterPublishers() throws Exception {
        RegQuery query = RegQuery.publishers().withDomain("domain");

        core.discover(query, regAddr, 2000);

        verify(driver).filterRegistration(eq(name), eq(query.data().build()), eq(2000L));
    }


    @Test
    public void filterSubscribers() throws Exception {
        RegQuery query = RegQuery.subscribers().withSubject("subject");

        core.discover(query, regAddr, 2000);

        verify(driver).filterRegistration(eq(name), eq(query.data().build()), eq(2000L));
    }


    @Test
    public void allPublishers() throws Exception {
        RegQuery query = RegQuery.publishers().all();

        core.discover(query, regAddr, 2000);

        verify(driver).allRegistration(eq(name), eq(query.data().build()), eq(2000L));
    }


    @Test
    public void allSubscribers() throws Exception {
        RegQuery query = RegQuery.subscribers().all();

        core.discover(query, regAddr, 2000);

        verify(driver).allRegistration(eq(name), eq(query.data().build()), eq(2000L));
    }


    private RegData.Builder createRegistration(RegData.OwnerType regType, Topic topic) {
        return RegDataFactory.newRegistration(name, regType, topic.toString());
    }
}
