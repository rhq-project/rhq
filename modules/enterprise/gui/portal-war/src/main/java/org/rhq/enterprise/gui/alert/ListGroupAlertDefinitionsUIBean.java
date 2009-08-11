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
package org.rhq.enterprise.gui.alert;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.alert.GroupAlertDefinitionManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Joseph Marques
 */
public class ListGroupAlertDefinitionsUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ListGroupAlertDefinitionsUIBean";

    private GroupAlertDefinitionManagerLocal groupAlertDefinitionManager = LookupUtil.getGroupAlertDefinitionManager();

    public ListGroupAlertDefinitionsUIBean() {
    }

    public String createNewAlertDefinition() {
        return "createNewAlertDefinition";
    }

    public String deleteSelectedAlertDefinitions() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selectedAlertDefinitions = getSelectedAlertDefinitions();
        Integer[] alertDefinitionIds = getIntegerArray(selectedAlertDefinitions);

        try {
            int deleted = groupAlertDefinitionManager.removeGroupAlertDefinitions(subject, alertDefinitionIds);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Deleted " + deleted
                + " group alert definitions.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete group alert definitions.", e);
        }

        return "success";
    }

    public String enableSelectedAlertDefinitions() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selectedAlertDefinitions = getSelectedAlertDefinitions();
        Integer[] alertDefinitionIds = getIntegerArray(selectedAlertDefinitions);

        try {
            int enabled = groupAlertDefinitionManager.enableGroupAlertDefinitions(subject, alertDefinitionIds);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Enabled " + enabled
                + " group alert definitions.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to enable group alert definitions.", e);
        }

        return "success";
    }

    public String disableSelectedAlertDefinitions() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selectedAlertDefinitions = getSelectedAlertDefinitions();
        Integer[] alertDefinitionIds = getIntegerArray(selectedAlertDefinitions);

        try {
            int disabled = groupAlertDefinitionManager.disableGroupAlertDefinitions(subject, alertDefinitionIds);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Disabled " + disabled
                + " group alert definitions.");
        } catch (Exception e) {
            FacesContextUtility
                .addMessage(FacesMessage.SEVERITY_ERROR, "Failed to disable group alert definitions.", e);
        }

        return "success";
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListGroupAlertDefinitionsDataModel(PageControlView.GroupAlertDefinitionsList,
                MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ListGroupAlertDefinitionsDataModel extends PagedListDataModel<AlertDefinition> {
        public ListGroupAlertDefinitionsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<AlertDefinition> fetchPage(PageControl pc) {
            PageList<AlertDefinition> results = groupAlertDefinitionManager.findGroupAlertDefinitions(getSubject(),
                getResourceGroup().getId(), pc);
            return results;
        }
    }

    private String[] getSelectedAlertDefinitions() {
        return FacesContextUtility.getRequest().getParameterValues("selectedAlertDefinitions");
    }

    private Integer[] getIntegerArray(String[] input) {
        Integer[] output = new Integer[input.length];
        for (int i = 0; i < output.length; i++) {
            output[i] = Integer.valueOf(input[i]);
        }

        return output;
    }
}