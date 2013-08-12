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

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.composite.ServerWithAgentCountComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.cloud.TopologyManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ListServersUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ListServersUIBean";

    private TopologyManagerLocal topologyManager = LookupUtil.getTopologyManager();

    public ListServersUIBean() {
    }

    public String removeSelectedServers() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selected = getSelectedServers();
        Integer[] ids = getIntegerArray(selected);

        if (ids.length > 0) {
            try {
                topologyManager.deleteServers(subject, ids);

                FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Removed [" + ids.length
                    + "] servers from the cloud.");
            } catch (Exception e) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to remove selected servers.", e);
            }
        } else {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_WARN, "No servers selected.");
        }

        return "success";
    }

    public String updateServerManualMaintenance(boolean manualMaintenance) {
        // Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selected = getSelectedServers();
        Integer[] ids = getIntegerArray(selected);

        if (ids.length > 0) {
            try {
                topologyManager.updateServerManualMaintenance(EnterpriseFacesContextUtility.getSubject(), ids,
                    manualMaintenance);

                if (manualMaintenance) {
                    FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Set [" + ids.length
                        + "] servers' manual maintenance status.");
                } else {
                    FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Removed [" + ids.length
                        + "] servers' manual maintenance status.");
                }

            } catch (Exception e) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to set selected server modes", e);
            }
        }

        return "success";
    }

    public String setSelectedServersModeMaintenance() {
        return updateServerManualMaintenance(true);
    }

    public String setSelectedServersModeNormal() {
        return updateServerManualMaintenance(false);
    }

    @Override
    public DataModel getDataModel() {
        if (null == dataModel) {
            dataModel = new ListServersDataModel(PageControlView.ServersList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ListServersDataModel extends PagedListDataModel<ServerWithAgentCountComposite> {
        public ListServersDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        @SuppressWarnings("unchecked")
        public PageList<ServerWithAgentCountComposite> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();

            PageList<ServerWithAgentCountComposite> results = topologyManager.getServerComposites(subject, pc);
            return results;
        }
    }

    private String[] getSelectedServers() {
        return FacesContextUtility.getRequest().getParameterValues("selectedServers");
    }

    private Integer[] getIntegerArray(String[] input) {
        if (input == null) {
            return new Integer[0];
        }

        Integer[] output = new Integer[input.length];
        for (int i = 0; i < output.length; i++) {
            output[i] = Integer.valueOf(input[i]);
        }

        return output;
    }
}