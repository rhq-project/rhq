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

import java.util.HashMap;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;

import org.rhq.core.domain.cluster.Server;
import org.rhq.core.domain.cluster.Server.OperationMode;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.server.cluster.ClusterManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ViewServerUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ViewServerUIBean";

    private AgentManagerLocal agentManager = LookupUtil.getAgentManager();
    private ClusterManagerLocal clusterManager = LookupUtil.getClusterManager();
    private Server server;

    public ViewServerUIBean() {
        int serverId = FacesContextUtility.getRequiredRequestParameter("serverId", Integer.class);
        server = clusterManager.getServerById(serverId);
    }

    @Override
    public DataModel getDataModel() {
        if (null == dataModel) {
            dataModel = new ViewServerDataModel(PageControlView.ServerView, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    public Server getServer() {
        return server;
    }

    public String edit() {
        return "edit";
    }

    public String save() {
        try {
            clusterManager.updateServer(getSubject(), getServer());
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "The server has been updated; "
                + "changes to address/port/securePort require a restart to take affect.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Error: " + e.getMessage());
            return "edit"; // stay in edit mode on failure
        }

        return "success";
    }

    public String cancel() {
        return "success";
    }

    public Map<String, OperationMode> getOperationModes() {
        Map<String, OperationMode> modes = new HashMap<String, OperationMode>();
        for (OperationMode mode : OperationMode.values()) {
            modes.put(mode.name(), mode);
        }
        return modes;
    }

    private class ViewServerDataModel extends PagedListDataModel<Agent> {
        public ViewServerDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        @SuppressWarnings("unchecked")
        public PageList<Agent> fetchPage(PageControl pc) {
            int serverId = getServer().getId();
            PageList<Agent> results = agentManager.getAgentsByServer(getSubject(), serverId, pc);
            return results;
        }
    }

}