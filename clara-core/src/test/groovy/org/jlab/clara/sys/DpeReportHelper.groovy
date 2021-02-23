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

import org.jlab.clara.base.core.ClaraConstants
import org.jlab.clara.msg.core.Actor
import org.jlab.clara.msg.core.ActorUtils
import org.jlab.clara.msg.core.Topic
import org.jlab.clara.msg.errors.ClaraMsgException
import org.jlab.clara.msg.net.ProxyAddress
import org.json.JSONObject

class DpeReportHelper {
    static void main(String... args) {
        var dpeAddress = args.length == 0
            ? new ProxyAddress("localhost")
            : new ProxyAddress("localhost", args[0] as Integer)

        var jsonTopic = Topic.build(ClaraConstants.DPE_REPORT)
        try (var subscriber = new Actor("report_subscriber")) {  // codenarc-disable-line
            subscriber.subscribe(dpeAddress, jsonTopic, (msg) -> {
                try {
                    var data = new String(msg.data)
                    var output = new JSONObject(data).toString(2)
                    println output
                } catch (Exception e) {
                    e.printStackTrace()
                }
            })
            ActorUtils.keepAlive()
        } catch (ClaraMsgException e) {
            e.printStackTrace()
        }
    }
}
