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

package org.jlab.clara.base

import org.jlab.clara.tests.Integration
import org.json.JSONObject
import spock.lang.Specification

import org.jlab.clara.util.report.JsonUtils

@Integration
class ContainerRegistrationDataSpec extends Specification {

    JSONObject json = JsonDataUtil.parseRegistrationExample()

    def "Parse container registration data with registered services"() {
        given:
        var data = new ContainerRegistrationData(JsonUtils.getContainer(json, 1))

        expect:
        with(data) {
            name().canonicalName() == "10.1.1.10_java:franklin"
            startTime() != null
        }

        and:
        var services = data.services().collect { it.name().canonicalName() } as Set

        services == ["10.1.1.10_java:franklin:Engine2", "10.1.1.10_java:franklin:Engine3"] as Set
    }

    def "Parse container registration with empty services"() {
        given:
        var data = new ContainerRegistrationData(JsonUtils.getContainer(json, 2))

        expect:
        data.services().empty
    }
}
