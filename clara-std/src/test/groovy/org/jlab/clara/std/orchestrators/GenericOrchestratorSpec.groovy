/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators

import groovy.transform.TypeChecked
import org.hamcrest.Matcher
import org.jlab.clara.base.ClaraLang
import org.jlab.clara.base.DpeName
import org.jlab.clara.std.orchestrators.GenericOrchestrator.DpeReportCB
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Consumer

import static org.hamcrest.Matchers.containsInAnyOrder
import static spock.util.matcher.HamcrestSupport.expect

class GenericOrchestratorSpec extends Specification {

    // codenarc-disable PropertyName
    @Shared FE = new TestNode("10.1.1.254")
    @Shared N1 = new TestNode("10.1.1.1")
    @Shared N2 = new TestNode("10.1.1.2")
    @Shared N3 = new TestNode("10.1.1.3")
    @Shared N4 = new TestNode("10.1.1.4")
    // codenarc-enable

    @Shared ExecutorService executor = Executors.newFixedThreadPool(3)

    ApplicationInfo app
    CoreOrchestrator orchestrator

    DpeReportCBTest cb

    void setup() {
        orchestrator = Mock(CoreOrchestrator) {
            getFrontEnd() >> new DpeName(FE.host, ClaraLang.JAVA)
        }
    }

    def "Single-lang local-mode: only process local front-end"() {
        given:
        app = singleLangData()
        cb = localModeCallback(app)

        when:
        reported.each {
            cb.receiveFrom(it.java)
        }

        then:
        cb.acceptedNodes() == workerNodes(expected)

        where:
        reported     || expected
        [FE]         || [FE]
        [N1, N2]     || []
        [N1, N2, FE] || [FE]
    }

    def "Single-lang local mode: only process front-end node once"() {
        given:
        app = singleLangData()
        cb = localModeCallback(app)

        when:
        3.times {
            cb.receiveFrom(FE.java)
        }

        then:
        cb.acceptedNodes() == workerNodes([FE])
    }

    def "Single-lang cloud mode: use available nodes"() {
        given:
        app = singleLangData()
        cb = cloudModeCallback(app, useFE, 10)

        when:
        reported.each {
            cb.receiveFrom(it.java)
        }

        then:
        expect cb.acceptedNodes(), containsNodes(expected)

        where:
        useFE | reported     || expected
        true  | [FE]         || [FE]
        true  | [FE, N1, N2] || [FE, N1, N2]
        false | [FE]         || []
        false | [FE, N1, N2] || [N1, N2]
    }

    def "Single-lang cloud mode: only process each node once"() {
        given:
        app = AppData.newAppInfo(AppData.J1)
        cb = cloudModeCallback(app, useFE, 10)

        when:
        reported.each {
            cb.receiveFrom(it.java)
        }

        then:
        expect cb.acceptedNodes(), containsNodes(expected)

        where:
        useFE | reported                    || expected
        true  | [FE, FE, FE]                || [FE]
        true  | [N1, N2, FE, N1, N2, FE]    || [FE, N1, N2]
        true  | [FE, N1, FE, N1, FE, N1]    || [FE, N1]
        false | [N1, N1, N1]                || [N1]
        false | [N1, N2, N3, N1, N2, N3]    || [N1, N2, N3]
        false | [N1, N2, N1, N2, N1, N2]    || [N1, N2]
    }

    def "Single lang cloud mode: limit number of nodes"() {
        given:
        app = AppData.newAppInfo(AppData.J1)
        cb = cloudModeCallback(app, useFE, 3)

        when:
        cb.receiveFrom(FE.java)
        cb.receiveFrom(N3.java)

        cb.waitCallbacks()

        cb.receiveFrom(N3.java)
        cb.receiveFrom(N2.java)
        cb.receiveFrom(N1.java)

        then:
        with(cb.acceptedNodes()) { nodes ->
            nodes.size() == 3
            workerNode(FE) in nodes == useFE
        }

        where:
        useFE << [true, false]
    }


    def "Multi-lang local-mode: only process local front-end"() {
        given:
        app = AppData.newAppInfo(AppData.J1, AppData.C1)
        cb = localModeCallback(app)

        when:
        reported.each {
            cb.receiveFrom(it.java)
            cb.receiveFrom(it.cpp)
        }

        then:
        cb.acceptedNodes() == workerNodes(expected)

        where:
        reported     || expected
        [FE]         || [FE]
        [N1, N2]     || []
        [N1, N2, FE] || [FE]
    }

    def "Multi-lang local mode: only process front-end node once"() {
        given:
        app = AppData.newAppInfo(AppData.J1, AppData.C1)
        cb = localModeCallback(app)

        when:
        3.times {
            cb.receiveFrom(FE.java)
            cb.receiveFrom(FE.cpp)
        }

        then:
        cb.acceptedNodes() == workerNodes([FE])
    }

    def "Multi-lang local-mode: support all combination of languages"() {
        given:
        app = AppData.newAppInfo(*services)
        cb = localModeCallback(app)

        when:
        langs.each {
            cb.receiveFrom(FE."$it")
        }

        then:
        cb.acceptedNodes() == workerNodes([FE])

        where:
        _ | services
        _ | [AppData.J1, AppData.C1]
        _ | [AppData.J1, AppData.P1]
        _ | [AppData.J1, AppData.C2, AppData.P1]

        langs = getLanguages(services)
    }

    def "Multi-lang local mode: ignore incomplete front-end"() {
        given:
        app = AppData.newAppInfo(AppData.J1, AppData.C1)
        cb = localModeCallback(app)

        when:
        cb.receiveFrom(FE.java)

        then:
        cb.acceptedNodes().empty
    }

    def "Multi-lang cloud mode: use available nodes"() {
        given:
        app = AppData.newAppInfo(AppData.J1, AppData.C1)
        cb = cloudModeCallback(app, useFE, 10)

        when:
        nodes.each {
            cb.receiveFrom(it.java)
            cb.receiveFrom(it.cpp)
        }

        then:
        expect cb.acceptedNodes(), containsNodes(expected)

        where:
        useFE | nodes        || expected
        true  | [FE]         || [FE]
        true  | [FE, N1, N2] || [FE, N1, N2]
        false | [FE]         || []
        false | [FE, N1, N2] || [N1, N2]
    }

    def "Multi-lang cloud mode: only process each node once"() {
        given:
        app = AppData.newAppInfo(AppData.J1, AppData.C1)
        cb = cloudModeCallback(app, useFE, 10)

        when:
        reported.each {
            cb.receiveFrom(it.java)
            cb.receiveFrom(it.cpp)
        }

        then:
        expect cb.acceptedNodes(), containsNodes(expected)

        where:
        useFE | reported                 || expected
        true  | [FE, FE, FE]             || [FE]
        true  | [N1, N2, FE, N1, N2, FE] || [FE, N1, N2]
        true  | [FE, N1, FE, N1, FE, N1] || [FE, N1]
        false | [N1, N1, N1]             || [N1]
        false | [N1, N2, N3, N1, N2, N3] || [N1, N2, N3]
        false | [N1, N2, N1, N2, N1, N2] || [N1, N2]
    }

    def "Multi lang cloud mode: limit number of nodes"() {
        given:
        app = AppData.newAppInfo(AppData.J1, AppData.C1)
        cb = cloudModeCallback(app, useFE, 3)

        when:
        cb.receiveFrom(FE.java)
        cb.receiveFrom(N3.java)

        cb.receiveFrom(FE.cpp)
        cb.receiveFrom(N3.cpp)

        cb.waitCallbacks()

        cb.receiveFrom(N3.java)
        cb.receiveFrom(N2.java)
        cb.receiveFrom(N1.java)

        cb.receiveFrom(N3.cpp)
        cb.receiveFrom(N2.cpp)
        cb.receiveFrom(N1.cpp)

        then:
        with(cb.acceptedNodes()) { nodes ->
            nodes.size() == 3
            workerNode(FE) in nodes == useFE
        }

        where:
        useFE << [true, false]
    }

    def "Multi-lang cloud-mode: support all combination of languages"() {
        given:
        app = AppData.newAppInfo(*services)
        cb = cloudModeCallback(app, false, 10)

        when:
        langs.each {
            cb.receiveFrom(N1."$it")
            cb.receiveFrom(N2."$it")
        }

        then:
        expect cb.acceptedNodes(), containsNodes([N1, N2])

        where:
        _ | services
        _ | [AppData.J1, AppData.C1]
        _ | [AppData.J1, AppData.P1]
        _ | [AppData.J1, AppData.C2, AppData.P1]

        langs = getLanguages(services)
    }

    def "Multi-lang cloud mode: ignore incomplete nodes"() {
        given:
        app = multiLangData()
        cb = cloudModeCallback(app, false, 10)

        when:
        cb.receiveFrom(N1.java)
        cb.receiveFrom(N3.cpp)
        cb.receiveFrom(N2.java)

        cb.receiveFrom(N3.java)
        cb.receiveFrom(N4.cpp)
        cb.receiveFrom(N1.cpp)

        then:
        expect cb.acceptedNodes(), containsNodes([N1, N3])
    }

    @TypeChecked
    static ApplicationInfo singleLangData() {
        AppData.newAppInfo(AppData.J1)
    }

    @TypeChecked
    static ApplicationInfo multiLangData() {
        AppData.newAppInfo(AppData.J1, AppData.C1)
    }

    @TypeChecked
    private DpeReportCBTest localModeCallback(ApplicationInfo app) {
        return new DpeReportCBTest(app, OrchestratorOptions.builder().build())
    }

    @TypeChecked
    private DpeReportCBTest cloudModeCallback(ApplicationInfo app,
                                              boolean includeFrontEnd,
                                              int maxNodes) {
        var builder = OrchestratorOptions.builder().tap {
            cloudMode()
            if (includeFrontEnd) {
                useFrontEnd()
            }
            withMaxNodes(maxNodes)
        }
        return new DpeReportCBTest(app, builder.build())
    }

    @TypeChecked
    private WorkerNode workerNode(TestNode node) {
        DpeInfo[] dpes = app.languages.collect { lang -> new DpeName(node.host, lang) }
                                      .collect { name -> new DpeInfo(name, AppData.CORES, "") }

        WorkerApplication app = AppData.builder().withDpes(dpes).build()
        return new WorkerNode(orchestrator, app)
    }

    @TypeChecked
    private List<WorkerNode> workerNodes(List<TestNode> nodes) {
        return nodes.collect { workerNode(it) }
    }

    @TypeChecked
    private Matcher<Iterable<? extends WorkerNode>> containsNodes(List<TestNode> nodes) {
        WorkerNode[] workers = workerNodes(nodes)
        containsInAnyOrder(workers)
    }

    private List<String> getLanguages(Iterable<ServiceInfo> services) {
        services.collect { it.lang().name().toLowerCase() }
    }


    @TypeChecked
    static class TestNode {
        final String host

        TestNode(String host) {
            this.host = host
        }

        String getJava() { "${host}_java" }
        String getCpp() { "${host}_cpp" }
        String getPython() { "${host}_python" }
    }


    @TypeChecked
    private class DpeReportCBTest {

        private final List<Callable<Object>> tasks
        private final List<WorkerNode> nodes
        private final Consumer<WorkerNode> nodeConsumer
        private final DpeReportCB callback

        DpeReportCBTest(ApplicationInfo application, OrchestratorOptions options) {
            tasks = Collections.synchronizedList(new ArrayList<Callable<Object>>())
            nodes = Collections.synchronizedList(new ArrayList<WorkerNode>())
            nodeConsumer = nodes::add
            callback = new DpeReportCB(orchestrator, options, application, nodeConsumer)
        }

        void receiveFrom(String dpeName) {
            // simulate the subscription callback being executed in the actor thread-pool
            tasks << Executors.callable(() -> callback.callback(AppData.dpe(dpeName)))
        }

        void waitCallbacks() throws Exception {
            executor.invokeAll(tasks)
            tasks.clear()
        }

        /**
         * The list of alive nodes processed by the DPE report callback, in order.
         */
        List<WorkerNode> acceptedNodes() throws Exception {
            waitCallbacks()
            nodes
        }
    }
}
