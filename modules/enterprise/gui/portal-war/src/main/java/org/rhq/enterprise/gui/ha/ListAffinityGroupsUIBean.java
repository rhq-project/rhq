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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.cluster.composite.AffinityGroupCountComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.server.cluster.AffinityGroupManagerBean;
import org.rhq.enterprise.server.cluster.AffinityGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ListAffinityGroupsUIBean extends PagedDataTableUIBean {

    private final Log log = LogFactory.getLog(AffinityGroupManagerBean.class);

    public static final String MANAGED_BEAN_NAME = "ListAffinityGroupsUIBean";

    private AffinityGroupManagerLocal affinityGroupManager = LookupUtil.getAffinityGroupManager();

    public String deleteSelectedAffinityGroups() {

        String[] selectedAffinityGroups = getSelectedAffinityGroups();
        Integer[] affinityGroupIds = getIntegerArray(selectedAffinityGroups);

        try {
            int removedCount = affinityGroupManager.delete(getSubject(), affinityGroupIds);
            FacesContextUtility
                .addMessage(FacesMessage.SEVERITY_INFO, "Removed [" + removedCount + "] AffinityGroups.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to remove selected AffinityGroups: "
                + e.getMessage());
            log.error(e);
        }

        return "success";
    }

    @Override
    public DataModel getDataModel() {
        if (null == dataModel) {
            dataModel = new ListServersDataModel(PageControlView.ListAffinityGroups, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ListServersDataModel extends PagedListDataModel<AffinityGroupCountComposite> {
        public ListServersDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<AffinityGroupCountComposite> fetchPage(PageControl pc) {
            PageList<AffinityGroupCountComposite> results = affinityGroupManager.getComposites(getSubject(), pc);
            return results;
        }
    }

    private String[] getSelectedAffinityGroups() {
        return FacesContextUtility.getRequest().getParameterValues("selectedAffinityGroups");
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