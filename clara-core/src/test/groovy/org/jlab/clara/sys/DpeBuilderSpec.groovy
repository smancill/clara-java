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

package org.jlab.clara.sys

import org.jlab.clara.msg.net.ProxyAddress
import org.jlab.clara.sys.Dpe.Builder
import spock.lang.Rollup
import spock.lang.Specification

import java.util.concurrent.TimeUnit


class DpeBuilderSpec extends Specification {

    private static final String DEFAULT_HOST = Dpe.DEFAULT_PROXY_HOST

    def "A DPE is a front-end by default"() {
        given:
        var builder = new Builder()

        expect:
        builder.isFrontEnd
    }

    def "A DPE is a worker if a front-end address is given"() {
        given:
        var builder = new Builder("10.2.9.100")

        expect:
        !builder.isFrontEnd
    }

    def "Front-end DPE: use default local address"() {
        given:
        var builder = new Builder()

        expect:
        builder.frontEndAddress == proxy(DEFAULT_HOST)
        builder.localAddress == proxy(DEFAULT_HOST)
    }

    @Rollup
    def "Front-end DPE: set local address"() {
        expect:
        builder.frontEndAddress == address
        builder.localAddress == address

        where:
        builder                                             || address
        new Builder().withHost("10.2.9.100")                || proxy("10.2.9.100")
        new Builder().withPort(9000)                        || proxy(DEFAULT_HOST, 9000)
        new Builder().withHost("10.2.9.100").withPort(9000) || proxy("10.2.9.100", 9000)
    }

    @Rollup
    def "Worker DPE: require remote front-end address"() {
        expect:
        builder.frontEndAddress == address
        builder.localAddress == proxy(DEFAULT_HOST)

        where:
        builder                         || address
        new Builder("10.2.9.100")       || proxy("10.2.9.100")
        new Builder("10.2.9.100", 9000) || proxy("10.2.9.100", 9000)
    }

    def "Worker DPE: use default local address"() {
        given:
        var builder = new Builder("10.2.9.1")

        expect:
        builder.frontEndAddress == proxy("10.2.9.1")
        builder.localAddress == proxy(DEFAULT_HOST)
    }

    @Rollup
    def "Worker DPE: set local address"() {
        expect:
        builder.frontEndAddress == proxy("10.2.9.1")
        builder.localAddress == localAddress

        where:
        builder                                                     || localAddress
        new Builder("10.2.9.1").withHost("10.2.9.4")                || proxy("10.2.9.4")
        new Builder("10.2.9.1").withPort(8500)                      || proxy(DEFAULT_HOST, 8500)
        new Builder("10.2.9.1").withHost("10.2.9.4").withPort(8500) || proxy("10.2.9.4", 8500)
    }

    def "DPE: use default #option"() {
        given:
        var builder = new Builder()

        expect:
        builder."${option}" == defaultValue

        where:
        option         || defaultValue
        "session"      || ""
        "description"  || ""
        "maxCores"     || Dpe.DEFAULT_MAX_CORES
        "poolSize"     || Dpe.DEFAULT_POOL_SIZE
        "reportPeriod" || Dpe.DEFAULT_REPORT_PERIOD
    }

    def "DPE: set #option"() {
        given:
        var builder = new Builder()."${setter}"(*args)

        expect:
        builder."${option}" == value

        where:
        option         | args                   || value
        "session"      | ["XYZ"]                || "XYZ"
        "description"  | ["desc"]               || "desc"
        "maxCores"     | [32]                   || 32
        "poolSize"     | [12]                   || 12
        "reportPeriod" | [20, TimeUnit.SECONDS] || 20_000L

        setter = "with${option.capitalize()}"
    }

    private static def proxy(String host, port = Dpe.DEFAULT_PROXY_PORT) {
        new ProxyAddress(host, port)
    }
}
