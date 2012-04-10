/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.core.pc.standaloneContainer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Handle command line history
 * @author Heiko W. Rupp
 */
public class History {

    private static final String HISTORY_HELP = "!! : repeat the last action\n" + //
        "!? : show the history of commands issued\n" + //
        "!h : show this help\n" + //
        "!nn : repeat history item with number nn\n" + //
        "!w fileName : write history to file with name fileName\n" + //
        "!dnn : delete history item with number nn";
    /** Holder for the command history */
    List<String> history = new ArrayList<String>(10);

    /**
     * Handle processing of the command history. This gives some csh like commands
     * and records the commands given. Nice side effect is the possibility to write the
     * history to disk and to use this later as input so that testing can be scripted.
     * Commands are:
     * <ul>
     * <li>!! : repeat the last action</li>
     * <li>!? : show the history</li>
     * <li>!h : show history help</li>
     * <li>!<i>nnn</i> : repeat history item with number <i>nnn</i></li>
     * <li>!w <i>file</i> : write the history to the file <i>file</i></li>
     * <li>!d<i>nnn</i> : delete the history item with number <i>nnn</i>
     * </ul>
     * @param answer the input given on the command line
     * @return a command or '!' if no command substitution from the history was possible.
     */
    public String handleHistory(String answer) {

        // Normal command - just return it
        if (!answer.startsWith("!")) {
            history.add(answer);
            return answer;
        }

        // History commands
        if (answer.startsWith("!?")) {
            for (int i = 0; i < history.size(); i++)
                System.out.println("[" + i + "]: " + history.get(i));
        } else if (answer.startsWith("!h")) {
            System.out.println(HISTORY_HELP);
            return "!";
        } else if (answer.startsWith("!!")) {
            String text = history.get(history.size() - 1);
            System.out.println(text);
            history.add(text);
            return text;
        } else if (answer.matches("![0-9]+")) {
            String id = answer.substring(1);
            Integer i;
            try {
                i = Integer.valueOf(id);
            } catch (NumberFormatException nfe) {
                System.err.println(id + " is no valid history position");
                return "!";
            }
            if (i > history.size()) {
                System.err.println(i + " is no valid history position");
                return "!";
            } else {
                String text = history.get(i);
                System.out.println(text);
                history.add(text);
                return text;
            }
        } else if (answer.startsWith("!w")) {
            String[] tokens = answer.split(" ");
            if (tokens.length < 2) {
                System.err.println("Not enough parameters. You need to give a file name");
            }
            File file = new File(tokens[1]);
            try {
                if (file.createNewFile()) {
                    if (file.canWrite()) {
                        Writer writer = new FileWriter(file);
                        try {
                            for (String item : history) {
                                writer.write(item);
                                writer.write("\n");
                            }
                        } finally {
                            writer.close();
                        }
                    } else {
                        System.err.println("Can not write to file " + file);
                    }
                }
            } catch (IOException ioe) {
                System.err.println("Saving the history to file " + file + " failed: " + ioe.getMessage());
            }
            return "!";
        } else if (answer.matches("!d[0-9]+")) {
            String id = answer.substring(2);
            Integer i;
            try {
                i = Integer.valueOf(id);
            } catch (NumberFormatException nfe) {
                System.err.println(id + " is no valid history position");
                return "!";
            }
            if (i > history.size()) {
                System.err.println(i + " is no valid history position");
                return "!";
            }
            history.remove(i.intValue());
            return "!";
        } else {
            System.err.println(answer + " is no valid history command");
            return "!";
        }
        return "!";
    }

    public int size() {
        return history.size();
    }

}
