/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.sys.ccc;

import org.jlab.clara.base.core.ClaraComponent;
import org.jlab.clara.base.error.ClaraException;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Clara compiler. Compiles the application logical description, i.e.
 * simple/conditional routing schema in a sets of instructions for a specified
 * service. Below is an example of the application code, written in the
 * specific Clara language:
 * <pre>
 * S1 + S2;
 * if ( S1 == "abc" && S2 != "xyz") {
 *   S2 + S3;
 * } elseif ( S1 == "fred" ) {
 *     S2 + S4;
 * } else {
 *     S2 + S5,S6,S7;
 * }
 * S4,S5 + &S8;
 * </pre>
 *
 * @author gurjyan
 * @version 4.x
 * @since 5/29/15
 */
public class CompositionCompiler {

    /**
     * String that starts with a character and can have preceding number.
     */
    public static final String WORD = "([a-zA-Z_0-9-]*)";

    /**
     * Service canonical name.
     * Format: {@code dpe_name:container_name:engine_name}
     */
    public static final String SERV_NAME = ClaraComponent.SERVICE_NAME_REGEX;

    /**
     * Routing statement. Example:
     * <ul>
     * <li>{@code S1 + S2 + S3;}</li>
     * <li>{@code S1 , S2 + S3;}</li>
     * <li>{@code S1 + S2 , S3;}</li>
     * <li>{@code S1 , S2 + &S3;}</li>
     * <li>{@code S1;}</li>
     * </ul>
     * Note that regular expression does not include end of statement operator.
     */
    public static final String STATEMENT = SERV_NAME + "(?:," + SERV_NAME + ")*"
                                         + "(?:(?:\\+&?" + SERV_NAME + ")*|(?:\\+" + SERV_NAME
                                         + "(?:," + SERV_NAME + ")*)*)";

    /**
     * Clara simple Condition. Example:
     * <li>{@code Service == "state_name}"</li>
     * <li>{@code Service != "state_name"</li>
     */
    public static final String SIMP_COND = SERV_NAME + "(?:==|!=)\"" + WORD + "\"";

    /**
     * Clara complex Condition. Example:
     * <li>{@code (Service1 == "state_name1" && Service2 == "state_name2)}</li>
     * <li>{@code (Service1 == "state_name1" !!
     *             Service2 == "state_name2" !!
     *             Service2 != "state_name3")}</li>
     */
    public static final String COMP_COND = SIMP_COND + "(?:(?:&&|!!)" + SIMP_COND + ")*";

    /**
     * Clara conditional statement.
     */
    public static final String COND = "(?:(?:\\}?if|\\}elseif)\\(" + COMP_COND + "\\)\\{"
                                    + STATEMENT + ")|(?:\\}else\\{" + STATEMENT + ")";

    private static final Pattern STATEMENT_PATTERN = Pattern.compile(STATEMENT);

    private static final Pattern COND_PATTERN = Pattern.compile(COND);

    public Set<Instruction> instructions = new LinkedHashSet<>();

    // The name of the service relative to which compilation will be done.
    private String myServiceName;

    /**
     * Constructor.
     *
     * @param service the name of the service relative to which to compile.
     */
    public CompositionCompiler(String service) {
        myServiceName = service;
    }

    public void compile(String iCode) throws ClaraException {

        // This is a new request reset
        reset();

        // Create a single string with no blanks
        String pCode = noBlanks(iCode);

        // split single string program using
        // Clara ; end of statement operator
        // in case of the conditional statements the }
        // scope operator can be the first after tokenize with,
        // so preProcess will take of that too.
        Set<String> pp = preProcess(pCode);

        // start analysing and building compiled instructions
        String[] ppi = pp.toArray(new String[0]);

        int i = -1;
        while (++i < ppi.length) {

            String scs1 = ppi[i];

            // conditional statement
            if (scs1.startsWith("if(")
                    || scs1.startsWith("}if(")
                    || scs1.startsWith("}elseif(")
                    || scs1.startsWith("}else")) {

                Instruction instruction = parseCondition(scs1);

                // ADB: assuming the intention here was to allow multiple
                // statements under one conditional -- otherwise why make it
                // nested?
                while (++i < ppi.length) {

                    String scs2 = ppi[i];

                    if (!scs2.startsWith("}")
                            && !scs2.startsWith("if(")
                            && !scs2.startsWith("}if(")
                            && !scs2.startsWith("}elseif(")
                            && !scs2.startsWith("}else")) {

                        // if ignoring the conditional, then ignore its statements also
                        if (instruction != null) {
                            parseConditionalStatement(scs2, instruction);
                        }
                    } else {
                        break;
                    }
                }
                if (instruction != null) {
                    instructions.add(instruction);
                }
                i--;
                // routing statement
            } else {
                parseStatement(scs1);
            }
        }

        if (instructions.isEmpty()) {
            throw new ClaraException("Composition is irrelevant for a service.");
        }

    }

    /**
     * Tokenize code by Clara end of statement operator ";".
     *
     * @param pCode code string
     * @return set of tokens, including simple routing statements as well as conditionals
     * @throws ClaraException
     */
    private Set<String> preProcess(String pCode) throws ClaraException {
        if (!pCode.contains(";") && !pCode.endsWith(";")) {
            throw new ClaraException("Syntax error in the Clara routing program. "
                    + "Missing end of statement operator = \";\"");
        }
        Set<String> r = new LinkedHashSet<>();
        // tokenize by ;
        StringTokenizer st = new StringTokenizer(pCode, ";");

        while (st.hasMoreTokens()) {

            String text = st.nextToken();

            // ADB: by stripping out the closing brace here you lose the ability
            //      to correctly parse multiple statements in a block
            //
            // this will get read of very last }
            //text = CUtility.removeFirst(text, "}");

            // ignore
            if (!text.equals("") && !text.equals("}")) {
                r.add(text);
            }
        }
        return r;
    }

    private boolean parseStatement(String iStmt) throws ClaraException {
        boolean b = false;
        Instruction ti = new Instruction(myServiceName);

        // ignore a leading }
        iStmt = CompositionParser.removeFirst(iStmt, "}");

        // unconditional routing statement
        try {
            Matcher m = STATEMENT_PATTERN.matcher(iStmt);

            if (m.matches()) {

                // ignore conditional statements not concerning me
                if (!iStmt.contains(myServiceName)) {
                    return false;
                }

                Statement ts = new Statement(iStmt, myServiceName);
                ti.addUnCondStatement(ts);
                instructions.add(ti);
                b = true;
            } else {
                throw new ClaraException("Syntax error in the Clara routing program. "
                        + "Malformed routing statement");
            }
        } catch (PatternSyntaxException e) {
            System.err.println(e.getDescription());
        }
        return b;
    }

    private boolean parseConditionalStatement(String iStmt, Instruction ti) throws ClaraException {
        boolean b = false;

        // unconditional routing statement
        Matcher m = STATEMENT_PATTERN.matcher(iStmt);
        if (m.matches()) {

            // ignore conditional statements not concerning me
            if (!iStmt.contains(myServiceName)) {
                return false;
            }

            Statement ts = new Statement(iStmt, myServiceName);

            // inside condition, so add as the corect type
            if (ti.getIfCondition() != null) {
                ti.addIfCondStatement(ts);
            } else if (ti.getElseifCondition() != null) {
                ti.addElseifCondStatement(ts);
            } else {
                ti.addElseCondStatement(ts);
            }
            b = true;
        } else {
            throw new ClaraException("Syntax error in the Clara routing program. "
                    + "Malformed routing statement");
        }

        return b;
    }

    private Instruction parseCondition(String iCnd) throws ClaraException {
        Instruction ti;

        Matcher m = COND_PATTERN.matcher(iCnd);

        if (m.matches()) {
            try {
                // get first statement and analyze it
                String statementStr = iCnd.substring(iCnd.indexOf("{"));

                // ignore conditions not concerning me
                if (!statementStr.contains(myServiceName)) {
                    return null;
                }

                Statement ts = new Statement(statementStr, myServiceName);

                // create Instruction
                ti = new Instruction(myServiceName);
                if (iCnd.startsWith("}if(") || iCnd.startsWith("if(")) {
                    String conditionStr = iCnd.substring(iCnd.indexOf("(") + 1,
                                                         iCnd.lastIndexOf(")"));
                    Condition tc = new Condition(conditionStr, myServiceName);
                    ti.setIfCondition(tc);
                    ti.addIfCondStatement(ts);
                } else if (iCnd.startsWith("}elseif(")) {
                    String conditionStr = iCnd.substring(iCnd.indexOf("(") + 1,
                                                         iCnd.lastIndexOf(")"));
                    Condition tc = new Condition(conditionStr, myServiceName);
                    ti.setElseifCondition(tc);
                    ti.addElseifCondStatement(ts);
                } else if (iCnd.startsWith("}else")) {
                    ti.addElseCondStatement(ts);
                }
            } catch (StringIndexOutOfBoundsException e) {
                throw new ClaraException("Syntax error in the Clara routing program. "
                            + "Missing parenthesis");
            }
        } else {
            throw new ClaraException("Syntax error in the Clara routing program. "
                    + "Malformed conditional statement");
        }
        return ti;
    }

    public void reset() {
        instructions.clear();
    }

    /**
     * Returns an entire program one consequent string.
     *
     * @param x input program text
     * @return single string representation of the program
     */
    private String noBlanks(String x) {
        StringTokenizer st = new StringTokenizer(x);
        StringBuilder sb = new StringBuilder();
        while (st.hasMoreTokens()) {
            sb.append(st.nextToken().trim());
        }
        return sb.toString();
    }

    public Set<Instruction> getInstructions() {
        return instructions;
    }

    public Set<String> getUnconditionalLinks() {
        Set<String> outputs = new HashSet<>();
        for (Instruction inst : instructions) {
            // NOTE: instruction routing statements are exclusive: will be
            //       either unconditional, if, elseif, or else.
            if (inst.getUnCondStatements() != null && !inst.getUnCondStatements().isEmpty()) {
                for (Statement stmt : inst.getUnCondStatements()) {
                    outputs.addAll(stmt.getOutputLinks());
                }
            }
        }
        return outputs;
    }

    public Set<String> getLinks(ServiceState ownerSS, ServiceState inputSS) {

        Set<String> outputs = new HashSet<>();

        // The list of routing instructions supply the output links
        //
        // Instructions with unconditional routing always provide output links
        //
        // Conditional routing evaluates a sequence of instructions:
        //
        //   * one if-conditional instruction
        //   * zero-or-more else-if conditional instructions
        //   * zero-or-one else conditional instruction
        //
        // In a sequence, only the first conditional to evaluate to "true"
        // supplies output links

        // keep track of when one of the if/elseif/else conditions has been chosen
        boolean inCondition = false;
        boolean conditionChosen = false;

        for (Instruction inst : instructions) {
            // NOTE: instruction routing statements are exclusive: will be
            //       either unconditional, if, elseif, or else.
            if (inst.getUnCondStatements() != null && !inst.getUnCondStatements().isEmpty()) {
                // no longer in a conditional now
                inCondition = false;
                for (Statement stmt : inst.getUnCondStatements()) {
                    outputs.addAll(stmt.getOutputLinks());
                }
                continue;
            }

            if (inst.getIfCondition() != null) {
                inCondition = true;
                conditionChosen = false;
                if (inst.getIfCondition().isTrue(ownerSS, inputSS)) {
                    conditionChosen = true;
                    for (Statement stmt : inst.getIfCondStatements()) {
                        outputs.addAll(stmt.getOutputLinks());
                    }
                }
                continue;
            }

            // must be in a conditional already to process an elseif or else
            if (inCondition && !conditionChosen) {
                if (inst.getElseifCondition() != null) {
                    if (inst.getElseifCondition().isTrue(ownerSS, inputSS)) {
                        conditionChosen = true;
                        for (Statement stmt : inst.getElseifCondStatements()) {
                            outputs.addAll(stmt.getOutputLinks());
                        }
                    }
                    continue;
                }

                if (!inst.getElseCondStatements().isEmpty()) {
                    conditionChosen = true;
                    for (Statement stmt : inst.getElseCondStatements()) {
                        outputs.addAll(stmt.getOutputLinks());
                    }
                }
            }
        }

        return outputs;
    }
}
