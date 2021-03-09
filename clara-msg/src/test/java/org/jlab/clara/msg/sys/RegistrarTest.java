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

package org.jlab.clara.msg.sys;

import org.jlab.clara.msg.core.ActorUtils;
import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.RegDataProto.RegData;
import org.jlab.clara.msg.data.RegDataProto.RegData.Builder;
import org.jlab.clara.msg.data.RegDataProto.RegData.OwnerType;
import org.jlab.clara.msg.data.RegQuery;
import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.Context;
import org.jlab.clara.msg.net.RegAddress;
import org.jlab.clara.msg.sys.regdis.RegDriver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

@Tag("integration")
public class RegistrarTest {

    private RegDriver driver;

    private final Set<RegData> registration = new HashSet<>();
    private final String name = "registrar_test";

    @Test
    public void testRegistrationDataBase() throws Exception {
        Context context = Context.newContext();
        Registrar registrar = null;
        try {
            try {
                registrar = new Registrar(context);
                registrar.start();
            } catch (ClaraMsgException e) {
                System.err.println(e.getMessage());
            }

            ConnectionFactory factory = new ConnectionFactory(context);
            driver = factory.createRegistrarConnection(new RegAddress());
            ActorUtils.sleep(200);

            long start = System.currentTimeMillis();

            addRandom(10000);
            check();

            removeRandom(2500);
            check();

            addRandom(1000);
            check();

            removeRandomHost();
            check();

            addRandom(1000);
            check();

            removeRandom(2500);
            filter();

            addRandom(1000);
            same();

            removeRandom(2500);
            all();

            removeAll();
            check();

            long end = System.currentTimeMillis();
            System.out.println("Total time: " + (end - start) / 1000.0);
        } finally {
            if (driver != null) {
                driver.close();
            }
            context.close();
            if (registrar != null) {
                registrar.shutdown();
            }
        }
    }


    public void addRandom(int size) throws ClaraMsgException {
        System.out.println("INFO: Registering " + size + " random actors...");
        for (int i = 0; i < size; i++) {
            Builder rndReg = RegDataFactory.randomRegistration();
            RegData data = rndReg.build();
            driver.addRegistration(name, data);
            registration.add(data);
        }
    }


    public void removeRandom(int size) throws ClaraMsgException {
        System.out.println("INFO: Removing " + size + " random actors...");

        int first = new Random().nextInt(registration.size() - size);
        int end = first + size;
        int i = 0;
        Iterator<RegData> it = registration.iterator();
        while (it.hasNext()) {
            if (i == end) {
                break;
            }
            RegData reg = it.next();
            if (i >= first) {
                it.remove();
                driver.removeRegistration(name, reg);
            }
            i++;
        }
    }


    public void removeRandomHost() throws ClaraMsgException {
        String host = RegDataFactory.random(RegDataFactory.testHosts);
        removeHost(host);
    }


    private void removeHost(String host) throws ClaraMsgException {
        System.out.println("INFO: Removing host " + host);
        registration.removeIf(r -> r.getHost().equals(host));
        driver.removeAllRegistration("test", host);
    }


    public void removeAll() throws ClaraMsgException {
        for (String host : RegDataFactory.testHosts) {
            driver.removeAllRegistration("test", host);
        }
        registration.clear();
    }


    public void check() throws ClaraMsgException {
        checkActors(OwnerType.PUBLISHER);
        checkActors(OwnerType.SUBSCRIBER);
    }


    public void filter() throws ClaraMsgException {
        filterActors(OwnerType.PUBLISHER);
        filterActors(OwnerType.SUBSCRIBER);
    }


    public void same() throws ClaraMsgException {
        compareActors(OwnerType.PUBLISHER);
        compareActors(OwnerType.SUBSCRIBER);
    }


    public void all() throws ClaraMsgException {
        allActors(OwnerType.PUBLISHER);
        allActors(OwnerType.SUBSCRIBER);
    }


    private void checkActors(OwnerType regType) throws ClaraMsgException {
        ResultAssert checker = new ResultAssert("topic", regType);
        for (String topic : RegDataFactory.testTopics) {
            Builder data = getQuery(regType).matching(Topic.wrap(topic)).data();
            Predicate<RegData> predicate = discoveryPredicate(regType, topic);

            Set<RegData> result = driver.findRegistration(name, data.build());
            Set<RegData> expected = find(regType, predicate);

            checker.assertThat(topic, result, expected);
        }
    }

    private Predicate<RegData> discoveryPredicate(OwnerType regType, String topic) {
        final Topic searchTopic = Topic.wrap(topic);
        if (regType == OwnerType.PUBLISHER) {
            return r -> searchTopic.isParent(getTopic(r));
        } else {
            return r -> getTopic(r).isParent(searchTopic);
        }
    }


    private void filterActors(OwnerType regType) throws ClaraMsgException {
        filterByDomain(regType);
        filterBySubject(regType);
        filterByType(regType);
        filterByHost(regType);
    }


    private void filterByDomain(OwnerType regType) throws ClaraMsgException {
        Set<String> domains = Stream.of(RegDataFactory.testTopics)
                                    .map(Topic::wrap)
                                    .map(Topic::domain)
                                    .filter(t -> !t.equals(Topic.ANY))
                                    .collect(Collectors.toSet());

        ResultAssert checker = new ResultAssert("domain", regType);
        for (String domain : domains) {
            Builder data = getQuery(regType).withDomain(domain).data();

            Set<RegData> result = driver.filterRegistration(name, data.build());
            Set<RegData> expected = find(regType, e -> e.getDomain().equals(domain));

            checker.assertThat(domain, result, expected);
        }
    }


    private void filterBySubject(OwnerType regType) throws ClaraMsgException {
        Set<String> subjects = Stream.of(RegDataFactory.testTopics)
                                      .map(Topic::wrap)
                                      .map(Topic::subject)
                                      .filter(t -> !t.equals(Topic.ANY))
                                      .collect(Collectors.toSet());

        ResultAssert checker = new ResultAssert("subject", regType);
        for (String subject : subjects) {
            Builder data = getQuery(regType).withSubject(subject).data();

            Set<RegData> result = driver.filterRegistration(name, data.build());
            Set<RegData> expected = find(regType, e -> e.getSubject().equals(subject));

            checker.assertThat(subject, result, expected);
        }
    }


    private void filterByType(OwnerType regType) throws ClaraMsgException {
        Set<String> types = Stream.of(RegDataFactory.testTopics)
                                  .map(Topic::wrap)
                                  .map(Topic::type)
                                  .filter(t -> !t.equals(Topic.ANY))
                                  .collect(Collectors.toSet());

        ResultAssert checker = new ResultAssert("type", regType);
        for (String type : types) {
            Builder data = getQuery(regType).withType(type).data();

            Set<RegData> result = driver.filterRegistration(name, data.build());
            Set<RegData> expected = find(regType, e -> e.getType().equals(type));

            checker.assertThat(type, result, expected);
        }
    }


    private void filterByHost(OwnerType regType) throws ClaraMsgException {
        ResultAssert checker = new ResultAssert("host", regType);
        for (String host : RegDataFactory.testHosts) {
            Builder data = getQuery(regType).withHost(host).data();

            Set<RegData> result = driver.filterRegistration(name, data.build());
            Set<RegData> expected = find(regType, e -> e.getHost().equals(host));

            checker.assertThat(host, result, expected);
        }
    }


    private void compareActors(OwnerType regType) throws ClaraMsgException {
        ResultAssert checker = new ResultAssert("topic", regType);
        for (String topic : RegDataFactory.testTopics) {
            Topic searchTopic = Topic.wrap(topic);
            Builder data = getQuery(regType).withSame(searchTopic).data();

            Set<RegData> result = driver.sameRegistration(name, data.build());
            Set<RegData> expected = find(regType, r -> getTopic(r).equals(searchTopic));

            checker.assertThat(topic, result, expected);
        }
    }


    private void allActors(OwnerType regType) throws ClaraMsgException {
        Builder data = getQuery(regType).all().data();

        Set<RegData> result = driver.filterRegistration(name, data.build());
        Set<RegData> expected = find(regType, e -> true);

        String owner = regType == OwnerType.PUBLISHER ? "publishers" : "subscribers";
        if (result.equals(expected)) {
            System.out.printf("Found %3d %s%n", result.size(), owner);
        } else {
            System.out.println("All: " + owner);
            System.out.println("Result: " + result.size());
            System.out.println("Expected: " + expected.size());
            fail("Sets doesn't match!!!");
        }
    }


    private Set<RegData> find(OwnerType regType,
                                       Predicate<RegData> predicate) {
        return registration.stream()
                           .filter(r -> r.getOwnerType() == regType)
                           .filter(predicate)
                           .collect(Collectors.toSet());
    }


    private Topic getTopic(RegData reg) {
        return Topic.build(reg.getDomain(), reg.getSubject(), reg.getType());
    }


    private RegQuery.Factory getQuery(OwnerType regType) {
        if (regType == OwnerType.PUBLISHER) {
            return RegQuery.publishers();
        } else {
            return RegQuery.subscribers();
        }
    }


    private static final class ResultAssert {

        private final String valueName;
        private final OwnerType regType;

        private ResultAssert(String valueName, OwnerType regType) {
            this.valueName = valueName;
            this.regType = regType;
        }

        private void assertThat(String data,
                                Set<RegData> result,
                                Set<RegData> expected) {
            if (result.equals(expected)) {
                String owner = regType == OwnerType.PUBLISHER ? "publishers" : "subscribers";
                System.out.printf("Found %3d %s with %s %s%n",
                                  result.size(), owner, valueName, data);
            } else {
                String outName = valueName.substring(0, 1).toUpperCase() + valueName.substring(1);
                System.out.println(outName + ": " + data);
                System.out.println("Result: " + result.size());
                System.out.println("Expected: " + expected.size());
                fail("Sets doesn't match!!!");
            }
        }
    }
}
