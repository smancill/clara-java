/*
 * Copyright (c) 2018.  Jefferson Lab (JLab). All rights reserved.
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

package org.jlab.clara.util

import spock.lang.Rollup
import spock.lang.Shared
import spock.lang.Specification

@Rollup
class EnvUtilsSpec extends Specification {

    @Shared Map env = [VAR: "value", TEST: "this test", DIR: "/mnt/files"]

    def "Expansion of declared variable"() {
        expect:
        EnvUtils.expandEnvironment(template, env) == output

        where:
        template                        || output
        '$VAR'                          || "value"
        '${VAR}'                        || "value"
        '${VAR:-default}'               || "value"
        '${VAR} and ${VAR}'             || "value and value"
        'the ${VAR} is ${DIR}'          || "the value is /mnt/files"
        '$DIR/exp1'                     || "/mnt/files/exp1"
        'test ${TEST}'                  || "test this test"
    }

    def "Expansion of missing variable uses inline default"() {
        expect:
        EnvUtils.expandEnvironment(template, env) == output

        where:
        template                                    || output
        '${FOO:-bar}'                               || "bar"
        'a ${MISSING:-default value1} variable'     || "a default value1 variable"
    }

    def "Expansion of missing variable with no inline default returns empty"() {
        expect:
        EnvUtils.expandEnvironment(template, env) == output

        where:
        template                        || output
        'a ${MISSING} variable'         || "a  variable"
        '$MISSING'                      || ""
    }

    def "Expansion of variable with unbraced separator ignores separator and default value"() {
        expect:
        EnvUtils.expandEnvironment(template, env) == output

        where:
        template                        || output
        'a $VAR:-foo'                   || "a value:-foo"
        'a $MISSING:-bar'               || "a :-bar"
    }

    def "Expansion ignores escaped variable"() {
        expect:
        EnvUtils.expandEnvironment(template, env) == output

        where:
        template                        || output
        'this is \\$VAR'                || 'this is $VAR'
        '\\${VAR}'                      || '${VAR}'
    }

    def "Expansion of template without declared variable returns same input"() {
        expect:
        EnvUtils.expandEnvironment(template, env) == output

        where:
        template                        || output
        "foo bar"                       || "foo bar"
        ""                              || ""
        "{}"                            || "{}"
        '\\$'                           || '$'
    }

    def "Expansion of malformed variable throws"() {
        when:
        EnvUtils.expandEnvironment(template, env)

        then:
        thrown IllegalArgumentException

        where:
        _ | template
        _ | '${'
        _ | '$}'
        _ | '${}'
        _ | '${ }'
        _ | '${ foo}'
        _ | '${foo }'
    }
}
