/*
 * Copyright (c) 2017.  Jefferson Lab (JLab). All rights reserved.
 *
 * Permission to use, copy, modify, and distribute  this software and its
 * documentation for educational, research, and not-for-profit purposes,
 * without fee and without a signed licensing agreement.
 *
 * IN NO EVENT SHALL JLAB BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL
 * INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING
 * OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JLAB HAS
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JLAB SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE. THE CLARA SOFTWARE AND ACCOMPANYING DOCUMENTATION, IF ANY,
 * PROVIDED HEREUNDER IS PROVIDED "AS IS". JLAB HAS NO OBLIGATION TO PROVIDE
 * MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * This software was developed under the United States Government license.
 * For more information contact author at gurjyan@jlab.org
 * Department of Experimental Nuclear Physics, Jefferson Lab.
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
