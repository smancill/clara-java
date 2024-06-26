package org.jlab.clara.util.logging;

import java.io.PrintStream;

// checkstyle:off
/**
 * This class encapsulates the user's choice of output target.
 *
 *
 * @author ceki
 *
 */
@SuppressWarnings("ALL")
class OutputChoice {

    enum OutputChoiceType {
        SYS_OUT, SYS_ERR, FILE;
    }

    final OutputChoiceType outputChoiceType;
    final PrintStream targetPrintStream;


    OutputChoice(OutputChoiceType outputChoiceType) {
        if (outputChoiceType == OutputChoiceType.FILE) {
            throw new IllegalArgumentException();
        }
        this.outputChoiceType = outputChoiceType;
        this.targetPrintStream = null;
    }


    OutputChoice(PrintStream printStream) {
        this.outputChoiceType = OutputChoiceType.FILE;
        this.targetPrintStream = printStream;
    }


    PrintStream getTargetPrintStream() {
        switch (outputChoiceType) {
            case SYS_OUT:
                return System.out;
            case SYS_ERR:
                return System.err;
            case FILE:
                return targetPrintStream;
            default:
                throw new IllegalArgumentException();
        }

    }

}
