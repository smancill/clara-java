/*
 * Copyright (C) 2016. Jefferson Lab (JLAB). All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for governmental use, educational, research, and not-for-profit
 * purposes, without fee and without a signed licensing agreement.
 *
 * IN NO EVENT SHALL JLAB BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL,
 * INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF
 * THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JLAB HAS BEEN ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JLAB SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE. THE CLARA SOFTWARE AND ACCOMPANYING DOCUMENTATION, IF ANY, PROVIDED
 * HEREUNDER IS PROVIDED "AS IS". JLAB HAS NO OBLIGATION TO PROVIDE MAINTENANCE,
 * SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * This software was developed under the United States Government License.
 * For more information contact author at gurjyan@jlab.org
 * Department of Experimental Nuclear Physics, Jefferson Lab.
 */

package org.jlab.clara.msg.core

import org.jlab.clara.msg.data.MetaDataProto.MetaData
import org.jlab.clara.msg.data.MimeType
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.ByteOrder

class MessageSpec extends Specification {

    @Shared Topic testTopic = Topic.wrap("test_topic")
    @Shared String testMime = "data/binary"
    @Shared byte[] testData = [0x0, 0x1, 0x2, 0xa, 0xb]

    @Unroll("Create and parse a message from a <#klass.simpleName> value")
    def "Helpers to create and parse message can detect the type of primitive/array values"() {
        when: "creating a message from an object"
        var msg = Message.createFrom(testTopic, value)

        then: "the message has the right mime-type"
        msg.mimeType == mimeType

        and: "the parsed message data is a different object than the original data"
        Message.parseData(msg) !== value

        and: "the parsed message data is equal to the original data"
        Message.parseData(msg) == value
        Message.parseData(msg, klass) == value

        where:
        value                               | mimeType                  | klass
        "test_data"                         | MimeType.STRING           | String.class
        460                                 | MimeType.SFIXED32         | Integer.class
        520L                                | MimeType.SFIXED64         | Long.class
        100.2f                              | MimeType.FLOAT            | Float.class
        2000.5d                             | MimeType.DOUBLE           | Double.class
        ["foo", "bar"] as String[]          | MimeType.ARRAY_STRING     | String[].class
        [3, 4, 5] as Integer[]              | MimeType.ARRAY_SFIXED32   | Integer[].class
        [8, 100] as Long[]                  | MimeType.ARRAY_SFIXED64   | Long[].class
        [1.0f, 2.0f] as Float[]             | MimeType.ARRAY_FLOAT      | Float[].class
        [3.2d, 40.7d, 58.5d] as Double[]    | MimeType.ARRAY_DOUBLE     | Double[].class
        ["a", "b", "c"] as Set              | MimeType.JOBJECT          | Object.class
    }

    def "Creating a message without setting the byte order uses the default order for the data"() {
        when:
        var meta = MetaData.newBuilder().tap {
            dataType = "test/binary"
        }
        var msg = new Message(testTopic, meta, [0x0, 0x1] as byte[])

        then:
        !msg.hasDataOrder()
        msg.dataOrder == ByteOrder.BIG_ENDIAN
    }

    def "Creating a message can specify the byte order for the data"() {
        when:
        var meta = MetaData.newBuilder().tap {
            dataType = testMime
            byteOrder = MetaData.Endian.Little
        }
        var msg = new Message(testTopic, meta, testData)

        then:
        msg.hasDataOrder()
        msg.dataOrder == ByteOrder.LITTLE_ENDIAN
    }

    def "Creating a simple response message uses the reply topic and the same request data"() {
        given: "A message with 'replyTo' set"
        var meta = MetaData.newBuilder().tap {
            dataType = testMime
            replyTo = "return:123"
        }
        var msg = new Message(testTopic, meta, testData)

        when: "creating a simple response for the message"
        var res = Message.createResponse(msg)

        then: "the response has the same data and uses the 'reply' to as the topic"
        res.topic == Topic.wrap("return:123")
        res.mimeType == msg.mimeType
        res.data == msg.data

        and: "the response has the 'replyTo' field not set"
        !res.hasReplyTopic()
    }

    def "Creating a custom response message uses the reply topic and the response data"() {
        given: "a message with 'replyTo' set"
        var meta = MetaData.newBuilder().tap {
            dataType = testMime
            replyTo = "return:321"
        }
        var msg = new Message(testTopic, meta, testData)

        when: "creating a data response for the message"
        var res = Message.createResponse(msg, 1000)

        then: "the response uses the 'replyTo' as topic and has its own data"
        res.topic == Topic.wrap("return:321")
        res.mimeType == MimeType.SFIXED32
        Message.parseData(res) == 1000

        and: "the response has the 'replyTo' field not set"
        !res.hasReplyTopic()
    }
}
