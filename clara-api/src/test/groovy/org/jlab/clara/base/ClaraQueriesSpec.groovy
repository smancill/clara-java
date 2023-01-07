/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base

import org.jlab.clara.base.core.ClaraBase
import org.jlab.clara.base.core.ClaraComponent
import org.jlab.clara.base.core.ClaraConstants
import org.jlab.clara.base.core.MessageUtil
import org.jlab.clara.msg.core.Message
import org.jlab.clara.msg.core.Topic
import org.jlab.clara.msg.data.RegDataProto.RegData
import org.jlab.clara.msg.net.Context
import org.jlab.clara.msg.net.ProxyAddress
import org.jlab.clara.msg.net.RegAddress
import org.jlab.clara.msg.net.SocketFactory
import org.jlab.clara.msg.sys.Registrar
import org.jlab.clara.msg.sys.regdis.RegDriver
import org.jlab.clara.msg.sys.regdis.RegFactory
import org.jlab.clara.tests.Integration
import org.json.JSONObject
import spock.lang.AutoCleanup
import spock.lang.Narrative
import spock.lang.Rollup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

import static org.jlab.clara.base.ClaraQueries.BaseQuery
import static org.jlab.clara.base.ClaraQueries.ClaraQueryBuilder

abstract class ClaraQueriesSpec extends Specification {

    @AutoCleanup
    @Shared
    data = new TestData()

    @Subject
    ClaraQueryBuilder queryBuilder

    def setup() {
        queryBuilder = new ClaraQueryBuilder(base(data), ClaraComponent.dpe())
    }

    private static ClaraBase base(TestData data) {
        new ClaraBase(ClaraComponent.dpe(), ClaraComponent.dpe()) {
            @Override
            Message syncPublish(ProxyAddress address, Message msg, long timeout) {
                // Mock DPE JSON report response
                var report = data.jsonReport(msg.topic.subject())
                MessageUtil.buildRequest(msg.topic, report.toString())
            }
        }
    }

    protected static class TestData {

        private static final TYPE = RegData.Type.SUBSCRIBER

        private static final DATE = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern(ClaraConstants.DATE_FORMAT))

        private static final SERVICE_DATA = [
            E: [name: "E", author: "Trevor", desc: "Calculate error"],
            F: [name: "F", author: "Franklin", desc: "Find term"],
            G: [name: "G", author: "Michael", desc: "Grep term"],
            H: [name: "H", author: "Franklin", desc: "Calculate height"],
            M: [name: "M", author: "Michael", desc: "Get max"],
            N: [name: "N", author: "Michael", desc: "Sum N"],
        ]

        private static final DPE_DATA = [
            [
                key: "J1",
                host: "10.2.9.1",
                lang: ClaraLang.JAVA,
                containers: [
                    [name: "A", services: ["E", "F", "G"]],
                    [name: "B", services: ["E", "H"]],
                    [name: "C", services: ["H"]],
                ]
            ],
            [
                key: "C1",
                host: "10.2.9.1",
                lang: ClaraLang.CPP,
                containers: [
                    [name: "A", services: ["M", "N"]],
                    [name: "C", services: ["N"]],
                ],
            ],
            [
                key: "J2",
                host: "10.2.9.2",
                lang: ClaraLang.JAVA,
                containers: [
                    [name: "A", services: ["E", "F"]],
                ]
            ],
            [
                key: "C2",
                host: "10.2.9.2",
                lang: ClaraLang.CPP,
                containers: [
                    [name: "A", services: ["M"]],
                ],
            ],
        ]

        private final Context context
        private final Registrar server
        private final RegDriver driver

        private final Map<String, Map<String, Object>> data = new HashMap<>()

        TestData() {
            var addr = new RegAddress("localhost", 7775)

            context = Context.newContext()
            server = new Registrar(context, addr)
            driver = new RegDriver(addr, new SocketFactory(context.context))

            server.start()
            driver.connect()

            makeRegistration()
            makeJsonReport()
        }

        void close() {
            driver.close()
            context.close()
            server.shutdown()
        }

        private void makeRegistration() {
            for (dpe in DPE_DATA) {
                var dpeName = new DpeName(dpe.host, dpe.lang)
                register(dpeName, "dpe:")

                for (container in dpe.containers) {
                    var contName = new ContainerName(dpeName, container.name)
                    register(contName, "container:")

                    for (service in container['services'].collect { SERVICE_DATA[it] }) {
                        var servName = new ServiceName(contName, service.name)
                        register(servName)
                    }
                }
            }
        }

        private void makeJsonReport() {
            for (dpe in DPE_DATA) {
                var dpeKey = dpe.key
                var dpeName = new DpeName(dpe.host, dpe.lang)

                var dpeRegData = regData(dpeName, [containers: []])
                var dpeRunData = runData(dpeName, [containers: []])

                for (container in dpe.containers) {
                    var contKey = "${dpeKey}:${container['name']}" as String
                    var contName = new ContainerName(dpeName, container.name)

                    var contRegData = regData(contName, [services: []])
                    var contRunData = runData(contName, [services: []])

                    for (service in container['services'].collect { SERVICE_DATA[it] }) {
                        var servKey = "${contKey}:${service.name}" as String
                        var servName = new ServiceName(contName, service.name)

                        var servRegData = regData(servName, [
                            class_name: service.name,
                            author: service.author,
                            description: service.desc,
                        ])
                        var servRunData = runData(servName)

                        contRegData.append('services', servRegData)
                        contRunData.append('services', servRunData)

                        data[servKey] = [name: servName, regData: servRegData, runData: servRunData]
                    }

                    dpeRegData.append('containers', contRegData)
                    dpeRunData.append('containers', contRunData)

                    data[contKey] = [name: contName, regData: contRegData, runData: contRunData]
                }

                data[dpeKey] = [name: dpeName, regData: dpeRegData, runData: dpeRunData]
            }
        }

        private static JSONObject regData(ClaraName name, Map extraValues = [:]) {
            new JSONObject([name: name.canonicalName(), start_time: DATE] + extraValues)
        }

        private static JSONObject runData(ClaraName name, Map extraValues = [:]) {
            new JSONObject([name: name.canonicalName(), snapshot_time: DATE] + extraValues)
        }

        private void register(ClaraName name, String topicPrefix = "") {
            var addr = name.address().proxyAddress()
            var topic = Topic.wrap(topicPrefix + name.canonicalName())
            var data = RegFactory.newRegistration(name.canonicalName(), "", addr, TYPE, topic)
            driver.addRegistration("test", data)
        }

        ClaraName name(String key) {
            data[key]['name'] as ClaraName
        }

        Set<ClaraName> names(String... keys) {
            keys.collect { data[it]['name'] as ClaraName } as Set
        }

        def jsonReport(String dpeName) {
            var dpeData = data.find { it.value.regData['name'] == dpeName }.value
            new JSONObject(
                ("${ClaraConstants.REGISTRATION_KEY}" as String): dpeData.regData,
                ("${ClaraConstants.RUNTIME_KEY}" as String): dpeData.runData,
            )
        }
    }
}


@Rollup
@Narrative("""Check a specific query type (defined by each subclass)
against all available filters. The specific query for each result type T:
DPE (D), container (C) or service (S), shall be returned by #query(selector).""")
abstract class AbstractQueriesSpec<D, C, S> extends ClaraQueriesSpec {

    def "Query all DPEs"() {
        given:
        DpeFilter filter = ClaraFilters.allDpes()

        when:
        Set<D> result = run query(filter)

        then:
        expect result, ["J1", "J2", "C1", "C2"]
    }

    def "Query DPEs by host"() {
        given:
        DpeFilter filter = ClaraFilters.dpesByHost(host)

        when:
        Set<D> result = run query(filter)

        then:
        expect result, keys

        where:
        host       || keys
        "10.2.9.1" || ["J1", "C1"]
        "10.2.9.9" || []
    }

    def "Query DPEs by language"() {
        given:
        DpeFilter filter = ClaraFilters.dpesByLanguage(lang)

        when:
        Set<D> result = run query(filter)

        then:
        expect result, keys

        where:
        lang             || keys
        ClaraLang.JAVA   || ["J1", "J2"]
        ClaraLang.PYTHON || []
    }

    def "Query all containers"() {
        given:
        ContainerFilter filter = ClaraFilters.allContainers()

        when:
        Set<C> result = run query(filter)

        then:
        expect result, ["J1:A", "J1:B", "J1:C", "J2:A", "C1:A", "C1:C", "C2:A"]
    }

    def "Query containers by host"() {
        given:
        ContainerFilter filter = ClaraFilters.containersByHost(host)

        when:
        Set<C> result = run query(filter)

        then:
        expect result, keys

        where:
        host       || keys
        "10.2.9.1" || ["J1:A", "J1:B", "J1:C", "C1:A", "C1:C"]
        "10.2.9.9" || []
    }

    def "Query containers by language"() {
        given:
        ContainerFilter filter = ClaraFilters.containersByLanguage(lang)

        when:
        Set<C> result = run query(filter)

        then:
        expect result, keys

        where:
        lang             || keys
        ClaraLang.CPP    || ["C1:A", "C1:C", "C2:A"]
        ClaraLang.PYTHON || []
    }

    def "Query containers by DPE"() {
        given:
        ContainerFilter filter = ClaraFilters.containersByDpe(dpe)

        when:
        Set<C> result = run query(filter)

        then:
        expect result, keys

        where:
        name              || keys
        "10.2.9.1_java"   || ["J1:A", "J1:B", "J1:C"]
        "10.2.9.8_python" || []

        dpe = new DpeName(name)
    }

    def "Query containers by name"() {
        given:
        ContainerFilter filter = ClaraFilters.containersByName(name)

        when:
        Set<C> result = run query(filter)

        then:
        expect result, keys

        where:
        name || keys
        "A"  || ["J1:A", "C1:A", "J2:A", "C2:A"]
        "Z"  || []
    }

    def "Query all services"() {
        given:
        ServiceFilter filter = ClaraFilters.allServices()

        when:
        Set<S> result = run query(filter)

        then:
        expect result, ["J1:A:E", "J1:A:F", "J1:A:G", "J1:B:E", "J1:B:H", "J1:C:H",
                        "J2:A:E", "J2:A:F",
                        "C1:A:M", "C1:A:N", "C1:C:N", "C2:A:M"]
    }

    def "Query services by host"() {
        given:
        ServiceFilter filter = ClaraFilters.servicesByHost(host)

        when:
        Set<S> result = run query(filter)

        then:
        expect result, keys

        where:
        host       || keys
        "10.2.9.2" || ["J2:A:E", "J2:A:F", "C2:A:M"]
        "10.8.8.1" || []
    }

    def "Query services by language"() {
        given:
        ServiceFilter filter = ClaraFilters.servicesByLanguage(lang)

        when:
        Set<S> result = run query(filter)

        then:
        expect result, keys

        where:
        lang             || keys
        ClaraLang.CPP    || ["C1:A:M", "C1:A:N", "C1:C:N", "C2:A:M"]
        ClaraLang.PYTHON || []
    }

    def "Query services by DPE"() {
        given:
        ServiceFilter filter = ClaraFilters.servicesByDpe(dpe)

        when:
        Set<S> result = run query(filter)

        then:
        expect result, keys

        where:
        name            || keys
        "10.2.9.1_cpp"  || ["C1:A:M", "C1:A:N", "C1:C:N"]
        "10.2.9.3_java" || []

        dpe = new DpeName(name)
    }

    def "Query services by container"() {
        given:
        ServiceFilter filter = ClaraFilters.servicesByContainer(container)

        when:
        Set<S> result = run query(filter)

        then:
        expect result, keys

        where:
        name              || keys
        "10.2.9.1_java:A" || ["J1:A:E", "J1:A:F", "J1:A:G"]
        "10.2.9.3_java:R" || []

        container = new ContainerName(name)
    }

    def "Query services by name"() {
        given:
        ServiceFilter filter = ClaraFilters.servicesByName(name)

        when:
        Set<S> result = run query(filter)

        then:
        expect result, keys

        where:
        name || keys
        "E"  || ["J1:A:E", "J1:B:E", "J2:A:E"]
        "Y"  || []
    }

    def "Query services by author"() {
        given:
        ServiceFilter filter = ClaraFilters.servicesByAuthor(author)

        when:
        Set<S> result = run query(filter)

        then:
        expect result, keys

        where:
        author   || keys
        "Trevor" || ["J1:A:E", "J1:B:E", "J2:A:E"]
        "David"  || []
    }

    def "Query services by description"() {
        given:
        ServiceFilter filter = ClaraFilters.servicesByDescription(description)

        when:
        Set<S> result = run query(filter)

        then:
        expect result, keys

        where:
        description        || keys
        ".*[Cc]alculate.*" || ["J1:A:E", "J1:B:E", "J2:A:E", "J1:B:H", "J1:C:H"]
        "Not present"      || []
    }

    protected static <T> T run(BaseQuery<BaseQuery, T> query) {
        query.syncRun(5, TimeUnit.SECONDS)
    }

    protected abstract BaseQuery query(selector)

    protected abstract void expect(Set result, List keys)
}


@Integration
@Rollup
@Title("Query canonical names")
@Narrative("""Check the canonical name queries against all available filters,
plus the discovery queries that also return canonical names but do not take a filter.""")
class CanonicalNameQueriesSpec
        extends AbstractQueriesSpec<DpeName, ContainerName, ServiceName> {

    def "Discover registered DPE"() {
        when:
        Boolean result = run queryBuilder.discover(dpe)

        then:
        result == isRegistered

        where:
        name              || isRegistered
        "10.2.9.1_java"   || true
        "10.2.9.1_python" || false

        dpe = new DpeName(name)
    }

    def "Discover registered container"() {
        when:
        Boolean result = run queryBuilder.discover(container)

        then:
        result == isRegistered

        where:
        name             || isRegistered
        "10.2.9.1_cpp:C" || true
        "10.2.9.1_cpp:R" || false

        container = new ContainerName(name)
    }

    def "Discover registered service"() {
        when:
        Boolean result = run queryBuilder.discover(service)

        then:
        result == isRegistered

        where:
        name                || isRegistered
        "10.2.9.1_java:B:H" || true
        "10.2.9.1_cpp:R:H"  || false

        service = new ServiceName(name)
    }

    @Override
    protected BaseQuery query(selector) {
        queryBuilder.canonicalNames(selector)
    }

    @Override
    protected void expect(Set result, List keys) {
        assert result == data.names(*keys)
    }
}


@Rollup
@Narrative("""Check a specific data query type (defined by each subclass)
against all available filters, plus the data query wrappers
that accept a name instead of a filter.""")
abstract class DataQueriesSpec<D, C, S> extends AbstractQueriesSpec<D, C, S> {

    def "Get DPE by canonical name"() {
        given:
        var name = new DpeName(dpe)

        when:
        Optional<D> result = run query(name)

        then:
        with(result, check)

        where:
        dpe               || check
        "10.2.9.1_java"   || { Optional r -> expect(r, "J1") }
        "10.2.9.1_python" || { Optional r -> assert r.empty }
    }

    def "Get container by canonical name"() {
        given:
        var name = new ContainerName(container)

        when:
        Optional<C> result = run query(name)

        then:
        with(result, check)

        where:
        container        || check
        "10.2.9.1_cpp:C" || { Optional r -> expect(r, "C1:C") }
        "10.2.9.1_cpp:X" || { Optional r -> assert r.empty }
    }

    def "Get service by canonical name"() {
        given:
        var name = new ServiceName(service)

        when:
        Optional<S> result = run query(name)

        then:
        with(result, check)

        where:
        service             || check
        "10.2.9.1_java:B:H" || { Optional r -> expect(r, "J1:B:H") }
        "10.2.9.1_java:Z:M" || { Optional r -> assert r.empty }
    }

    @Override
    protected void expect(Set result, List keys) {
        assert result.collect { it.name() } as Set == data.names(*keys)
    }

    protected void expect(Optional result, String key) {
        assert result.get().name() == data.name(key)
    }
}


@Integration
@Title("Query registration data")
@Narrative("""Check the registration data queries against all available filters.""")
class RegistrationDataQueriesSpec extends DataQueriesSpec<
        DpeRegistrationData,
        ContainerRegistrationData,
        ServiceRegistrationData> {

    @Override
    protected BaseQuery query(selector) {
        queryBuilder.registrationData(selector)
    }
}


@Integration
@Title("Query runtime data")
@Narrative("""Check the runtime data queries against all available filters.""")
class RuntimeDataQueriesSpec extends DataQueriesSpec<
        DpeRuntimeData,
        ContainerRuntimeData,
        ServiceRuntimeData> {

    @Override
    protected BaseQuery query(selector) {
        queryBuilder.runtimeData(selector)
    }
}
