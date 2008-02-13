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
package org.rhq.core.domain.resource.composite;

import java.util.Date;
import org.rhq.core.domain.resource.Agent;

/**
 * Pairs an {@link Agent} with its {@link Agent#getLastAvailabilityReport() last report time}. The purpose of this is so
 * we can query the database for all agents and only have to keep in memory just the things we'll need initially, that
 * is the last available time and the agent ID.
 *
 * @author John Mazzitelli
 */
public class AgentLastAvailabilityReportComposite {
    private final int agentId;
    private final String agentName;
    private final String remoteEndpoint;
    private final Date lastAvailabilityReport;

    public AgentLastAvailabilityReportComposite(int agentId, String agentName, String remoteEndpoint, Date lastReport) {
        this.agentId = agentId;
        this.agentName = agentName;
        this.remoteEndpoint = remoteEndpoint;
        this.lastAvailabilityReport = lastReport;
    }

    public int getAgentId() {
        return agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getRemoteEndpoint() {
        return remoteEndpoint;
    }

    public Date getLastAvailabilityReport() {
        return lastAvailabilityReport;
    }

    @Override
    public int hashCode() {
        return this.agentId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof AgentLastAvailabilityReportComposite))) {
            return false;
        }

        return (this.agentId == ((AgentLastAvailabilityReportComposite) obj).agentId);
    }

    @Override
    public String toString() {
        return "AgentLastAvailableReportComposite: id=[" + this.agentId + "], name=[" + this.agentName
            + "], remote-endpoint=[" + this.remoteEndpoint + "], last-report=[" + this.lastAvailabilityReport + "]";
    }
}