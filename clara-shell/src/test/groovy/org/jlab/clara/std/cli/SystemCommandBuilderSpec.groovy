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
            addArgument('custom services.yml')
            addArgument('data/files.txt')
        }
    }

    def "Output array does not need quotes"() {
        expect:
        builder.toArray() == [
            '${CLARA_HOME}/bin/clara-orchestrator',
            '-t', '10',
            '-i', '$CLAS12DIR/exp/input',
            'custom services.yml', 'data/files.txt',
        ]
    }

    def "Output string needs quotes"() {
        expect:
        builder.toString() == \
            '"${CLARA_HOME}/bin/clara-orchestrator"' +
            ' -t 10' +
            ' -i "$CLAS12DIR/exp/input"' +
            ' "custom services.yml" data/files.txt'
    }

    def "Output string can quote everything"() {
        given:
        builder.quoteAll(true)

        expect:
        builder.toString() == \
            '"${CLARA_HOME}/bin/clara-orchestrator"' +
            ' "-t" "10"' +
            ' "-i" "$CLAS12DIR/exp/input"' +
            ' "custom services.yml" "data/files.txt"'
    }

    def "Output string may be multi-line"() {
        given:
        builder.multiLine(true)

        expect:
        builder.toString() == '''\
                "${CLARA_HOME}/bin/clara-orchestrator" \\
                        -t 10 \\
                        -i "$CLAS12DIR/exp/input" \\
                        "custom services.yml" \\
                        data/files.txt'''
                .stripIndent()
    }
}
