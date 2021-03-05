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

import spock.lang.Rollup
import spock.lang.Specification

import static org.jlab.clara.msg.core.Topic.ANY
import static org.jlab.clara.msg.core.Topic.build
import static org.jlab.clara.msg.core.Topic.wrap

@Rollup
class TopicSpec extends Specification {

    def "Build topic from individual parts"() {
        expect:
        topic.toString() == expected

        where:
        topic                                           || expected

        build("rock")                                   || "rock"
        build("rock", "*")                              || "rock"
        build("rock", null)                             || "rock"
        build("rock", "*", "*")                         || "rock"
        build("rock", "*", null)                        || "rock"
        build("rock", null, "*")                        || "rock"
        build("rock", null, null)                       || "rock"

        build("rock", "metal")                          || "rock:metal"
        build("rock", "metal", "*")                     || "rock:metal"
        build("rock", "metal", null)                    || "rock:metal"

        build("rock", "metal", "metallica")             || "rock:metal:metallica"
        build("rock", "metal", "metallica:lars:james")  || "rock:metal:metallica:lars:james"
        build("rock", "metal", "metallica:lars:*")      || "rock:metal:metallica:lars"
    }


    def "Build topic with undefined domain throws an exception"() {
        when:
        builder()

        then:
        thrown IllegalArgumentException

        where:
        _ | builder
        _ | { build("*") }
        _ | { build(null) }
        _ | { build("*", "subject") }
        _ | { build(null, "subject") }
    }

    def "Wrap raw string topic"() {
        expect:
        topic.toString() == expected

        where:
        topic                           || expected
        wrap("rock")                    || "rock"
        wrap("rock:metal")              || "rock:metal"
        wrap("rock:metal:slayer")       || "rock:metal:slayer"
        wrap("rock:metal:slayer:tom")   || "rock:metal:slayer:tom"
    }

    def "Get specific topic parts"() {
        expect:
        topic.domain() == domain
        topic.subject() == subject
        topic.type() == type

        where:
        topic                                       || domain   | subject   | type

        build("rock")                               || "rock"   | ANY       | ANY
        build("rock", "metal")                      || "rock"   | "metal"   | ANY
        build("rock", "metal", "metallica")         || "rock"   | "metal"   | "metallica"
        build("rock", "metal", "metallica:lars")    || "rock"   | "metal"   | "metallica:lars"

        wrap("rock")                                || "rock"   | ANY       | ANY
        wrap("rock:metal")                          || "rock"   | "metal"   | ANY
        wrap("rock:metal:metallica")                || "rock"   | "metal"   | "metallica"
        wrap("rock:metal:metallica:lars")           || "rock"   | "metal"   | "metallica:lars"
    }

    def "Detect if a topic is parent of another"() {
        expect:
        wrap(topic).isParent(wrap(other)) == isParent

        where:
        topic                   | other                         || isParent

        "rock"                  | "rock"                        || true
        "rock"                  | "rock:metal"                  || true
        "rock"                  | "rock:metal:slayer"           || true
        "rock"                  | "rock:metal:metallica"        || true
        "rock"                  | "rock:alternative"            || true
        "rock"                  | "movies"                      || false
        "rock"                  | "movies:classic"              || false
        "rock"                  | "movies:classic:casablanca"   || false

        "rock:metal"            | "rock"                        || false
        "rock:metal"            | "rock:metal"                  || true
        "rock:metal"            | "rock:metal:metallica"        || true
        "rock:metal"            | "rock:metal:slayer"           || true
        "rock:metal"            | "rock:alternative"            || false
        "rock:metal"            | "movies"                      || false
        "rock:metal"            | "movies:classic"              || false
        "rock:metal"            | "movies:classic:casablanca"   || false

        "rock:metal:metallica"  | "rock"                        || false
        "rock:metal:metallica"  | "rock:metal"                  || false
        "rock:metal:metallica"  | "rock:metal:metallica"        || true
        "rock:metal:metallica"  | "rock:metal:slayer"           || false
        "rock:metal:metallica"  | "rock:alternative"            || false
        "rock:metal:metallica"  | "movies"                      || false
        "rock:metal:metallica"  | "movies:classic"              || false
        "rock:metal:metallica"  | "movies:classic:casablanca"   || false
    }
}
