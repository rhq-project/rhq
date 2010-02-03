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

import java.io.FileInputStream;
import java.io.IOException;

import jline.ConsoleReader;

public class JlineAgentInputReader implements AgentInputReader {

    private ConsoleReader jline;

    public JlineAgentInputReader() throws IOException {
        jline = new ConsoleReader();
    }

    public JlineAgentInputReader(FileInputStream fis) throws IOException {
        jline = new ConsoleReader(fis, null);
    }

    public String readLine() throws IOException {
        return jline.readLine();
    }
    
    public String readLine(char mask) throws IOException {
        return jline.readLine(Character.valueOf(mask));
    }
    
    public void close() throws IOException {
        jline.getInput().close();
    }
}
