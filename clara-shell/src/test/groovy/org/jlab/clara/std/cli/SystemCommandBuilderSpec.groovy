/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.cli

import spock.lang.Specification
import spock.lang.Subject

class SystemCommandBuilderSpec extends Specification {

    @Subject
    SystemCommandBuilder builder

    void setup() {
        builder = new SystemCommandBuilder('${CLARA_HOME}/bin/clara-orchestrator').tap {
            addOption('-t', 10)
            addOption('-i', '$CLAS12DIR/exp/input')
            addArgument('custom services.yaml')
            addArgument('data/files.txt')
        }
    }

    def "Output array does not need quotes"() {
        expect:
        builder.toArray() == [
            '${CLARA_HOME}/bin/clara-orchestrator',
            '-t', '10',
            '-i', '$CLAS12DIR/exp/input',
            'custom services.yaml', 'data/files.txt',
        ] as String[]
    }

    def "Output string needs quotes"() {
        expect:
        builder.toString() == \
            '"${CLARA_HOME}/bin/clara-orchestrator"' +
            ' -t 10' +
            ' -i "$CLAS12DIR/exp/input"' +
            ' "custom services.yaml" data/files.txt'
    }

    def "Output string can quote everything"() {
        given:
        builder.quoteAll(true)

        expect:
        builder.toString() == \
            '"${CLARA_HOME}/bin/clara-orchestrator"' +
            ' "-t" "10"' +
            ' "-i" "$CLAS12DIR/exp/input"' +
            ' "custom services.yaml" "data/files.txt"'
    }

    def "Output string may be multi-line"() {
        given:
        builder.multiLine(true)

        expect:
        builder.toString() == '''\
                "${CLARA_HOME}/bin/clara-orchestrator" \\
                        -t 10 \\
                        -i "$CLAS12DIR/exp/input" \\
                        "custom services.yaml" \\
                        data/files.txt'''
                .stripIndent()
    }
}
