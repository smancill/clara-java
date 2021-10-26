/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.orchestrators

import org.jlab.clara.base.ClaraLang
import org.jlab.clara.base.ContainerName
import org.jlab.clara.base.ServiceName
import spock.lang.Specification

class WorkerApplicationSpec extends Specification {

    def "Get stage service"() {
        given:
        WorkerApplication app = AppData.builder().build()

        expect:
        app.stageService() == service("10.1.1.10_java:master:S1")
    }

    def "Get reader service"() {
        given:
        WorkerApplication app = AppData.builder().build()

        expect:
        app.readerService() == service("10.1.1.10_java:master:R1")
    }

    def "Get writer service"() {
        given:
        WorkerApplication app = AppData.builder().build()

        expect:
        app.writerService() == service("10.1.1.10_java:master:W1")
    }

    def "Get data-processing services for single-lang application"() {
        given:
        WorkerApplication app = AppData.builder().build()

        expect:
        app.processingServices() == services(
            "10.1.1.10_java:master:J1",
            "10.1.1.10_java:master:J2",
            "10.1.1.10_java:master:J3",
        )
    }

    def "Get data-processing services for multi-lang application"() {
        given:
        WorkerApplication app = AppData.builder()
            .withServices(AppData.J1, AppData.C1, AppData.C2, AppData.P1)
            .withDpes(AppData.DPE1, AppData.DPE2, AppData.DPE3)
            .build()

        expect:
        app.processingServices() == services(
            "10.1.1.10_java:master:J1",
            "10.1.1.10_cpp:master:C1",
            "10.1.1.10_cpp:master:C2",
            "10.1.1.10_python:slave:P1",
        )
    }

    def "Get unique container"() {
        given:
        WorkerApplication app = AppData.builder().build()

        expect:
        allContainers(app) == containers("10.1.1.10_java:master")
    }

    def "Get all containers for single-lang application"() {
        given:
        WorkerApplication app = AppData.builder()
            .withServices(AppData.J1, AppData.J2, AppData.K1)
            .build()

        expect:
        allContainers(app) == containers(
            "10.1.1.10_java:master",
            "10.1.1.10_java:slave"
        )
    }

    def "Get all containers for multi-lang application"() {
        given:
        WorkerApplication app = AppData.builder()
                .withServices(AppData.J1, AppData.K1, AppData.C1, AppData.P1)
                .withDpes(AppData.DPE1, AppData.DPE2, AppData.DPE3)
                .build()

        expect:
        allContainers(app) == containers(
            "10.1.1.10_java:master",
            "10.1.1.10_java:slave",
            "10.1.1.10_cpp:master",
            "10.1.1.10_python:slave",
        )
    }

    def "Get language for single-lang application"() {
        given:
        ApplicationInfo info = AppData.newAppInfo(AppData.J1, AppData.J2, AppData.J3)

        expect:
        info.languages == [ClaraLang.JAVA] as Set
    }

    def "Get languages for multi-lang application"() {
        given:
        ApplicationInfo info = AppData.newAppInfo(AppData.J1, AppData.C1, AppData.P1)

        expect:
        info.languages == [ClaraLang.JAVA, ClaraLang.CPP, ClaraLang.PYTHON] as Set
    }

    def "Create the composition for a data processing chain"() {
        given:
        WorkerApplication app = AppData.builder()
                .withServices(AppData.J1, AppData.J2, AppData.J3)
                .build()

        when:
        var composition = app.composition()

        then:
        var expected = "10.1.1.10_java:master:R1+" +
                       "10.1.1.10_java:master:J1+" +
                       "10.1.1.10_java:master:J2+" +
                       "10.1.1.10_java:master:J3+" +
                       "10.1.1.10_java:master:W1+" +
                       "10.1.1.10_java:master:R1;"

        composition.toString() == expected
    }

    def "Create the composition for data processing and monitoring chains"() {
        given:
        WorkerApplication app = AppData.builder()
                .withServices(AppData.J1, AppData.J2)
                .withMonitoring(AppData.K1, AppData.K2)
                .build()

        when:
        var composition = app.composition()

        then:
        var expected = "10.1.1.10_java:master:R1+" +
                       "10.1.1.10_java:master:J1+" +
                       "10.1.1.10_java:master:J2+" +
                       "10.1.1.10_java:master:W1+" +
                       "10.1.1.10_java:master:R1;" +
                       "10.1.1.10_java:master:J2+" +
                       "10.1.1.10_java:slave:K1+" +
                       "10.1.1.10_java:slave:K2;"

        composition.toString() == expected
    }

    private static ServiceName service(String name) {
        new ServiceName(name)
    }

    private static List services(String... elem) {
        elem.collect(ServiceName::new)
    }

    private static Set containers(String... elem) {
        elem.collect(ContainerName::new)
    }

    private static Set allContainers(WorkerApplication app) {
        app.allContainers().values().flatten().collect() as Set
    }
}
