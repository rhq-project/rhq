/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

import java.io.IOException;

/**
 * Abstracts out the console input to allow us to use either a native console or a Java based console.
 *
 * @author John Mazzitelli
 */
public interface AgentInputReader {
    /**
     * Indicates if the input is coming from a console (i.e. keyboard) or if its coming from
     * a file.
     *
     * @return <code>true</code> if console is the source of the input; <code>false</code> if file is the source
     */
    boolean isConsole();

    /**
     * Reads a line from the input console.
     * 
     * @return the line
     * @throws IOException
     */
    String readLine() throws IOException;

    /**
     * Read a line of input, but do not echo back to the user what is being typed. This is used mainly when asking for
     * things like passwords.
     * If the console implementation does not support no-echo input, the input will be
     * output as the user typed it.
     * 
     * @return the line typed by the user
     * @throws IOException
     */
    String readLineNoEcho() throws IOException;

    /**
     * Close the reader.
     * @throws IOException
     */
    void close() throws IOException;
}
