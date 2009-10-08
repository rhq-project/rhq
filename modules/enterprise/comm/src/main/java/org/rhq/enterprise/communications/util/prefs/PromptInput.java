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
package org.rhq.enterprise.communications.util.prefs;

import java.io.IOException;

/**
 * Implementations of this class will need to obtain input from the user and return that input as a String.
 *
 * @author John Mazzitelli
 */
public interface PromptInput {
    /**
     * Read a line of input, allowing the user to see what is being typed.
     *
     * @return the line of input entered by the user
     *
     * @throws IOException
     */
    String readLine() throws IOException;

    /**
     * Read a line of input, but do not echo back to the user what is being typed. This is used mainly when asking for
     * things like passwords in which you do not want people looking over the user's shoulder spying what is being
     * typed.
     *
     * @return the line of input entered by the user
     *
     * @throws IOException
     */
    String readLineNoEcho() throws IOException;
}