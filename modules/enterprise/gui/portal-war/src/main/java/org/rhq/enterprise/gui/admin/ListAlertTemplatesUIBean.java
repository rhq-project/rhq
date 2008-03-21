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
package org.rhq.enterprise.gui.admin;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.alert.AlertTemplateManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ListAlertTemplatesUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ListAlertTemplatesUIBean";

    private ResourceType resourceType;
    private AlertTemplateManagerLocal alertTemplateManager = LookupUtil.getAlertTemplateManager();
    private AuthorizationManagerLocal authorizationManager = LookupUtil.getAuthorizationManager();

    public ListAlertTemplatesUIBean() {
    }

    public String createNewAlertTemplate() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        boolean isAuthorized = authorizationManager.hasGlobalPermission(subject, Permission.MANAGE_SETTINGS);

        if (!isAuthorized) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "User is not authorized to create new alert templates");
            return "success"; // go back to the same page to show message
        }

        return "createNewAlertTemplate";
    }

    public String deleteSelectedAlertTemplates() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        String[] selectedAlertDefinitions = getSelectedAlertDefinitions();
        Integer[] alertDefinitionIds = getIntegerArray(selectedAlertDefinitions);

        try {
            alertTemplateManager.removeAlertTemplates(subject, alertDefinitionIds, true);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Deleted " + alertDefinitionIds.length
                + " alert definitions.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete alert definitions.", e);
        }

        return "success";
    }

    /* comment out enable/disable template-wide functionality until the UI can support this intuitively
     * public String enableSelectedAlertTemplates() { Subject   subject                  =
     * EnterpriseFacesContextUtility.getSubject(); String[]  selectedAlertDefinitions = getSelectedAlertDefinitions();
     * Integer[] alertDefinitionIds       = getIntegerArray(selectedAlertDefinitions);
     *
     * try {   alertTemplateManager.enableAlertTemplates( subject, alertDefinitionIds, false );
     * FacesContextUtility.addMessage( FacesMessage.SEVERITY_INFO,                                    "Enabled " +
     * alertDefinitionIds.length + " alert definitions." ); } catch (Exception e) {   FacesContextUtility.addMessage(
     * FacesMessage.SEVERITY_ERROR,                                    "Failed to enable alert definitions.", e); }
     * return "success"; }
     *
     * public String disableSelectedAlertTemplates() { Subject   subject                  =
     * EnterpriseFacesContextUtility.getSubject(); String[]  selectedAlertDefinitions = getSelectedAlertDefinitions();
     * Integer[] alertDefinitionIds       = getIntegerArray(selectedAlertDefinitions);
     *
     * try {   alertTemplateManager.disableAlertTemplates( subject, alertDefinitionIds, false );
     * FacesContextUtility.addMessage( FacesMessage.SEVERITY_INFO,                                    "Disabled " +
     * alertDefinitionIds.length + " alert definitions." ); } catch (Exception e) {   FacesContextUtility.addMessage(
     * FacesMessage.SEVERITY_ERROR,                                    "Failed to disable alert definitions.", e); }
     * return "success"; }
     */

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListAlertTemplatesDataModel(PageControlView.AlertTemplatesList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ListAlertTemplatesDataModel extends PagedListDataModel<AlertDefinition> {
        public ListAlertTemplatesDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        @SuppressWarnings("unchecked")
        public PageList<AlertDefinition> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            ResourceType requestResourceType = EnterpriseFacesContextUtility.getResourceTypeIfExists();
            AlertTemplateManagerLocal manager = LookupUtil.getAlertTemplateManager();

            if (requestResourceType == null) {
                requestResourceType = resourceType;
            } else {
                resourceType = requestResourceType;
            }

            PageList<AlertDefinition> results = null;
            results = manager.getAlertTemplates(subject, requestResourceType.getId(), pc);
            return results;
        }
    }

    private String[] getSelectedAlertDefinitions() {
        return FacesContextUtility.getRequest().getParameterValues("selectedAlertTemplates");
    }

    private Integer[] getIntegerArray(String[] input) {
        Integer[] output = new Integer[input.length];
        for (int i = 0; i < output.length; i++) {
            output[i] = Integer.valueOf(input[i]);
        }

        return output;
    }
}