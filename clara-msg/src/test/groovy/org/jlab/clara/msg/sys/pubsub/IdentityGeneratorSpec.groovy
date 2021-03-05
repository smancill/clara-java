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

package org.jlab.clara.msg.sys.pubsub

import spock.lang.Specification

class IdentityGeneratorSpec extends Specification {

    List<String> testIds = 10.collect { IdentityGenerator.getCtrlId() }

    def "Control ids should have 9 digits"() {
        expect:
        testIds.every { it.length() == 9 }
    }

    def "Control ids should have the same language identifier as first digit"() {
        expect:
        testIds.every { it[0] == '1' }
    }

    def "Control ids should have the same 3-digit node prefix after the language identifier"() {
        expect:
        var prefixes = testIds.collect { it.substring(1, 4) }
        prefixes.unique(false) == [prefixes[0]]
    }

    def "Control ids should have a unique random 5-digit suffix after the node prefix"() {
        expect:
        var suffixes = testIds.collect { it.substring(4) }
        suffixes.unique(false) == suffixes
    }
}
