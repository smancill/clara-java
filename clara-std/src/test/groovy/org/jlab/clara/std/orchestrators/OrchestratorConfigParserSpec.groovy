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
import org.jlab.clara.std.orchestrators.CallbackInfo.RingCallbackInfo
import org.jlab.clara.std.orchestrators.CallbackInfo.RingTopic
import org.jlab.clara.tests.Integration
import org.json.JSONObject
import spock.lang.Specification
import spock.lang.Subject

@Integration
class OrchestratorConfigParserSpec extends Specification {

    private static final String CONT = OrchestratorConfigParser.getDefaultContainer()

    // codenarc-disable LineLength
    private static final Map SERVICE_DATA = [
        CustomReader: [c: "org.jlab.clas12.convertors.CustomReader", l: ClaraLang.JAVA],
        CustomWriter: [c: "org.jlab.clas12.convertors.CustomWriter", l: ClaraLang.CPP],
        ECReconstruction: [c: "org.jlab.clas12.ec.services.ECReconstruction", l: ClaraLang.JAVA],
        FTOFReconstruction: [c: "org.jlab.clas12.ftof.services.FTOFReconstruction", l: ClaraLang.JAVA],
        SeedFinder: [c: "org.clas12.services.tracking.SeedFinder", l: ClaraLang.JAVA],
        HeaderFilter: [c: "org.jlab.clas12.convertors.HeaderFilter", l: ClaraLang.CPP],
        ECMonitor: [c: "org.jlab.clas12.services.ECMonitoring", l: ClaraLang.JAVA],
        DCMonitor: [c: "org.jlab.clas12.services.DCMonitoring", l: ClaraLang.JAVA],
    ]
    // codenarc-enable LineLength

    private static final Map CALLBACK_DATA = [
        EC_histo: "org.jlab.clas12.callbacks.ECHistogramReport",
        EC_data: "org.jlab.clas12.callbacks.ECDataReport",
        DPE_reg: "org.jlab.clas12.callbacks.DpeRegReport",
        DPE_run: "org.jlab.clas12.callbacks.DpeRunReport",
    ]

    @Subject
    OrchestratorConfigParser parser

    def "Parsing valid services YAML file should not fail"() {
        when:
        parser = makeParser("/services-ok.yml")

        then:
        notThrown OrchestratorConfigException
    }

    def "Parsing malformed services YAML file should fail"() {
        given:
        parser = makeParser("/services-bad.yml")

        when:
        parser.parseDataProcessingServices()

        then:
        var ex = thrown(OrchestratorConfigException)
        ex.message == "missing name or class of service"
    }

    def "Parse IO services"() {
        given:
        parser = makeParser("/services-custom.yml")

        when:
        var ioServices = parser.parseInputOutputServices()

        then:
        ioServices["reader"] == service("CustomReader")
        ioServices["writer"] == service("CustomWriter")
    }

    def "Parse data-processing services"() {
        given:
        parser = makeParser("/services-custom.yml")

        when:
        var dataProcessingChain = parser.parseDataProcessingServices()

        then:
        dataProcessingChain == services("ECReconstruction", "SeedFinder",
                                        "HeaderFilter", "FTOFReconstruction")
    }

    def "Parse monitoring services"() {
        given:
        parser = makeParser("/services-custom.yml")

        when:
        var monitoringChain = parser.parseMonitoringServices()

        then:
        monitoringChain == services("ECMonitor", "DCMonitor")
    }

    def "Parse data ring callbacks"() {
        given:
        parser = makeParser("/service-callbacks.yml")

        when:
        var callbacks = parser.parseDataRingCallbacks()

        // codenarc-disable LineLength
        then:
        callbacks == [
            callback(callback: "EC_histo"),
            callback(callback: "EC_histo", state: "histogram"),
            callback(callback: "EC_histo", state: "histogram", session: "clas12_group1"),
            callback(callback: "EC_data", state: "data_filter", session: "clas12_group1", engine: "ECMonitor"),
            callback(callback: "DPE_reg"),
            callback(callback: "DPE_run", session: "clas12_group1")
        ]
        // codenarc-enable LineLength
    }

    def "Parse languages when only services of the same language are declared"() {
        given:
        var parser = makeParser("/services-ok.yml")

        expect:
        parser.parseLanguages() == [ClaraLang.JAVA] as Set
    }

    def "Parse languages when services of more than one language are declared"() {
        given:
        var parser = makeParser("/services-custom.yml")

        expect:
        parser.parseLanguages() == [ClaraLang.JAVA, ClaraLang.CPP] as Set
    }

    def "Parse mime-types when no mime-types are declared"() {
        given:
        parser = makeParser("/services-ok.yml")

        expect:
        parser.parseDataTypes().empty
    }

    def "Parse mime-types"() {
        given:
        parser = makeParser("/services-custom.yml")

        expect:
        parser.parseDataTypes() == ["binary/data-evio", "binary/data-hipo"] as Set
    }

    def "Parse services configuration when no configuration is declared"() {
        given:
        parser = makeParser("/services-ok.yml")

        when:
        JSONObject config = parser.parseConfiguration()

        then:
        config.length() == 0
    }

    def "Parse services configuration"() {
        given:
        parser = makeParser("/services-custom.yml")

        when:
        JSONObject config = parser.parseConfiguration()

        then:
        config.toMap() == [
            "global": [
                magnet: [torus: 10.75, solenoid: 0.5],
                ccdb: [run: 10, variation: "custom"],
                kalman: true,
            ],
            "io-services": [
                reader: [block_size: 10000],
                writer: [compression: 2],
            ],
            "services": [
                ECReconstruction: [log: true, layers: ["inner", "outer"]],
                HeaderFilter: [max_hits: 29],
            ],
        ]
    }

    def "Parse list of input files"() {
        given:
        var fileList = getClass().getResource("/files.list").path

        when:
        var inputFiles = OrchestratorConfigParser.readInputFiles(fileList)

        then:
        inputFiles == ["file1.ev", "file2.ev", "file3.ev", "file4.ev", "file5.ev"]
    }

    private static List<ServiceInfo> services(String... names) {
        names.collect { name -> service(name) }
    }

    private static ServiceInfo service(String name) {
        new ServiceInfo(SERVICE_DATA[name].c as String,
                        CONT, name, SERVICE_DATA[name].l as ClaraLang)
    }

    private static RingCallbackInfo callback(Map<String, String> args = [:]) {
        new RingCallbackInfo(CALLBACK_DATA[args.callback],
                             new RingTopic(args.state, args.session, args.engine))
    }

    private OrchestratorConfigParser makeParser(String resource) {
        new OrchestratorConfigParser(getClass().getResource(resource).path)
    }
}
