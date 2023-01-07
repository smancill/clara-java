/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.msg.core

import spock.lang.Isolated
import spock.lang.Specification

@Isolated
class ActorUtilsSpec extends Specification {

    def "Generator of unique 'replyTo' increments atomically"() {
        given: "A generator starting from zero"
        ActorUtils.setUniqueReplyToGenerator(0)

        expect: "The counter increments sequentially"
        ActorUtils.getUniqueReplyTo("subject") == "ret:subject:1000000"
        ActorUtils.getUniqueReplyTo("subject") == "ret:subject:1000001"
        ActorUtils.getUniqueReplyTo("subject") == "ret:subject:1000002"

        when: "Two threads are requesting unique ids concurrently"
        var t1 = Thread.start {
            (900_000 - 3).times { ActorUtils.getUniqueReplyTo("subject") }
        }

        var t2 = Thread.start {
            90_000.times { ActorUtils.getUniqueReplyTo("subject") }
        }

        t1.join()
        t2.join()

        then: "The ids are incremented atomically"
        ActorUtils.getUniqueReplyTo("subject") == "ret:subject:1990000"

        when: "The counter reaches next million"
        9_999.times {
            ActorUtils.getUniqueReplyTo("subject")
        }

        then: "The id resets to 0 again"
        ActorUtils.getUniqueReplyTo("subject") == "ret:subject:1000000"
        ActorUtils.getUniqueReplyTo("subject") == "ret:subject:1000001"
        ActorUtils.getUniqueReplyTo("subject") == "ret:subject:1000002"
    }

    // The implementation uses AtomicInteger as counter, so we must handle overflow
    // TODO: this can be improved with a custom atomic cyclic counter
    def "Generator of unique 'replyTo' overflows"() {
        when: "The counter is integer MAX_VALUE (0x7fff_ffff)"
        ActorUtils.setUniqueReplyToGenerator(Integer.MAX_VALUE)

        then: "The id uses the last 6-digits of MAX_VALUE"
        ActorUtils.getUniqueReplyTo("subject") == "ret:subject:1483647"  // 0x7fff_ffff

        and: "The counter continues as unsigned MIN_VALUE & 0xffff_ffff"
        ActorUtils.getUniqueReplyTo("subject") == "ret:subject:1483648"  // 0x8000_0000
        ActorUtils.getUniqueReplyTo("subject") == "ret:subject:1483649"  // 0x8000_0001

        when: "The counter reaches the next million"
        var n = 1_000_000 - 483_650
        n.times { ActorUtils.getUniqueReplyTo("subject") }

        then: "The id resets to 0 again"
        ActorUtils.getUniqueReplyTo("subject") == "ret:subject:1000000"
        ActorUtils.getUniqueReplyTo("subject") == "ret:subject:1000001"
        ActorUtils.getUniqueReplyTo("subject") == "ret:subject:1000002"

        when: "The counter reaches unsigned integer max value (0xffff_ffff)"
        ActorUtils.setUniqueReplyToGenerator(-1)

        then: "The id uses the last 6-digits of 0xffff_ffff"
        ActorUtils.getUniqueReplyTo("subject") == "ret:subject:1967295"  // 0xffff_ffff

        and: "The counter continues from 0"
        ActorUtils.getUniqueReplyTo("subject") == "ret:subject:1000000"  // 0x0000_0000
        ActorUtils.getUniqueReplyTo("subject") == "ret:subject:1000001"  // 0x0000_0001
    }

    def "Encoding actor identity creates a string of length 8"() {
        when:
        var encoded = ActorUtils.encodeIdentity("10.0.0.1", "test_actor")

        then:
        encoded.length() == 8
    }

    def "Serializing object as byte array and deserializing the bytes creates an equal object"() {
        given:
        var orig = ["led zeppelin", "pink floyd", "black sabbath"]

        when:
        var data = ActorUtils.serializeToBytes(orig)
        var clone = ActorUtils.deserialize(data) as List<String>

        then:
        clone !== orig
        clone == orig
    }

    def "Serializing object as string and deserializing the string creates an equal object"() {
        given:
        var orig = ["led zeppelin", "pink floyd", "black sabbath"] as Set

        when:
        var data = ActorUtils.serializeToByteString(orig)
        var clone = ActorUtils.deserialize(data) as Set<String>

        then:
        clone !== orig
        clone == orig
    }
}
