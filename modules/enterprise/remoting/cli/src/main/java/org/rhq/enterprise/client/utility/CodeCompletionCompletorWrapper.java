package org.rhq.enterprise.client.utility;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import jline.Completor;
import jline.ConsoleReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.scripting.CodeCompletion;

public class CodeCompletionCompletorWrapper implements Completor {

    private static final Log LOG = LogFactory.getLog(CodeCompletionCompletorWrapper.class);

    private CodeCompletion completion;
    private ChangeRegisteringPrintWriter output;
    private ConsoleReader consoleReader;

    public CodeCompletionCompletorWrapper(CodeCompletion completion, PrintWriter output, ConsoleReader consoleReader) {
        this.completion = completion;
        this.output = new ChangeRegisteringPrintWriter(output);
        this.consoleReader = consoleReader;
    }

    @Override
    public int complete(String buffer, int cursor, List candidates) {
        String start = this.consoleReader.getCursorBuffer().getBuffer().toString();

        output.setChanged(false);
        int ret = completion.complete(output, buffer, cursor, candidates);

        if (output.isChanged()) {
            try {
                output.flush();
                consoleReader.printNewline();
                consoleReader.drawLine();
            } catch (IOException e) {
                LOG.debug("Failed to draw a console reader line.", e);
            }
        }

        return ret;
    }
}
