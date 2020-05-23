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

package org.jlab.clara.base.core;

import org.jlab.clara.msg.core.Message;
import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.MimeType;

public final class MessageUtil {

    private MessageUtil() { }

    public static Topic buildTopic(Object... args) {
        StringBuilder topic  = new StringBuilder();
        topic.append(args[0]);
        for (int i = 1; i < args.length; i++) {
            topic.append(Topic.SEPARATOR);
            topic.append(args[i]);
        }
        return Topic.wrap(topic.toString());
    }

    public static String buildData(Object... args) {
        StringBuilder topic  = new StringBuilder();
        topic.append(args[0]);
        for (int i = 1; i < args.length; i++) {
            topic.append(ClaraConstants.DATA_SEP);
            topic.append(args[i]);
        }
        return topic.toString();
    }

    public static Message buildRequest(Topic topic, String data) {
        return new Message(topic, MimeType.STRING, data.getBytes());
    }
}
