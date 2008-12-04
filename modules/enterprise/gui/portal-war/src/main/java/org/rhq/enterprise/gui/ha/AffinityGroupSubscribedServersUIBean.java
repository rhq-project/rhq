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
import org.rhq.core.domain.cloud.Server;
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
public class AffinityGroupSubscribedServersUIBean extends PagedDataTableUIBean {

    public static final String MANAGED_BEAN_NAME = "AffinityGroupSubscribedServersUIBean";

    private AffinityGroupManagerLocal affinityGroupManager = LookupUtil.getAffinityGroupManager();

    public String unsubscribeServers() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selectedServerIdStrings = FacesContextUtility.getRequest().getParameterValues(
            "selectedServersToUnsubscribe");

        // Parse the server IDs to ints
        Integer[] selectedServerIds = new Integer[selectedServerIdStrings.length];
        for (int ii = 0; ii < selectedServerIdStrings.length; ii++) {
            selectedServerIds[ii] = Integer.parseInt(selectedServerIdStrings[ii]);
        }

        // Update the group
        affinityGroupManager.removeServersFromGroup(subject, selectedServerIds);

        return "successOrFailure";
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new AffinityGroupSubscribedServersDataModel(PageControlView.AffinityGroupSubscribedServers,
                MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class AffinityGroupSubscribedServersDataModel extends PagedListDataModel<Server> {

        private AffinityGroupSubscribedServersDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<Server> fetchPage(PageControl pc) {
            int affinityGroupId = FacesContextUtility.getRequiredRequestParameter("affinityGroupId", Integer.class);
            PageList<Server> results = affinityGroupManager.getServerMembers(getSubject(), affinityGroupId, pc);

            return results;
        }
    }
}
