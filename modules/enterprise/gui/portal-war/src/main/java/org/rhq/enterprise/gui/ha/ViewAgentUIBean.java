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
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cloud.FailoverListDetails;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.cloud.CloudManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Bean responsible for the agent details display page. This bean will also provide a pagable list of the
 * failover servers for a particular agent.
 *
 * @author Jason Dobies
 */
public class ViewAgentUIBean extends PagedDataTableUIBean {

    public static final String MANAGED_BEAN_NAME = "ViewAgentUIBean";

    private AgentManagerLocal agentManager = LookupUtil.getAgentManager();
    private CloudManagerLocal cloudManager = LookupUtil.getCloudManager();

    private Agent agent;

    public Agent getAgent() {
        if (agent == null) {
            hasPermission();
            int agentId = FacesContextUtility.getRequiredRequestParameter("agentId", Integer.class);
            agent = agentManager.getAgentByID(agentId);
        }
        return agent;
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ViewAgentUIBeanDataModel(PageControlView.AgentFailoverListView, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ViewAgentUIBeanDataModel extends PagedListDataModel<FailoverListDetails> {

        private ViewAgentUIBeanDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<FailoverListDetails> fetchPage(PageControl pc) {
            int agentId = getAgent().getId();
            PageList<FailoverListDetails> pageList = cloudManager.getFailoverListDetailsByAgentId(agentId, pc);

            return pageList;
        }
    }

    /**
     * Throws a permission exception if the user is not allowed to access this functionality. 
     */
    private void hasPermission() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        if (!LookupUtil.getAuthorizationManager().hasGlobalPermission(subject, Permission.MANAGE_INVENTORY)) {
            throw new PermissionException("User [" + subject.getName()
                + "] does not have the proper permissions to view or manage agents");
        }
    }
}
