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
import static org.mockito.Mockito.mock;

import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.StringsCompleter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ConfigVariableTest {

    @Test
    public void buildWithDefaultParameters() throws Exception {
        ConfigVariable v = ConfigVariable.newBuilder("var", "test variable").build();

        assertThat(v.getName(), is("var"));
        assertThat(v.getDescription(), is("test variable"));
        assertThat(v.hasValue(), is(false));
    }

    @Test
    public void buildWithInitialValue() throws Exception {
        Stream.of("value", 12, 12.8, false).forEach(o -> {
            ConfigVariable v = ConfigVariable.newBuilder("var", "test variable")
                    .withInitialValue(o)
                    .build();

            assertThat(v.hasValue(), is(true));
            assertThat(v.getValue(), is(o));
        });
    }

    @Test
    public void buildWithDefaultParser() throws Exception {
        ConfigVariable v = ConfigVariable.newBuilder("var", "test variable").build();

        v.parseValue("value");

        assertThat(v.getValue(), is("value"));
    }

    @Test
    public void buildWithCustomParser() throws Exception {
        ConfigVariable v = ConfigVariable.newBuilder("var", "test variable")
                .withParser(ConfigParsers::toPositiveInteger)
                .build();

        v.parseValue("187");

        assertThat(v.getValue(), is(187));
    }

    @Test
    public void buildWithExpectedStringValues() throws Exception {
        ConfigVariable v = ConfigVariable.newBuilder("var", "a test variable")
                .withExpectedValues("hello", "hola", "bonjour")
                .build();

        assertCandidates(v, "hello", "hola", "bonjour");
    }

    @Test
    public void buildWithExpectedObjectValues() throws Exception {
        ConfigVariable v = ConfigVariable.newBuilder("var", "test variable")
                .withExpectedValues(4, 8, 15)
                .build();

        assertCandidates(v, "4", "8", "15");
    }

    @Test
    public void buildWithDefaultCompleter() throws Exception {
        ConfigVariable v = ConfigVariable.newBuilder("var", "test variable").build();

        assertCandidates(v);
    }

    @Test
    public void buildWithCustomCompleter() throws Exception {
        ConfigVariable v = ConfigVariable.newBuilder("var", "test variable")
                .withCompleter(new StringsCompleter("one", "two"))
                .build();

        assertCandidates(v, "one", "two");
    }

    private static void assertCandidates(ConfigVariable variable, String... expected) {
        List<Candidate> candidates = new ArrayList<>();

        variable.getCompleter()
                .complete(mock(LineReader.class), mock(ParsedLine.class), candidates);

        assertThat(candidates.stream().map(Candidate::value).toArray(String[]::new),
                   is(expected));
    }
}
