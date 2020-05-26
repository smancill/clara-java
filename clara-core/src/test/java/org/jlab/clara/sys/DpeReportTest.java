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

package org.jlab.clara.sys;

import org.jlab.clara.base.core.ClaraConstants;
import org.jlab.clara.msg.core.Actor;
import org.jlab.clara.msg.core.ActorUtils;
import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.errors.ClaraMsgException;
import org.jlab.clara.msg.net.ProxyAddress;
import org.json.JSONObject;

public final class DpeReportTest {

    public static void main(String[] args) {
        ProxyAddress dpeAddress = new ProxyAddress("localhost");
        if (args.length > 0) {
            int port = Integer.parseInt(args[0]);
            dpeAddress = new ProxyAddress("localhost", port);
        }
        Topic jsonTopic = Topic.build(ClaraConstants.DPE_REPORT);
        try (Actor subscriber = new Actor("report_subscriber")) {
            subscriber.subscribe(dpeAddress, jsonTopic, (msg) -> {
                try {
                    String data = new String(msg.getData());
                    String output = new JSONObject(data).toString(2);
                    System.out.println(output);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            ActorUtils.keepAlive();
        } catch (ClaraMsgException e) {
            e.printStackTrace();
        }
    }

    private DpeReportTest() { }
}
