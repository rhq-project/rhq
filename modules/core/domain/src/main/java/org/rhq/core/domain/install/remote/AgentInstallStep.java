/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.core.domain.install.remote;

import java.io.Serializable;

/**
* @author Greg Hinkle
*/
public class AgentInstallStep implements Serializable {
    int resultCode;
    String command;
    String description;
    String result;
    long time;

    public AgentInstallStep() {
    }

    public AgentInstallStep(int resultCode, String command, String description, String result, long time) {
        this.resultCode = resultCode;
        this.command = command;
        this.description = description;
        this.result = result;
        this.time = time;
    }

    public int getResultCode() {
        return resultCode;
    }

    public String getDescription() {
        return description;
    }

    public String getCommand() {
        return command;
    }

    public String getResult() {
        return result;
    }

    public long getTime() {
        return time;
    }
}
