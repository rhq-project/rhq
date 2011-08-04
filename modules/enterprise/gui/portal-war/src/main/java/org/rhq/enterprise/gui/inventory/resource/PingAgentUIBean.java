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
package org.rhq.enterprise.gui.inventory.resource;

import org.rhq.core.domain.resource.Agent;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A JSF managed bean that can ping an agent managing a RHQ resource.
 *
 * @author John Mazzitelli
 */
public class PingAgentUIBean {
    public static final String MANAGED_BEAN_NAME = "PingAgentUIBean";

    private Boolean pingResults = null;
    private Agent agent = null;
    private AgentManagerLocal agentManager = LookupUtil.getAgentManager();
    private SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();

    public PingAgentUIBean() {
    }

    public Agent getAgent() {
        if (agent == null) {
            getData();
        }

        return agent;
    }

    public boolean getPingResults() {
        if (pingResults == null) {
            getData();
        }

        return pingResults;
    }

    public String getRemoteEndpointWrapped() {
        // some browsers (firefox in particular) won't wrap unless you put breaks in the string
        Agent theAgent = getAgent();
        if (theAgent != null) {
            String remoteEndpoint = theAgent.getRemoteEndpoint();
            if (remoteEndpoint != null) {
                return remoteEndpoint.replaceAll("&", " &");
            } else {
                return "!no remote endpoint associated with this resource!";
            }
        } else {
            return "!no agent associated with this resource!";
        }
    }

    private void getData() {
        try {
            int resourceId = FacesContextUtility.getRequiredRequestParameter(ParamConstants.RESOURCE_ID_PARAM,
                Integer.class);
            agent = agentManager.getAgentByResourceId(subjectManager.getOverlord(), resourceId);
            if (agent == null) {
                throw new IllegalStateException("No agent is associated with the resource with id " + resourceId + ".");
            }

            AgentClient client = agentManager.getAgentClient(agent);
            pingResults = client.ping(5000L);
        } catch (Throwable t) {
            pingResults = Boolean.FALSE;
        }
    }
}