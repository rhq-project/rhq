package org.rhq.enterprise.client.utility;

import java.io.PrintWriter;
import java.util.List;

import jline.Completor;

import org.rhq.scripting.CodeCompletion;

public class CodeCompletionCompletorWrapper implements Completor {

    private CodeCompletion completion;
    private PrintWriter output;

    public CodeCompletionCompletorWrapper(CodeCompletion completion, PrintWriter output) {
        this.completion = completion;
        this.output = output;
    }

    @Override
    public int complete(String buffer, int cursor, List candidates) {
        return completion.complete(output, buffer, cursor, candidates);
    }
}
