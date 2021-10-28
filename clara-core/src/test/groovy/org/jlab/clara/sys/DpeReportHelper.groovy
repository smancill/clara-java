/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
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
