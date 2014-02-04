/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.plugins.script;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lukas Krejci
 * @since 4.10
 */
public final class ScriptArgumentParser {

    private enum State {
        SPACE, ESCAPE, ARG, DOUBLE_QUOTE, DOUBLE_QUOTE_ESCAPE, SINGLE_QUOTE
    }

    private ScriptArgumentParser() {
    }

    public static String[] parse(String args, char escape) {
        State state = State.SPACE;
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
                } else if (c == '"') {
                    state = State.DOUBLE_QUOTE;
                } else if (c == '\'') {
                    state = State.SINGLE_QUOTE;
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
                } else if (c == '"') {
                    state = State.DOUBLE_QUOTE;
                } else if (c == '\'') {
                    state = State.SINGLE_QUOTE;
                } else if (!Character.isWhitespace(c)) {
                    arg.append(c);
                } else {
                    state = State.SPACE;
                }
                break;
            case DOUBLE_QUOTE:
                if (c == '"') {
                    state = State.ARG;
                } else if (c == escape) {
                    state = State.DOUBLE_QUOTE_ESCAPE;
                } else {
                    arg.append(c);
                }
                break;
            case DOUBLE_QUOTE_ESCAPE:
                if (c != '"' && c != escape) {
                    arg.append(escape);
                }
                arg.append(c);
                state = State.DOUBLE_QUOTE;
                break;
            case SINGLE_QUOTE:
                if (c == '\'') {
                    state = State.ARG;
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
}
