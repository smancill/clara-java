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
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
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
        var registrar = new RegistrarWrapper();
        try (registrar; var context = Context.newContext()) {
            try {
                var factory = new ConnectionFactory(context);
                driver = factory.createRegistrarConnection(new RegAddress());
                ActorUtils.sleep(200);

                var start = System.currentTimeMillis();

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

                var end = System.currentTimeMillis();
                System.out.printf("Total time: %.3f s%n", ((end - start) / 1000.0));
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
            var data = RegDataFactory.randomRegistration();
            driver.addRegistration(name, data);
            registration.add(data);
        }
    }


    public void removeRandomActor(int size) throws ClaraMsgException {
        System.out.println("INFO: Removing " + size + " random actors...");

        var first = randomGen.nextInt(registration.size() - size);
        var end = first + size;
        var i = 0;
        var it = registration.iterator();
        while (it.hasNext()) {
            if (i == end) {
                break;
            }
            var reg = it.next();
            if (i >= first) {
                it.remove();
                driver.removeRegistration(name, reg);
            }
            i++;
        }
    }


    public void removeRandomHost() throws ClaraMsgException {
        var host = RegDataFactory.random(RegDataFactory.testHosts);
        removeHost(host);
    }


    private void removeHost(String host) throws ClaraMsgException {
        System.out.println("INFO: Removing host " + host);
        registration.removeIf(r -> r.getHost().equals(host));
        driver.removeAllRegistration("test", host);
    }


    public void removeAll() throws ClaraMsgException {
        for (var host : RegDataFactory.testHosts) {
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
        var reg = new RegistrationHelper(regType, "topic");
        for (var topic : testTopics()) {
            var result = reg.request(RegDriver::findRegistration, r -> r.matching(topic));
            var expected = reg.findLocal(matchTopic(regType, topic));

            reg.assertThat(topic, result, expected);
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
        var reg = new RegistrationHelper(regType, "domain");
        for (var domain : testDomains()) {
            var result = reg.request(RegDriver::filterRegistration, r -> r.withDomain(domain));
            var expected = reg.findLocal(r -> r.getDomain().equals(domain));

            reg.assertThat(domain, result, expected);
        }
    }


    private void checkFilterBySubject(OwnerType regType) throws ClaraMsgException {
        var reg = new RegistrationHelper(regType, "subject");
        for (var subject : testSubjects()) {
            var result = reg.request(RegDriver::filterRegistration, r -> r.withSubject(subject));
            var expected = reg.findLocal(r -> r.getSubject().equals(subject));

            reg.assertThat(subject, result, expected);
        }
    }


    private void checkFilterByType(OwnerType regType) throws ClaraMsgException {
        var reg = new RegistrationHelper(regType, "type");
        for (var type : testTypes()) {
            var result = reg.request(RegDriver::filterRegistration, r -> r.withType(type));
            var expected = reg.findLocal(r -> r.getType().equals(type));

            reg.assertThat(type, result, expected);
        }
    }


    private void checkFilterByHost(OwnerType regType) throws ClaraMsgException {
        var reg = new RegistrationHelper(regType, "host");
        for (var host : RegDataFactory.testHosts) {
            var result = reg.request(RegDriver::filterRegistration, r -> r.withHost(host));
            var expected = reg.findLocal(r -> r.getHost().equals(host));

            reg.assertThat(host, result, expected);
        }
    }


    private void checkSameTopic(OwnerType regType) throws ClaraMsgException {
        var reg = new RegistrationHelper(regType, "topic");
        for (var topic : testTopics()) {
            var result = reg.request(RegDriver::sameRegistration, r -> r.withSame(topic));
            var expected = reg.findLocal(r -> getTopic(r).equals(topic));

            reg.assertThat(topic, result, expected);
        }
    }


    private void checkAll(OwnerType regType) throws ClaraMsgException {
        var reg = new RegistrationHelper(regType, "all");

        var result = reg.request(RegDriver::allRegistration, RegQuery.Factory::all);
        var expected = reg.findLocal(e -> true);

        reg.assertThat(result, expected);
    }


    private static Topic getTopic(RegData reg) {
        return Topic.build(reg.getDomain(), reg.getSubject(), reg.getType());
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


    @FunctionalInterface
    interface QueryRegistrar {
        Set<RegData> apply(RegDriver driver, String name, RegData data) throws ClaraMsgException;
    }


    private class RegistrationHelper {

        private final OwnerType regType;
        private final String valueType;

        RegistrationHelper(OwnerType regType, String valueType) {
            this.regType = regType;
            this.valueType = valueType;
        }

        Set<RegData> request(QueryRegistrar registrarFn,
                             Function<RegQuery.Factory, RegQuery> queryFn)
                throws ClaraMsgException {
            var data = queryFn.apply(queryFactory(regType)).data();
            return registrarFn.apply(driver, name, data);
        }

        Set<RegData> findLocal(Predicate<RegData> predicate) {
            return registration.stream()
                    .filter(r -> r.getOwnerType() == regType)
                    .filter(predicate)
                    .collect(Collectors.toSet());
        }

        void assertThat(Object value, Set<RegData> result, Set<RegData> expected) {
            assertThat(result, expected,
                       () -> String.format(" with %s %s", valueType, value),
                       () -> String.format(" to %s %s", valueType, value));
        }

        void assertThat(Set<RegData> result, Set<RegData> expected) {
            assertThat(result, expected, () -> "", () -> " " + valueType);
        }

        void assertThat(Set<RegData> result,
                        Set<RegData> expected,
                        Supplier<String> successSuffix,
                        Supplier<String> errorSuffix) {
            var type = regType == OwnerType.PUBLISHER ? "publishers" : "subscribers";
            if (result.equals(expected)) {
                System.out.printf("Found %3d %s%s%n", result.size(), type, successSuffix.get());
            } else {
                System.out.printf("%s:%s%n", capitalize(type), errorSuffix.get());
                System.out.println("Result: " + result.size());
                System.out.println("Expected: " + expected.size());
                fail("Sets don't match!!!");
            }
        }

        private static RegQuery.Factory queryFactory(OwnerType regType) {
            if (regType == OwnerType.PUBLISHER) {
                return RegQuery.publishers();
            } else {
                return RegQuery.subscribers();
            }
        }

        private static String capitalize(String arg) {
            return arg.substring(0, 1).toUpperCase() + arg.substring(1);
        }
    }
}
