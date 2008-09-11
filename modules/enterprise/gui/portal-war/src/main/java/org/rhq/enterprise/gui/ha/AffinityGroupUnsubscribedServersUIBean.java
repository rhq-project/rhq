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
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.cluster.AffinityGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.core.domain.cluster.Server;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.gui.util.FacesContextUtility;

/**
 * @author Jason Dobies
 */
public class AffinityGroupUnsubscribedServersUIBean extends PagedDataTableUIBean {

    public static final String MANAGED_BEAN_NAME = "AffinityGroupUnsubscribedServersUIBean";

    private AffinityGroupManagerLocal affinityGroupManager = LookupUtil.getAffinityGroupManager();

    public String subscribeServers() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String affinityGroupIdString = FacesContextUtility.getRequest().getParameter("affinityGroupId");
        String[] selectedServerIdStrings = FacesContextUtility.getRequest().getParameterValues("selectedServersToSubscribe");

        // Parse the server IDs to ints
        Integer[] selectedServerIds = new Integer[selectedServerIdStrings.length];
        for (int ii = 0; ii < selectedServerIdStrings.length; ii++) {
            selectedServerIds[ii] = Integer.parseInt(selectedServerIdStrings[ii]);
        }

        // Update the group
        int affinityGroupId = Integer.parseInt(affinityGroupIdString);
        affinityGroupManager.addServersToGroup(subject, affinityGroupId, selectedServerIds);

        return "successOrFailure";
    }

    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new AffinityGroupUnsubscribedServersDataModel(PageControlView.AffinityGroupUnsubscribedServers, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class AffinityGroupUnsubscribedServersDataModel extends PagedListDataModel<Server> {

        private AffinityGroupUnsubscribedServersDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        public PageList<Server> fetchPage(PageControl pc) {
            int affinityGroupId = FacesContextUtility.getRequiredRequestParameter("affinityGroupId", Integer.class);
            PageList<Server> results = affinityGroupManager.getServerNonMembers(getSubject(), affinityGroupId, pc);

            return results;
        }
    }
}
