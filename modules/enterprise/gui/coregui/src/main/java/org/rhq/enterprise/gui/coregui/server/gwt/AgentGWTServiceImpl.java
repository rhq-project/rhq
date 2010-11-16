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
package org.rhq.enterprise.gui.coregui.server.gwt;

import org.rhq.core.domain.resource.Agent;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.AgentGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Simeon Pinder
 */
public class AgentGWTServiceImpl extends AbstractGWTServiceImpl implements AgentGWTService {

    private static final long serialVersionUID = 1L;

    private AgentManagerLocal agentManager = LookupUtil.getAgentManager();

    @Override
    public Agent getAgentForResource(int resourceId) {
        try {
            //TODO: this needs security that was not handled before. No AgentManagerRemote... ??
            return SerialUtility.prepare(agentManager.getAgentByResourceId(getSessionSubject(), resourceId),
                "AgentService.getAgentForResource");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    //    public PageList<Availability> findAvailabilityForResource(int resourceId, PageControl pc) {
    //        try {
    //            return SerialUtility.prepare(availabilityManager.findAvailabilityForResource(getSessionSubject(),
    //                resourceId, pc), "AvailabilityService.findAvailabilityForResource");
    //        } catch (Exception e) {
    //            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
    //        }
    //    }
}
