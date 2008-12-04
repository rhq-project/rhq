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
package org.rhq.enterprise.gui.ha;

import javax.faces.model.DataModel;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.cloud.AffinityGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jason Dobies
 */
public class AffinityGroupUnsubscribedAgentsUIBean extends PagedDataTableUIBean {

    public static final String MANAGED_BEAN_NAME = "AffinityGroupUnsubscribedAgentsUIBean";

    private AffinityGroupManagerLocal affinityGroupManager = LookupUtil.getAffinityGroupManager();

    public String subscribeAgents() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String affinityGroupIdString = FacesContextUtility.getRequest().getParameter("affinityGroupId");
        String[] selectedAgentIdStrings = FacesContextUtility.getRequest().getParameterValues(
            "selectedAgentsToSubscribe");

        // Parse the agent IDs to ints
        Integer[] selectedAgentIds = new Integer[selectedAgentIdStrings.length];
        for (int ii = 0; ii < selectedAgentIdStrings.length; ii++) {
            selectedAgentIds[ii] = Integer.parseInt(selectedAgentIdStrings[ii]);
        }

        // Update the group
        int affinityGroupId = Integer.parseInt(affinityGroupIdString);
        affinityGroupManager.addAgentsToGroup(subject, affinityGroupId, selectedAgentIds);

        return "successOrFailure";
    }

    @Override
    public DataModel getDataModel() {
        if (null == dataModel) {
            dataModel = new AffinityGroupUnsubscribedAgentsDataModel(PageControlView.AffinityGroupUnsubscribedAgents,
                MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class AffinityGroupUnsubscribedAgentsDataModel extends PagedListDataModel<Agent> {

        private AffinityGroupUnsubscribedAgentsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<Agent> fetchPage(PageControl pc) {
            int affinityGroupId = FacesContextUtility.getRequiredRequestParameter("affinityGroupId", Integer.class);
            PageList<Agent> results = affinityGroupManager.getAgentNonMembers(getSubject(), affinityGroupId, pc);
            return results;
        }
    }
}
