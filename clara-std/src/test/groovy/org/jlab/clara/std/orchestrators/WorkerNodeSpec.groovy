/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators

import groovy.transform.TypeChecked
import org.jlab.clara.base.DpeName
import org.jlab.clara.base.ServiceName
import org.jlab.clara.engine.EngineData
import org.jlab.clara.engine.EngineDataType
import org.json.JSONObject
import spock.lang.Specification
import spock.lang.Subject

import java.nio.file.Path

class WorkerNodeSpec extends Specification {

    CoreOrchestrator orchestrator

    @Subject
    WorkerNode node

    void setup() {
        orchestrator = Mock(CoreOrchestrator)
    }

    def "Deploy single-lang application sends deploy requests for all services"() {
        given:
        node = new WorkerNode(orchestrator, SingleLangData.application())

        when:
        node.deployServices()

        then:
        SingleLangData.expectedDeployments.each {
            1 * orchestrator.deployService(it)
        }
        0 * orchestrator.deployService(_)
    }


    def "Deploy single-lang application checks if all services were deployed in the DPE"() {
        given:
        node = new WorkerNode(orchestrator, SingleLangData.application())

        when:
        node.deployServices()

        then:
        1 * orchestrator.checkServices(SingleLangData.expectedDpe, SingleLangData.expectedServices)
        0 * orchestrator.checkServices(_, _)
    }

    def "Check single-lang application queries all services in the DPE"() {
        given:
        node = new WorkerNode(orchestrator, SingleLangData.application())

        when:
        node.checkServices()

        then:
        1 * orchestrator.findServices(SingleLangData.expectedDpe, SingleLangData.expectedServices)
        0 * orchestrator.findServices(_, _)
    }

    def "Deploy multi-lang application sends deploy requests for all services"() {
        given:
        node = new WorkerNode(orchestrator, MultiLangData.application())

        when:
        node.deployServices()

        then:
        MultiLangData.expectedDeployments.each {
            1 * orchestrator.deployService(it)
        }
        0 * orchestrator.deployService(_)
    }

    def "Deploy multi-lang application checks if all services were deployed on every lang DPE"() {
        given:
        node = new WorkerNode(orchestrator, MultiLangData.application())

        when:
        node.deployServices()

        then:
        MultiLangData.expectedServices.each { dpe, services ->
            1 * orchestrator.checkServices(dpe, services)
        }
        0 * orchestrator.checkServices(_, _)
    }

    def "Check multi-lang application queries all services on every lang DPE"() {
        given:
        node = new WorkerNode(orchestrator, MultiLangData.application())

        when:
        node.checkServices()

        then:
        MultiLangData.expectedServices.each { dpe, services ->
            1 * orchestrator.findServices(dpe, services) >> true
        }
        0 * orchestrator.findServices(_, _)
    }

    def "Send user-defined configuration to IO services"() {
        given: "an application"
        node = new WorkerNode(orchestrator, MultiLangData.application())

        and: "IO services configuration"
        var config = new JSONObject("io-services": [
            reader: [block_size: 40_000, action: "ignored"],
            writer: [compression: 2, split_size: 100_000_000],
        ])

        and: "file paths"
        var input = Path.of("/mnt/data/in.dat")
        var output = Path.of("/mnt/data/out.dat")
        var paths = new OrchestratorPaths.Builder(input, output).build()

        and: "mock reader service config responses"
        var events = new EngineData().tap {
            setData(EngineDataType.INT32, 1200)
        }
        var order = new EngineData().tap {
            setData("reader_order")
        }
        orchestrator.syncSend(_, _ as String, _, _) >> events >> order

        when: "configure and open input/output files"
        node.setConfiguration(config)
        node.setFiles(paths, paths.allFiles[0])
        node.openFiles()

        then: "the reader configuration is sent to the reader"
        1 * orchestrator.syncConfig(_ as ServiceName, _ as JSONObject, _, _) >> { args ->
            with(args[0] as ServiceName) {
                it == SingleLangData.expectedServices[1]
            }
            with(args[1] as JSONObject) {
                getInt("block_size") == 40_000
            }
        }

        then: "the writer configuration is sent to the writer"
        1 * orchestrator.syncConfig(_ as ServiceName, _ as JSONObject, _, _) >> { args ->
            with(args[0] as ServiceName) {
                it == SingleLangData.expectedServices[2]
            }
            with(args[1] as JSONObject) {
                getInt("compression") == 2
                getInt("split_size") == 100_000_000
            }
        }
    }

    def "Send global service configuration to all services"() {
        given:
        var app = MultiLangData.application()
        node = new WorkerNode(orchestrator, app)

        and:
        var global = new JSONObject(foo: 1, bar: 2)
        var config = new JSONObject(global: global)

        when:
        node.setConfiguration(config)
        node.configureServices()

        then:
        app.processingServices().each {
            1 * orchestrator.syncConfig(it, { JSONObject json -> json.similar(global) }, _, _)
        }
        0 * orchestrator.syncConfig(*_)
    }

    @TypeChecked
    private static class SingleLangData {

        static WorkerApplication application() {
            return AppData.builder()
                .withServices(AppData.J1, AppData.J2, AppData.K1, AppData.K2)
                .build()
        }

        // codenarc-disable PropertyName
        static final List<DeployInfo> expectedDeployments = [
            deploy("10.1.1.10_java:master:S1", "org.test.S1", 1),
            deploy("10.1.1.10_java:master:R1", "org.test.R1", 1),
            deploy("10.1.1.10_java:master:W1", "org.test.W1", 1),
            deploy("10.1.1.10_java:master:J1", "org.test.J1", AppData.CORES),
            deploy("10.1.1.10_java:master:J2", "org.test.J2", AppData.CORES),
            deploy("10.1.1.10_java:slave:K1", "org.test.K1", AppData.CORES),
            deploy("10.1.1.10_java:slave:K2", "org.test.K2", AppData.CORES),
        ]

        static final DpeName expectedDpe = new DpeName("10.1.1.10_java")

        static final Set<ServiceName> expectedServices = [
            new ServiceName("10.1.1.10_java:master:S1"),
            new ServiceName("10.1.1.10_java:master:R1"),
            new ServiceName("10.1.1.10_java:master:W1"),
            new ServiceName("10.1.1.10_java:master:J1"),
            new ServiceName("10.1.1.10_java:master:J2"),
            new ServiceName("10.1.1.10_java:slave:K1"),
            new ServiceName("10.1.1.10_java:slave:K2"),
        ] as Set
        // codenarc-enable
    }

    @TypeChecked
    private static class MultiLangData {

        static WorkerApplication application() {
            return AppData.builder()
                .withServices(AppData.J1, AppData.J2, AppData.C1, AppData.C2, AppData.P1)
                .withDpes(AppData.DPE1, AppData.DPE2, AppData.DPE3)
                .build()
        }

        // codenarc-disable PropertyName
        static final Set<DeployInfo> expectedDeployments = [
            deploy("10.1.1.10_java:master:S1", "org.test.S1", 1),
            deploy("10.1.1.10_java:master:R1", "org.test.R1", 1),
            deploy("10.1.1.10_java:master:W1", "org.test.W1", 1),
            deploy("10.1.1.10_java:master:J1", "org.test.J1", AppData.CORES),
            deploy("10.1.1.10_java:master:J2", "org.test.J2", AppData.CORES),
            deploy("10.1.1.10_cpp:master:C1", "org.test.C1", AppData.CORES),
            deploy("10.1.1.10_cpp:master:C2", "org.test.C2", AppData.CORES),
            deploy("10.1.1.10_python:slave:P1", "org.test.P1", AppData.CORES),
        ] as Set

        static final Map<DpeName, Set<ServiceName>> expectedServices = [
            new DpeName("10.1.1.10_java"): [
                new ServiceName("10.1.1.10_java:master:S1"),
                new ServiceName("10.1.1.10_java:master:R1"),
                new ServiceName("10.1.1.10_java:master:W1"),
                new ServiceName("10.1.1.10_java:master:J1"),
                new ServiceName("10.1.1.10_java:master:J2"),
            ] as Set,
            new DpeName("10.1.1.10_cpp"): [
                new ServiceName("10.1.1.10_cpp:master:C1"),
                new ServiceName("10.1.1.10_cpp:master:C2"),
            ] as Set,
            new DpeName("10.1.1.10_python"): [
                new ServiceName("10.1.1.10_python:slave:P1"),
            ] as Set,
        ]
        // codenarc-enable
    }

    private static DeployInfo deploy(String name, String classPath, int poolSize) {
        new DeployInfo(new ServiceName(name), classPath, poolSize)
    }
}
