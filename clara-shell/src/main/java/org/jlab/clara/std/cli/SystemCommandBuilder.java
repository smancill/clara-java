/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class SystemCommandBuilder {

    private static final Pattern NEED_QUOTES = Pattern.compile("[^A-Za-z0-9/._-]");

    private final List<Token> cmd = new ArrayList<>();

    private boolean quoteAll = false;
    private boolean multiLine = false;

    SystemCommandBuilder(Path program) {
        this(program.toString());
    }

    SystemCommandBuilder(String program) {
        cmd.add(new Token(program, false));
    }

    public void addOption(String option) {
        cmd.add(new Token(option, true));
    }

    public void addOption(String option, Object value) {
        cmd.add(new Token(option, true));
        cmd.add(new Token(value.toString(), false));
    }

    public void addArgument(Object argument) {
        cmd.add(new Token(argument.toString(), true));
    }

    public void quoteAll(boolean quote) {
        quoteAll = quote;
    }

    public void multiLine(boolean split) {
        multiLine = split;
    }

    private Token mayQuote(Token token) {
        Matcher matcher = NEED_QUOTES.matcher(token.value);
        if (matcher.find() || quoteAll) {
            String quoted = "\"" + token.value + "\"";
            return new Token(quoted, token.split);
        }
        return token;
    }

    private String maySplit(Token token) {
        if (multiLine && token.split) {
            return "\\\n        " + token.value;
        }
        return token.value;
    }

    public String[] toArray() {
        return cmd.stream()
                .map(Token::toString)
                .toArray(String[]::new);
    }

    @Override
    public String toString() {
        return cmd.stream()
                .map(this::mayQuote)
                .map(this::maySplit)
                .collect(Collectors.joining(" "));
    }


    private record Token(String value, boolean split) {

        @Override
        public String toString() {
            return value;
        }
    }
}
