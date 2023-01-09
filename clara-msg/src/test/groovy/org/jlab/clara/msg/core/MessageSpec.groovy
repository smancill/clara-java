/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
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
    def "Helpers to create and parse message can detect the type of primitive values"() {
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
        460                                 | MimeType.INT32            | Integer.class
        520L                                | MimeType.INT64            | Long.class
        100.2f                              | MimeType.FLOAT            | Float.class
        2000.5d                             | MimeType.DOUBLE           | Double.class
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
        res.mimeType == MimeType.INT32
        Message.parseData(res) == 1000

        and: "the response has the 'replyTo' field not set"
        !res.hasReplyTopic()
    }
}
