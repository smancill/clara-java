/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.sys

import org.jlab.clara.msg.net.ProxyAddress
import spock.lang.Rollup
import spock.lang.Specification
import spock.lang.Subject

class DpeOptionsParserSpec extends Specification {

    private static final String DPE_HOST_OPT = "--host"
    private static final String DPE_PORT_OPT = "--port"

    private static final String FE_HOST_OPT = "--fe-host"
    private static final String FE_PORT_OPT = "--fe-port"

    private static final String SESSION_OPT = "--session"
    private static final String DESC_OPT = "--description"

    private static final String POOL_OPT = "--poolsize"
    private static final String CORES_OPT = "--max-cores"
    private static final String REPORT_OPT = "--report"

    private static final String SOCKETS_OPT = "--max-sockets"
    private static final String IO_THREADS_OPT = "--io-threads"

    private static final String DEFAULT_HOST = Dpe.DEFAULT_PROXY_HOST

    @Subject
    DpeOptionsParser parser

    def setup() {
        parser = new DpeOptionsParser()
    }

    def "A DPE is a front-end by default"() {
        when:
        parser.parse()

        then:
        parser.isFrontEnd()
    }

    def "A DPE is a worker if a front-end address is given"() {
        when:
        parser.parse(FE_HOST_OPT, "10.2.9.100")

        then:
        !parser.isFrontEnd()
    }

    def "Front-end DPE: use default local address"() {
        when:
        parser.parse()

        then:
        parser.frontEnd() == proxy(DEFAULT_HOST)
        parser.localAddress() == proxy(DEFAULT_HOST)
    }

    @Rollup
    def "Front-end DPE: set local address"() {
        when:
        parser.parse(*args)

        then:
        parser.frontEnd() == address
        parser.localAddress() == address

        where:
        args                                             || address
        [DPE_HOST_OPT, "10.2.9.4"]                       || proxy("10.2.9.4")
        [DPE_PORT_OPT, "8500"]                           || proxy(DEFAULT_HOST, 8500)
        [DPE_HOST_OPT, "10.2.9.4", DPE_PORT_OPT, "8500"] || proxy("10.2.9.4", 8500)
    }

    @Rollup
    def "Worker DPE: require remote front-end address"() {
        when:
        parser.parse(*args)

        then:
        parser.frontEnd() == address
        parser.localAddress() == proxy(DEFAULT_HOST)

        where:
        args                                             || address
        [FE_HOST_OPT, "10.2.9.100"]                      || proxy("10.2.9.100")
        [FE_HOST_OPT, "10.2.9.100", FE_PORT_OPT, "9000"] || proxy("10.2.9.100", 9000)
    }

    def "Worker DPE: use default local address"() {
        when:
        parser.parse(FE_HOST_OPT, "10.2.9.100")

        then:
        parser.frontEnd() == proxy("10.2.9.100")
        parser.localAddress() == proxy(DEFAULT_HOST)
    }

    @Rollup
    def "Worker DPE: set local address"() {
        when:
        parser.parse(*args)

        then:
        parser.frontEnd() == proxy("10.2.9.100")
        parser.localAddress() == address

        where:
        options                                          || address
        [DPE_HOST_OPT, "10.2.9.4"]                       || proxy("10.2.9.4")
        [DPE_PORT_OPT, "8500"]                           || proxy(DEFAULT_HOST, 8500)
        [DPE_HOST_OPT, "10.2.9.4", DPE_PORT_OPT, "8500"] || proxy("10.2.9.4", 8500)

        args = [FE_HOST_OPT, "10.2.9.100", *options]
    }

    def "Worker DPE: require both front-end host and port if front-end port is set"() {
        when:
        parser.parse(FE_PORT_OPT, "9000")

        then:
        var ex = thrown(DpeOptionsParser.DpeOptionsException)
        ex.message =~ "remote front-end host is required"
    }

    def "DPE: use default #option"() {
        when:
        parser.parse()

        then:
        getter(parser) == defaultValue

        // codenarc-disable SpaceAfterOpeningBrace
        where:
        option         | getter                               || defaultValue
        "session"      | ({ p -> p.session() })               || ""
        "description"  | ({ p -> p.description() })           || ""
        "maxCores"     | ({ p -> p.config().maxCores() })     || Dpe.DEFAULT_MAX_CORES
        "poolSize"     | ({ p -> p.config().poolSize() })     || Dpe.DEFAULT_POOL_SIZE
        "reportPeriod" | ({ p -> p.config().reportPeriod() }) || Dpe.DEFAULT_REPORT_PERIOD
        "maxSockets"   | ({ p -> p.maxSockets() })            || Dpe.DEFAULT_MAX_SOCKETS
        "ioThreads"    | ({ p -> p.ioThreads() })             || Dpe.DEFAULT_IO_THREADS
        // codenarc-enable
    }

    def "DPE: set #option"() {
        when:
        parser.parse(*args)

        then:
        getter(parser) == value

        // codenarc-disable SpaceAfterOpeningBrace
        where:
        option         | getter                               | optName        | optArg || value
        "session"      | ({ p -> p.session() })               | SESSION_OPT    | "XYX"  || "XYX"
        "description"  | ({ p -> p.description() })           | DESC_OPT       | "desc" || "desc"
        "maxCores"     | ({ p -> p.config().maxCores() })     | CORES_OPT      | "32"   || 32
        "poolSize"     | ({ p -> p.config().poolSize() })     | POOL_OPT       | "10"   || 10
        "reportPeriod" | ({ p -> p.config().reportPeriod() }) | REPORT_OPT     | "20"   || 20_000
        "maxSockets"   | ({ p -> p.maxSockets() })            | SOCKETS_OPT    | "4096" || 4096
        "ioThreads"    | ({ p -> p.ioThreads() })             | IO_THREADS_OPT | "2"    || 2

        args = [optName, optArg]
        // codenarc-enable
    }

    private static def proxy(String host, int port = Dpe.DEFAULT_PROXY_PORT) {
        new ProxyAddress(host, port)
    }
}
