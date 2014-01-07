package org.rhq.plugins.script;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lukas Krejci
 * @since 4.10
 */
public final class ScriptArgumentParser {

    private enum State {
        SPACE, ESCAPE, ARG, QUOTE
    }

    private ScriptArgumentParser() {
    }

    public static String[] parse(String args, char escape) {
        State state = State.SPACE;
        char activeQuote = '\u0000';
        List<String> parsedArgs = new ArrayList<String>();

        int i = 0;
        int len = args.length();
        StringBuilder arg = new StringBuilder();
        while (i < len) {
            char c = args.charAt(i++);

            switch (state) {
            case SPACE:
                boolean isNotWhitespace = false;
                if (!Character.isWhitespace(c)) {
                    insertArg(arg, parsedArgs);
                    isNotWhitespace = true;
                }

                if (c == escape) {
                    state = State.ESCAPE;
                } else if (isQuote(c)) {
                    activeQuote = c;
                    state = State.QUOTE;
                } else if (isNotWhitespace) {
                    arg.append(c);
                    state = State.ARG;
                }
                break;
            case ESCAPE:
                arg.append(c);
                state = State.ARG;
                break;
            case ARG:
                if (c == escape) {
                    state = State.ESCAPE;
                } else if (isQuote(c)) {
                    activeQuote = c;
                    state = State.QUOTE;
                } else if (!Character.isWhitespace(c)) {
                    arg.append(c);
                } else {
                    state = State.SPACE;
                }
                break;
            case QUOTE:
                if (c == activeQuote) {
                    state = State.ARG;
                } else if (c == escape) {
                    state = State.ESCAPE;
                } else {
                    arg.append(c);
                }
                break;
            }
        }

        //if we errored out on something, let's just put the result in as an arg. It is the responsibility of the
        //user to provide well-formed argument string.
        if (arg.length() > 0) {
            parsedArgs.add(arg.toString());
        }

        return parsedArgs.toArray(new String[parsedArgs.size()]);
    }

    private static void insertArg(StringBuilder bld, List<String> args) {
        if (bld.length() > 0) {
            args.add(bld.toString());
            bld.delete(0, bld.length());
        }
    }

    private static boolean isQuote(char c) {
        return c == '\'' || c == '"';
    }
}
