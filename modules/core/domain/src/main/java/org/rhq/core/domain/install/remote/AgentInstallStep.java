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
 * Encapsulates the results of a SSH command that was issued by the remote agent installer utility.
 * 
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class AgentInstallStep implements Serializable {
    private static final long serialVersionUID = 1L;

    private String command;
    private String description;
    private int resultCode;
    private String result;
    private long duration;

    public AgentInstallStep() {
    }

    public AgentInstallStep(String command, String description, int resultCode, String result, long duration) {
        this.command = command;
        this.description = description;
        this.resultCode = resultCode;
        this.result = result;
        this.duration = duration;
    }

    public String getCommand() {
        return command;
    }

    public String getDescription() {
        return description;
    }

    public int getResultCode() {
        return resultCode;
    }

    public String getResult() {
        return result;
    }

    public long getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AgentInstallStep [description=").append(description).append(", result=").append(result).append(
            ", resultCode=").append(resultCode).append(", duration=").append(duration).append("]");
        return builder.toString();
    }

}
