/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.sys;

import org.jlab.clara.msg.core.Topic;
import org.jlab.clara.msg.data.RegDataProto.RegData;
import org.jlab.clara.msg.data.RegDataProto.RegData.OwnerType;
import org.jlab.clara.msg.sys.regdis.RegFactory;

import java.util.Random;

// checkstyle.off: ConstantName
final class RegDataFactory {

    public static final String[] testTopics = {
        "writer",
        "writer:adventures",
        "writer:adventures:books",
        "writer:adventures:tales",
        "writer:scifi:books",
        "writer:scifi:tales",
        "actor",
        "actor:action",
        "actor:drama",
        "actor:comedy",
        "actor:action:movies",
        "actor:action:series",
        "actor:comedy:movies",
        "actor:comedy:series",
        "actor:drama:movies",
        "actor:drama:series",
    };

    public static final String[] testNames = {
        "A", "B", "C", "D", "E",
        "F", "G", "H", "I", "J",
        "K", "L", "M", "N", "O",
        "P", "Q", "R", "S", "T",
        "U", "V", "W", "X", "Y",
        "Z"
    };

    public static final String[] testHosts = {
        "10.2.9.50",  "10.2.9.60",
        "10.2.9.51",  "10.2.9.61",
        "10.2.9.52",  "10.2.9.62",
        "10.2.9.53",  "10.2.9.63",
        "10.2.9.54",  "10.2.9.64",
        "10.2.9.55",  "10.2.9.65",
        "10.2.9.56",  "10.2.9.66",
        "10.2.9.57",  "10.2.9.67",
        "10.2.9.58",  "10.2.9.68",
        "10.2.9.59",  "10.2.9.69",
    };

    private static final Random rnd = new Random();

    private RegDataFactory() { }

    public static String random(String[] array) {
        var idx = rnd.nextInt(array.length);
        return array[idx];
    }

    public static RegData randomRegistration() {
        var name = random(testNames);
        var host = random(testHosts);
        var topic = random(testTopics);
        var type = rnd.nextBoolean() ? OwnerType.PUBLISHER : OwnerType.SUBSCRIBER;
        return RegFactory.newRegistration(name, host, type, Topic.wrap(topic));
    }
}
