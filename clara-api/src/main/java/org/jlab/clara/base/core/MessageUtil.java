/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.base.core;

import org.jlab.clara.msg.core.Message;
import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.MimeType;

public final class MessageUtil {

    private MessageUtil() { }

    public static Topic buildTopic(Object... args) {
        var topic  = new StringBuilder();
        topic.append(args[0]);
        for (int i = 1; i < args.length; i++) {
            topic.append(Topic.SEPARATOR);
            topic.append(args[i]);
        }
        return Topic.wrap(topic.toString());
    }

    public static String buildData(Object... args) {
        var topic  = new StringBuilder();
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
