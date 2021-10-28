/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators

import org.hamcrest.Matcher
import org.jlab.clara.base.DpeName
import org.jlab.clara.std.orchestrators.GenericOrchestrator.DpeReportCB
import spock.lang.Specification

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Consumer

import static org.hamcrest.Matchers.containsInAnyOrder
import static spock.util.matcher.HamcrestSupport.expect

class GenericOrchestratorSpec extends Specification {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3)
    private static final DpeInfo FRONT_END = AppData.dpe("10.1.1.254_java")

    ApplicationInfo app
    CoreOrchestrator orchestrator

    DpeReportCBTest cb

    void setup() {
        orchestrator = Mock(CoreOrchestrator) {
            getFrontEnd() >> FRONT_END.name
        }
    }

    static final List<Node> NODES = ([254] + (1..5)).collect { new Node("10.1.1." + it) }
    static final int FE = 0

    def "Single-lang local-mode: only process local front-end"() {
        given:
        app = singleLangData()
        cb = localModeCallback(app)

        when:
        toNodes(reported).each {
            cb.receiveFrom(it.java)
        }

        then:
        cb.acceptedNodes() == makeNodes(expected)

        where:
        reported   || expected
        [FE]       || [FE]
        [1, 2]     || []
        [1, 2, FE] || [FE]
    }

    def "Single-lang local mode: only process front-end node once"() {
        given:
        app = singleLangData()
        cb = localModeCallback(app)

        when:
        3.times {
            cb.receiveFrom(NODES[FE].java)
        }

        then:
        cb.acceptedNodes() == makeNodes([FE])
    }

    def "Single-lang cloud mode: use available nodes"() {
        given:
        app = singleLangData()
        cb = cloudModeCallback(app, useFE, 10)

        when:
        toNodes(nodes).each {
            cb.receiveFrom(it.java)
        }

        then:
        expect cb.acceptedNodes(), containsNodes(expected)

        where:
        useFE | nodes      || expected
        true  | [FE]       || [FE]
        true  | [FE, 1, 2] || [FE, 1, 2]
        false | [FE]       || []
        false | [FE, 1, 2] || [1, 2]
    }

    def "Single-lang cloud mode: only process each node once"() {
        given:
        app = AppData.newAppInfo(AppData.J1)
        cb = cloudModeCallback(app, useFE, 10)

        when:
        toNodes(reported).each {
            cb.receiveFrom(it.java)
        }

        then:
        expect cb.acceptedNodes(), containsNodes(expected)

        where:
        useFE | reported              || expected
        true  | [FE, FE, FE]          || [FE]
        true  | [1, 2, FE, 1, 2, FE]  || [FE, 1, 2]
        true  | [FE, 1, FE, 1, FE, 1] || [FE, 1]
        false | [1, 1, 1]             || [1]
        false | [1, 2, 3, 1, 2, 3]    || [1, 2, 3]
        false | [1, 2, 1, 2, 1, 2]    || [1, 2]
    }

    def "Single lang cloud mode: limit number of nodes"() {
        given:
        app = AppData.newAppInfo(AppData.J1)
        cb = cloudModeCallback(app, useFE, 3)

        when:
        cb.receiveFrom(NODES[FE].java)
        cb.receiveFrom(NODES[3].java)

        cb.waitCallbacks()

        cb.receiveFrom(NODES[3].java)
        cb.receiveFrom(NODES[2].java)
        cb.receiveFrom(NODES[1].java)

        then:
        with(cb.acceptedNodes()) { nodes ->
            nodes.size() == 3
            useFE ? makeNode(FE) in nodes : makeNode(FE) !in nodes
        }

        where:
        useFE << [true, false]
    }


    def "Multi-lang local-mode: only process local front-end"() {
        given:
        app = AppData.newAppInfo(AppData.J1, AppData.C1)
        cb = localModeCallback(app)

        when:
        toNodes(reported).each {
            cb.receiveFrom(it.java)
            cb.receiveFrom(it.cpp)
        }

        then:
        cb.acceptedNodes() == makeNodes(expected)

        where:
        reported   || expected
        [FE]       || [FE]
        [1, 2]     || []
        [1, 2, FE] || [FE]
    }

    def "Multi-lang local mode: only process front-end node once"() {
        given:
        app = AppData.newAppInfo(AppData.J1, AppData.C1)
        cb = localModeCallback(app)

        when:
        3.times {
            cb.receiveFrom(NODES[FE].java)
            cb.receiveFrom(NODES[FE].cpp)
        }

        then:
        cb.acceptedNodes() == makeNodes([FE])
    }

    def "Multi-lang local-mode: support all combination of languages"() {
        given:
        app = AppData.newAppInfo(*services)
        cb = localModeCallback(app)

        when:
        langs.each {
            cb.receiveFrom(NODES[FE]."${it}")
        }

        then:
        cb.acceptedNodes() == makeNodes([FE])

        where:
        langs                     | services
        ["java", "cpp"]           | [AppData.J1, AppData.C1]
        ["java", "python"]        | [AppData.J1, AppData.P1]
        ["java", "cpp", "python"] | [AppData.J1, AppData.C2, AppData.P1]
    }

    def "Multi-lang local mode: ignore incomplete front-end"() {
        given:
        app = AppData.newAppInfo(AppData.J1, AppData.C1)
        cb = localModeCallback(app)

        when:
        cb.receiveFrom(NODES[FE].java)

        then:
        cb.acceptedNodes().empty
    }

    def "Multi-lang cloud mode: use available nodes"() {
        given:
        app = AppData.newAppInfo(AppData.J1, AppData.C1)
        cb = cloudModeCallback(app, useFE, 10)

        when:
        toNodes(nodes).each {
            cb.receiveFrom(it.java)
            cb.receiveFrom(it.cpp)
        }

        then:
        expect cb.acceptedNodes(), containsNodes(expected)

        where:
        useFE | nodes      || expected
        true  | [FE]       || [FE]
        true  | [FE, 1, 2] || [FE, 1, 2]
        false | [FE]       || []
        false | [FE, 1, 2] || [1, 2]
    }

    def "Multi-lang cloud mode: only process each node once"() {
        given:
        app = AppData.newAppInfo(AppData.J1, AppData.C1)
        cb = cloudModeCallback(app, useFE, 10)

        when:
        toNodes(reported).each {
            cb.receiveFrom(it.java)
            cb.receiveFrom(it.cpp)
        }

        then:
        expect cb.acceptedNodes(), containsNodes(expected)

        where:
        useFE | reported              || expected
        true  | [FE, FE, FE]          || [FE]
        true  | [1, 2, FE, 1, 2, FE]  || [FE, 1, 2]
        true  | [FE, 1, FE, 1, FE, 1] || [FE, 1]
        false | [1, 1, 1]             || [1]
        false | [1, 2, 3, 1, 2, 3]    || [1, 2, 3]
        false | [1, 2, 1, 2, 1, 2]    || [1, 2]
    }

    def "Multi lang cloud mode: limit number of nodes"() {
        given:
        app = AppData.newAppInfo(AppData.J1, AppData.C1)
        cb = cloudModeCallback(app, useFE, 3)

        when:
        cb.receiveFrom(NODES[FE].java)
        cb.receiveFrom(NODES[3].java)

        cb.receiveFrom(NODES[FE].cpp)
        cb.receiveFrom(NODES[3].cpp)

        cb.waitCallbacks()

        cb.receiveFrom(NODES[3].java)
        cb.receiveFrom(NODES[2].java)
        cb.receiveFrom(NODES[1].java)

        cb.receiveFrom(NODES[3].cpp)
        cb.receiveFrom(NODES[2].cpp)
        cb.receiveFrom(NODES[1].cpp)

        then:
        with(cb.acceptedNodes()) { nodes ->
            nodes.size() == 3
            useFE ? makeNode(FE) in nodes : makeNode(FE) !in nodes
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
            cb.receiveFrom(NODES[1]."${it}")
            cb.receiveFrom(NODES[2]."${it}")
        }

        then:
        expect cb.acceptedNodes(), containsNodes([1, 2])

        where:
        langs                     | services
        ["java", "cpp"]           | [AppData.J1, AppData.C1]
        ["java", "python"]        | [AppData.J1, AppData.P1]
        ["java", "cpp", "python"] | [AppData.J1, AppData.C2, AppData.P1]
    }

    def "Multi-lang cloud mode: ignore incomplete nodes"() {
        given:
        app = multiLangData()
        cb = cloudModeCallback(app, false, 10)

        when:
        cb.receiveFrom(NODES[1].java)
        cb.receiveFrom(NODES[3].cpp)
        cb.receiveFrom(NODES[2].java)

        cb.receiveFrom(NODES[3].java)
        cb.receiveFrom(NODES[4].cpp)
        cb.receiveFrom(NODES[1].cpp)

        then:
        expect cb.acceptedNodes(), containsNodes([1, 3])
    }


    static ApplicationInfo singleLangData() {
        AppData.newAppInfo(AppData.J1)
    }

    static ApplicationInfo multiLangData() {
        AppData.newAppInfo(AppData.J1, AppData.C1)
    }


    private DpeReportCBTest localModeCallback(ApplicationInfo app) {
        return new DpeReportCBTest(app, OrchestratorOptions.builder().build())
    }

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


    private WorkerNode makeNode(int id) {
        var dpes = { String host ->
            app.languages
                .collect { lang -> new DpeName(host, lang) }
                .collect { name -> new DpeInfo(name, AppData.CORES, "") }
                .toArray(DpeInfo[]::new)
        }

        WorkerApplication app = AppData.builder().withDpes(dpes(NODES[id].host)).build()
        return new WorkerNode(orchestrator, app)
    }

    private List<WorkerNode> makeNodes(ids) {
        return ids.collect { makeNode(it) }
    }

    private static Node[] toNodes(List<Integer> idx) {
        idx.collect { NODES[it] }
    }

    private Matcher<List<WorkerNode>> containsNodes(List<Integer> ids) {
        containsInAnyOrder(*makeNodes(ids))
    }


    static class Node {
        final String host

        Node(String host) {
            this.host = host
        }

        String getJava() { "${host}_java" }
        String getCpp() { "${host}_cpp" }
        String getPython() { "${host}_python" }
    }


    private class DpeReportCBTest {

        private final List<Callable<Object>> tasks
        private final List<WorkerNode> nodes
        private final Consumer<WorkerNode> nodeConsumer
        private final DpeReportCB callback

        DpeReportCBTest(ApplicationInfo application, OrchestratorOptions options) {
            tasks = Collections.synchronizedList(new ArrayList<>())
            nodes = Collections.synchronizedList(new ArrayList<>())
            nodeConsumer = nodes::add
            callback = new DpeReportCB(orchestrator, options, application, nodeConsumer)
        }

        void receiveFrom(String dpeName) {
            // simulate the subscription callback being executed in the actor thread-pool
            tasks << Executors.callable(() -> callback.callback(AppData.dpe(dpeName)))
        }

        void waitCallbacks() throws Exception {
            EXECUTOR.invokeAll(tasks)
            tasks.clear()
        }

        List<WorkerNode> acceptedNodes() throws Exception {
            waitCallbacks()
            return nodes
        }
    }
}
