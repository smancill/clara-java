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
import org.json.JSONArray
import org.json.JSONObject
import spock.lang.Specification

@Integration
class DpeRuntimeDataSpec extends Specification {

    JSONObject json = JsonDataUtil.parseRuntimeExample()

    def "Parse DPE runtime data with registered containers"() {
        given:
        var data = new DpeRuntimeData(json)

        expect:
        with(data) {
            name().canonicalName() == "10.1.1.10_java"
            snapshotTime() != null
            cpuUsage() > 0.0
            memoryUsage() > 0L
            systemLoad() > 0.0
        }

        and:
        var containers = data.containers().collect { it.name().canonicalName() } as Set

        containers == [
            "10.1.1.10_java:trevor",
            "10.1.1.10_java:franklin",
            "10.1.1.10_java:michael",
        ] as Set
    }

    def "Parse DPE runtime with empty containers"() {
        given:
        var json = json.put("containers", new JSONArray())
        var data = new DpeRuntimeData(json)

        expect:
        data.containers().empty
    }
}
