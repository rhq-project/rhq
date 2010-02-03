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
     * Reads a line from the input console.
     * 
     * @return the line
     * @throws IOException
     */
    String readLine() throws IOException;
    
    /**
     * Reads a line from the input console, echoing out the 'mask' character as opposed to the character
     * typed by the user. If the console implementation does not support mask echoes, either the
     * user-entered character is masked or some other masking is performed in an implementation specific way.
     * 
     * @param mask character to echo back to the user when the user types in the line.
     * @return the line typed by the user
     * @throws IOException
     */
    String readLine(char mask) throws IOException;
    
    /**
     * Close the reader.
     * @throws IOException
     */
    void close() throws IOException;
}
