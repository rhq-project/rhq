/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.agent;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around PrintWriter that allows you to also divert the
 * output to a listener.
 *
 * Since all print(), println(), and some write() type methods
 * boil down to calls to the write() methods in PrintWriter,
 * this method merely overrrides the appropriate write() methods
 * so it can send the bytes being written to a listener,
 * prior to actually writing the bytes to the underlying
 * PrintWriter stream.
 * 
 * @author John Mazzitelli
 */
public class AgentPrintWriter extends PrintWriter {

    /**
     * The list of listeners that will also see what is bring written
     * by this print writer. Must access this in a thread-safe way.
     */
    private List<Writer> listeners = new ArrayList<Writer>();

    private String lineSeparator;

    public AgentPrintWriter(PrintStream out, boolean b) {
        super(out, b);
        determineLineSeparator();
    }

    public AgentPrintWriter(FileWriter fileWriter, boolean b) {
        super(fileWriter, b);
        determineLineSeparator();
    }

    /**
     * Adds the given writer object as a listener to this print writer.
     * Anything written to this print writer is also written to this
     * listener. You can have more than one listener. When done,
     * you must remove the listener via {@link #removeListener(Writer)}.
     * 
     * @param listener the writer to add as a listener
     */
    public void addListener(Writer listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Removes the given listener from the list. If the listener
     * was not listening to this agent print writer object, this
     * method does nothing and returns normally.
     * 
     * This method will never attempt to close the writer.
     * 
     * @param listener the listener to remove
     */
    public void removeListener(Writer listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    @Override
    public void write(char[] buf, int off, int len) {
        synchronized (listeners) {
            for (Writer listener : listeners) {
                try {
                    listener.write(buf, off, len);
                } catch (IOException e) {
                }
            }
        }
        super.write(buf, off, len);
    }

    @Override
    public void write(int c) {
        synchronized (listeners) {
            for (Writer listener : listeners) {
                try {
                    listener.write(c);
                } catch (IOException e) {
                }
            }
        }
        super.write(c);
    }

    @Override
    public void write(String s, int off, int len) {
        synchronized (listeners) {
            for (Writer listener : listeners) {
                try {
                    listener.write(s, off, len);
                } catch (IOException e) {
                }
            }
        }
        super.write(s, off, len);
    }

    @Override
    public void println() {
        synchronized (listeners) {
            for (Writer listener : listeners) {
                try {
                    listener.write(lineSeparator);
                } catch (IOException e) {
                }
            }
        }
        super.println();
    }

    @Override
    public void flush() {
        synchronized (listeners) {
            for (Writer listener : listeners) {
                try {
                    listener.flush();
                } catch (IOException e) {
                }
            }
        }
        super.flush();
    }

    private void determineLineSeparator() {
        lineSeparator = System.getProperty("line.separator");
    }
}
