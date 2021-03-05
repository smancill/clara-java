/*
 * Copyright (c) 2016.  Jefferson Lab (JLab). All rights reserved.
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

package org.jlab.clara.base

import org.jlab.clara.base.ClaraRequests.BaseRequest
import org.jlab.clara.base.ClaraSubscriptions.BaseSubscription
import org.jlab.clara.base.core.ClaraBase
import org.jlab.clara.base.core.ClaraComponent
import org.jlab.clara.engine.EngineData
import org.jlab.clara.engine.EngineDataType
import org.jlab.clara.engine.EngineStatus
import org.jlab.clara.msg.core.Topic
import org.jlab.clara.msg.data.MetaDataProto.MetaData
import spock.lang.Rollup
import spock.lang.Specification
import spock.lang.Subject

class BaseOrchestratorSpec extends Specification {

    private static final String FE_HOST = "10.2.9.1_java"

    private static final Composition COMPOSITION =
            new Composition("10.2.9.96_java:master:E1+10.2.9.96_java:master:E2;")

    ClaraBase baseMock = Mock(ClaraBase)

    BaseRequest request
    BaseSubscription subscription

    @Subject
    BaseOrchestrator orchestrator

    def setup() {
        baseMock = Mock(ClaraBase) {
            getFrontEnd() >> ClaraComponent.dpe(FE_HOST)
            getName() >> "test_orchestrator"
        }

        orchestrator = new BaseOrchestrator() {
            @Override
            ClaraBase getClaraBase(String name, DpeName frontEnd, int poolSize) { baseMock }
        }
    }

    def "Exit DPE"() {
        given:
        var dpe = new DpeName("10.2.9.96_java")

        when:
        request = orchestrator.exit(dpe)

        then:
        assertRequest(
            host: "10.2.9.96",
            topic: "dpe:10.2.9.96_java",
            data: "stopDpe"
        )
    }

    def "Deploy container"() {
        given:
        var container = new ContainerName("10.2.9.96_java:master")

        when:
        request = orchestrator.deploy(container).withPoolsize(5)

        then:
        assertRequest(
            host: "10.2.9.96",
            topic: "dpe:10.2.9.96_java",
            data: "startContainer?master?5?undefined"
        )
    }

    def "Exit container"() {
        given:
        var container = new ContainerName("10.2.9.96_java:master")

        when:
        request = orchestrator.exit(container)

        then:
        assertRequest(
            host: "10.2.9.96",
            topic: "dpe:10.2.9.96_java",
            data: "stopContainer?master"
        )
    }

    def "Deploy service"() {
        given:
        var service = new ServiceName("10.2.9.96_java:master:E1")

        when:
        request = orchestrator.deploy(service, "org.example.service.E1").withPoolsize(10)

        then:
        assertRequest(
            host: "10.2.9.96",
            topic: "dpe:10.2.9.96_java",
            data: "startService?master?E1?org.example.service.E1?10?undefined?undefined"
        )
    }

    def "Exit service"() {
        given:
        var service = new ServiceName("10.2.9.96_java:master:E1")

        when:
        request = orchestrator.exit(service)

        then:
        assertRequest(
            host: "10.2.9.96",
            topic: "dpe:10.2.9.96_java",
            data: "stopService?master?E1"
        )
    }

    def "Configure service"() {
        given:
        var service = new ServiceName("10.2.9.96_java:master:E1")

        var data = new EngineData()
        data.setData(EngineDataType.STRING.mimeType(), "example")

        when:
        request = orchestrator.configure(service)
                              .withData(data)
                              .withDataTypes(EngineDataType.STRING)

        then:
        assertRequest(
            host: "10.2.9.96",
            topic: "10.2.9.96_java:master:E1",
            composition: "10.2.9.96_java:master:E1;",
            action: MetaData.ControlAction.CONFIGURE
        )
    }

    def "Execute service"() {
        given:
        var service = new ServiceName("10.2.9.96_java:master:E1")

        var data = new EngineData()
        data.setData(EngineDataType.STRING.mimeType(), "example")

        when:
        request = orchestrator.execute(service)
                              .withData(data)
                              .withDataTypes(EngineDataType.STRING)

        then:
        assertRequest(
            host: "10.2.9.96",
            topic: "10.2.9.96_java:master:E1",
            composition: "10.2.9.96_java:master:E1;",
            action: MetaData.ControlAction.EXECUTE
        )
    }

    def "Execute composition"() {
        given:
        var data = new EngineData()
        data.setData(EngineDataType.STRING.mimeType(), "example")

        when:
        request = orchestrator.execute(COMPOSITION)
                              .withData(data)
                              .withDataTypes(EngineDataType.STRING)

        then:
        assertRequest(
            host: "10.2.9.96",
            topic: "10.2.9.96_java:master:E1",
            composition: "10.2.9.96_java:master:E1+10.2.9.96_java:master:E2;",
            action:  MetaData.ControlAction.EXECUTE
        )
    }

    def "Start reporting done"() {
        given:
        var service = new ServiceName("10.2.9.96_java:master:E1")

        when:
        request = orchestrator.configure(service).startDoneReporting(1000)

        then:
        assertRequest(
            host: "10.2.9.96",
            topic: "10.2.9.96_java:master:E1",
            data: "serviceReportDone?1000"
        )
    }

    def "Stop reporting done"() {
        given:
        var service = new ServiceName("10.2.9.96_java:master:E1")

        when:
        request = orchestrator.configure(service).stopDoneReporting()

        then:
        assertRequest(
            host: "10.2.9.96",
            topic: "10.2.9.96_java:master:E1",
            data: "serviceReportDone?0"
        )
    }

    def "Start reporting data"() {
        given:
        var service = new ServiceName("10.2.9.96_java:master:E1")

        when:
        request = orchestrator.configure(service).startDataReporting(1000)

        then:
        assertRequest(
            host: "10.2.9.96",
            topic: "10.2.9.96_java:master:E1",
            data: "serviceReportData?1000"
        )
    }

    def "Stop reporting data"() {
        given:
        var service = new ServiceName("10.2.9.96_java:master:E1")

        when:
        request = orchestrator.configure(service).stopDataReporting()

        then:
        assertRequest(
            host: "10.2.9.96",
            topic: "10.2.9.96_java:master:E1",
            data: "serviceReportData?0"
        )
    }

    def "Listen service status"() {
        given:
        var service = new ServiceName("10.2.9.96_java:master:SimpleEngine")

        when:
        subscription = orchestrator.listen(service).status(EngineStatus.ERROR)

        then:
        assertSubscription(topic: "ERROR:10.2.9.96_java:master:SimpleEngine")
    }

    def "Listen service data"() {
        given:
        var service = new ServiceName("10.2.9.96_java:master:SimpleEngine")

        when:
        subscription = orchestrator.listen(service).data()

        then:
        assertSubscription(topic: "data:10.2.9.96_java:master:SimpleEngine")
    }

    def "Listen service done"() {
        given:
        var service = new ServiceName("10.2.9.96_java:master:SimpleEngine")

        when:
        subscription = orchestrator.listen(service).done()

        then:
        assertSubscription(topic: "done:10.2.9.96_java:master:SimpleEngine")
    }

    def "Listen DPEs alive"() {
        when:
        subscription = orchestrator.listen().aliveDpes()

        then:
        assertSubscription(topic: "dpeAlive:")
    }

    @Rollup
    def "Listen DPEs alive with session"() {
        when:
        subscription = orchestrator.listen().aliveDpes(session)

        then:
        assertSubscription(topic: topic)

        where:
        session   || topic
        ""        || "dpeAlive::"
        "*"       || "dpeAlive:"
        "foobar"  || "dpeAlive:foobar:"
        "foobar*" || "dpeAlive:foobar"
    }

    def "Listen DPEs report"() {
        when:
        subscription = orchestrator.listen().dpeReport()

        then:
        assertSubscription(topic: "dpeReport:")
    }

    @Rollup
    def "Listen DPEs report with session"() {
        when:
        subscription = orchestrator.listen().dpeReport(session)

        then:
        assertSubscription(topic: topic)

        where:
        session   || topic
        ""        || "dpeReport::"
        "*"       || "dpeReport:"
        "foobar"  || "dpeReport:foobar:"
        "foobar*" || "dpeReport:foobar"
    }

    def "Listen data ring"() {
        when:
        subscription = orchestrator.listen().dataRing()

        then:
        assertSubscription(topic: "ring:")
    }

    @Rollup
    def "Listen data ring with topic"() {
        when:
        subscription = orchestrator.listen().dataRing(new DataRingTopic(*args))

        then:
        assertSubscription(topic: topic)

        where:
        args                   || topic
        ["foo"]                || "ring:foo:"
        ["foo*"]               || "ring:foo"
        ["foo", ""]            || "ring:foo::"
        ["foo", "bar"]         || "ring:foo:bar:"
        ["foo", "bar*"]        || "ring:foo:bar"
        ["foo", "", "liz"]     || "ring:foo::liz"
        ["foo", "", "liz*"]    || "ring:foo::liz"
        ["foo", "bar", "liz"]  || "ring:foo:bar:liz"
        ["foo", "bar", "liz*"] || "ring:foo:bar:liz"
    }

    private void assertRequest(Map args) {
        assert request.frontEnd.dpeHost == args.host

        var msg = request.msg()
        assert msg.topic == Topic.wrap(args.topic as String)
        assert msg.metaData.author == "test_orchestrator"

        if ("data" in args) {
            assert new String(msg.data) == args.data
        } else {
            assert msg.metaData.composition == args.composition
            assert msg.metaData.action == args.action
        }
    }

    private void assertSubscription(Map args) {
        assert subscription.frontEnd.canonicalName == FE_HOST
        assert subscription.topic == Topic.wrap(args.topic as String)
    }
}
