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

import java.io.BufferedReader;
import java.io.IOException;

/**
 * A simple {@link PromptInput} implementation that delegates to a buffered reader. Note that because there is no Java
 * API that allows for reading input without echoing the typed input, this method's {@link #readLineNoEcho()} really
 * does echo (i.e. it is the same as {@link #readLine()}).
 *
 * @author John Mazzitelli
 */
public class BufferedReaderPromptInput implements PromptInput {
    private final BufferedReader reader;

    public BufferedReaderPromptInput(BufferedReader r) {
        reader = r;
    }

    public String readLine() throws IOException {
        return reader.readLine();
    }

    public String readLineNoEcho() throws IOException {
        return readLine();
    }
}