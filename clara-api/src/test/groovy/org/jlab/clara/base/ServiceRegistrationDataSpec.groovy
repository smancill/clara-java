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
class ServiceRegistrationDataSpec extends Specification {

    JSONObject json = JsonDataUtil.parseRegistrationExample()

    def "Parse service registration data"() {
        given:
        var data = new ServiceRegistrationData(JsonUtils.getService(json, 1, 0))

        expect:
        with(data) {
            name().canonicalName() == "10.1.1.10_java:franklin:Engine2"
            className() == "org.jlab.clara.examples.Engine2"
            startTime() != null
            poolSize() == 2
            author() == "Trevor"
            version() == "1.0"
            description() == "Some description of what it does."
        }
    }
}
