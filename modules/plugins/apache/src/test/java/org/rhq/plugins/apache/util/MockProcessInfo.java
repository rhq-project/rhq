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

package org.rhq.plugins.apache.util;

import org.rhq.core.system.ProcessInfo;

/**
 * Class used to pretend the apache process info in the runtime configuration tests.
 *
 * @author Lukas Krejci
 */
public class MockProcessInfo extends ProcessInfo {

    private long pid;
    private String[] commandLine;
    
    @Override
    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }
    
    @Override
    public String[] getCommandLine() {
        return commandLine;
    }
    
    public void setCommandLine(String[] commandLine) {
        this.commandLine = commandLine;
    }
}
