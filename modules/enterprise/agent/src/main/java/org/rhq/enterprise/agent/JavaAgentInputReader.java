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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import jline.ConsoleReader;

public class JavaAgentInputReader implements AgentInputReader {

    private final BufferedReader reader;
    private final boolean consoleInput;

    public JavaAgentInputReader() throws IOException {
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.consoleInput = true;
    }

    public JavaAgentInputReader(FileReader fr) throws IOException {
        this.reader = new BufferedReader(fr);
        this.consoleInput = false;
    }

    public boolean isConsole() {
        return this.consoleInput;
    }

    public String readLine() throws IOException {
        return this.reader.readLine();
    }
    
    public String readLineNoEcho() throws IOException {
        return this.reader.readLine(); // can't mask the input with this Java API
    }
    
    public void close() throws IOException {
        this.reader.close();
    }
}
