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

package org.jlab.clara.std.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SystemCommandBuilderTest {

    private SystemCommandBuilder b;

    @BeforeEach
    public void setUp() {
        b = new SystemCommandBuilder("${CLARA_HOME}/bin/clara-orchestrator");
        b.addOption("-t", 10);
        b.addOption("-i", "$CLAS12DIR/exp/input");
        b.addArgument("custom services.yml");
        b.addArgument("data/files.txt");
    }

    @Test
    public void outputArrayDoesNotNeedQuotes() throws Exception {
        // checkstyle.off: Indentation
        assertThat(b.toArray(), is(new String[]{
                "${CLARA_HOME}/bin/clara-orchestrator",
                "-t", "10",
                "-i", "$CLAS12DIR/exp/input",
                "custom services.yml",
                "data/files.txt"
                }));
        // checkstyle.on: Indentation
    }

    @Test
    public void outputStringNeedsQuotes() throws Exception {
        assertThat(b.toString(), is(
                "\"${CLARA_HOME}/bin/clara-orchestrator\""
                + " -t 10"
                + " -i \"$CLAS12DIR/exp/input\""
                + " \"custom services.yml\""
                + " data/files.txt"));
    }

    @Test
    public void outputStringCanQuoteEverything() throws Exception {
        b.quoteAll(true);

        assertThat(b.toString(), is(
                "\"${CLARA_HOME}/bin/clara-orchestrator\""
                + " \"-t\" \"10\""
                + " \"-i\" \"$CLAS12DIR/exp/input\""
                + " \"custom services.yml\""
                + " \"data/files.txt\""));
    }

    @Test
    public void outputStringMayBeMultiline() throws Exception {
        b.multiLine(true);

        assertThat(b.toString(), is(
                "\"${CLARA_HOME}/bin/clara-orchestrator\" \\"
                + "\n        -t 10 \\"
                + "\n        -i \"$CLAS12DIR/exp/input\" \\"
                + "\n        \"custom services.yml\" \\"
                + "\n        data/files.txt"));
    }
}
