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
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ListAgentsUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ListAgentsUIBean";

    private AgentManagerLocal agentManager = LookupUtil.getAgentManager();

    public ListAgentsUIBean() {
    }

    @Override
    public DataModel getDataModel() {
        if (null == dataModel) {
            dataModel = new ListAgentsDataModel(PageControlView.AgentsList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ListAgentsDataModel extends PagedListDataModel<Agent> {
        public ListAgentsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<Agent> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();

            PageList<Agent> results = agentManager.getAgentsByServer(subject, null, pc);
            return results;
        }
    }

}