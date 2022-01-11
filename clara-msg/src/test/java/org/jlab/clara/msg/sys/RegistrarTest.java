/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys;

import org.jlab.clara.msg.core.ActorUtils;
import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.RegDataProto.RegData;
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

    private final Random randomGen = new Random();

    @Test
    public void testRegistrationDataBase() throws Exception {
        RegistrarWrapper registrar = new RegistrarWrapper();
        try (registrar; Context context = Context.newContext()) {
            try {
                ConnectionFactory factory = new ConnectionFactory(context);
                driver = factory.createRegistrarConnection(new RegAddress());
                ActorUtils.sleep(200);

                long start = System.currentTimeMillis();

                addRandomActor(10000);
                checkActorsByTopic();

                removeRandomActor(2500);
                checkActorsByTopic();

                addRandomActor(1000);
                checkActorsByTopic();

                removeRandomHost();
                checkActorsByTopic();

                addRandomActor(1000);
                checkActorsByTopic();

                removeRandomActor(2500);
                checkActorsByFilter();

                addRandomActor(1000);
                checkActorsBySameTopic();

                removeRandomActor(2500);
                checkAllActors();

                removeAll();
                checkActorsByTopic();

                long end = System.currentTimeMillis();
                System.out.println("Total time: " + (end - start) / 1000.0);
            } finally {
                if (driver != null) {
                    driver.close();
                }
            }
        }
    }


    public void addRandomActor(int size) throws ClaraMsgException {
        System.out.println("INFO: Registering " + size + " random actors...");
        for (int i = 0; i < size; i++) {
            RegData data = RegDataFactory.randomRegistration();
            driver.addRegistration(name, data);
            registration.add(data);
        }
    }


    public void removeRandomActor(int size) throws ClaraMsgException {
        System.out.println("INFO: Removing " + size + " random actors...");

        int first = randomGen.nextInt(registration.size() - size);
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


    public void checkActorsByTopic() throws ClaraMsgException {
        checkMatchingTopic(OwnerType.PUBLISHER);
        checkMatchingTopic(OwnerType.SUBSCRIBER);
    }


    public void checkActorsByFilter() throws ClaraMsgException {
        checkFilter(OwnerType.PUBLISHER);
        checkFilter(OwnerType.SUBSCRIBER);
    }


    public void checkActorsBySameTopic() throws ClaraMsgException {
        checkSameTopic(OwnerType.PUBLISHER);
        checkSameTopic(OwnerType.SUBSCRIBER);
    }


    public void checkAllActors() throws ClaraMsgException {
        checkAll(OwnerType.PUBLISHER);
        checkAll(OwnerType.SUBSCRIBER);
    }


    private void checkMatchingTopic(OwnerType regType) throws ClaraMsgException {
        ResultAssert checker = new ResultAssert("topic", regType);
        for (Topic topic : testTopics()) {
            RegData data = queryFactory(regType).matching(topic).data();

            Set<RegData> result = driver.findRegistration(name, data);
            Set<RegData> expected = findLocal(regType, matchTopic(regType, topic));

            checker.assertThat(topic, result, expected);
        }
    }


    private static Predicate<RegData> matchTopic(OwnerType regType, Topic topic) {
        if (regType == OwnerType.PUBLISHER) {
            return r -> topic.isParent(getTopic(r));
        } else {
            return r -> getTopic(r).isParent(topic);
        }
    }


    private void checkFilter(OwnerType regType) throws ClaraMsgException {
        checkFilterByDomain(regType);
        checkFilterBySubject(regType);
        checkFilterByType(regType);
        checkFilterByHost(regType);
    }


    private void checkFilterByDomain(OwnerType regType) throws ClaraMsgException {
        ResultAssert checker = new ResultAssert("domain", regType);
        for (String domain : testDomains()) {
            RegData data = queryFactory(regType).withDomain(domain).data();

            Set<RegData> result = driver.filterRegistration(name, data);
            Set<RegData> expected = findLocal(regType, e -> e.getDomain().equals(domain));

            checker.assertThat(domain, result, expected);
        }
    }


    private void checkFilterBySubject(OwnerType regType) throws ClaraMsgException {
        ResultAssert checker = new ResultAssert("subject", regType);
        for (String subject : testSubjects()) {
            RegData data = queryFactory(regType).withSubject(subject).data();

            Set<RegData> result = driver.filterRegistration(name, data);
            Set<RegData> expected = findLocal(regType, e -> e.getSubject().equals(subject));

            checker.assertThat(subject, result, expected);
        }
    }


    private void checkFilterByType(OwnerType regType) throws ClaraMsgException {
        ResultAssert checker = new ResultAssert("type", regType);
        for (String type : testTypes()) {
            RegData data = queryFactory(regType).withType(type).data();

            Set<RegData> result = driver.filterRegistration(name, data);
            Set<RegData> expected = findLocal(regType, e -> e.getType().equals(type));

            checker.assertThat(type, result, expected);
        }
    }


    private void checkFilterByHost(OwnerType regType) throws ClaraMsgException {
        ResultAssert checker = new ResultAssert("host", regType);
        for (String host : RegDataFactory.testHosts) {
            RegData data = queryFactory(regType).withHost(host).data();

            Set<RegData> result = driver.filterRegistration(name, data);
            Set<RegData> expected = findLocal(regType, e -> e.getHost().equals(host));

            checker.assertThat(host, result, expected);
        }
    }


    private void checkSameTopic(OwnerType regType) throws ClaraMsgException {
        ResultAssert checker = new ResultAssert("topic", regType);
        for (Topic topic : testTopics()) {
            RegData data = queryFactory(regType).withSame(topic).data();

            Set<RegData> result = driver.sameRegistration(name, data);
            Set<RegData> expected = findLocal(regType, r -> getTopic(r).equals(topic));

            checker.assertThat(topic, result, expected);
        }
    }


    private void checkAll(OwnerType regType) throws ClaraMsgException {
        RegData data = queryFactory(regType).all().data();

        Set<RegData> result = driver.filterRegistration(name, data);
        Set<RegData> expected = findLocal(regType, e -> true);

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


    private Set<RegData> findLocal(OwnerType regType, Predicate<RegData> predicate) {
        return registration.stream()
                           .filter(r -> r.getOwnerType() == regType)
                           .filter(predicate)
                           .collect(Collectors.toSet());
    }


    private static Topic getTopic(RegData reg) {
        return Topic.build(reg.getDomain(), reg.getSubject(), reg.getType());
    }


    private static RegQuery.Factory queryFactory(OwnerType regType) {
        if (regType == OwnerType.PUBLISHER) {
            return RegQuery.publishers();
        } else {
            return RegQuery.subscribers();
        }
    }


    private static Topic[] testTopics() {
        return Stream.of(RegDataFactory.testTopics)
                     .map(Topic::wrap)
                     .toArray(Topic[]::new);
    }


    private static String[] testDomains() {
        return Stream.of(RegDataFactory.testTopics)
                     .map(Topic::wrap)
                     .map(Topic::domain)
                     .filter(t -> !t.equals(Topic.ANY))
                     .distinct()
                     .toArray(String[]::new);
    }


    private static String[] testSubjects() {
        return Stream.of(RegDataFactory.testTopics)
                     .map(Topic::wrap)
                     .map(Topic::subject)
                     .filter(t -> !t.equals(Topic.ANY))
                     .distinct()
                     .toArray(String[]::new);
    }


    private static String[] testTypes() {
        return Stream.of(RegDataFactory.testTopics)
                     .map(Topic::wrap)
                     .map(Topic::type)
                     .filter(t -> !t.equals(Topic.ANY))
                     .distinct()
                     .toArray(String[]::new);
    }


    private record ResultAssert(String valueType, OwnerType regType) {

        void assertThat(Object value, Set<RegData> result, Set<RegData> expected) {
            if (result.equals(expected)) {
                String owner = regType == OwnerType.PUBLISHER ? "publishers" : "subscribers";
                System.out.printf("Found %3d %s with %s %s%n",
                                  result.size(), owner, valueType, value);
            } else {
                String outName = valueType.substring(0, 1).toUpperCase() + valueType.substring(1);
                System.out.println(outName + ": " + value);
                System.out.println("Result: " + result.size());
                System.out.println("Expected: " + expected.size());
                fail("Sets don't match!!!");
            }
        }
    }
}
