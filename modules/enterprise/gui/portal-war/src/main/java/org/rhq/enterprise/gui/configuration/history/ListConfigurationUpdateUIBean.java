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
package org.rhq.enterprise.gui.configuration.history;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.StringUtility;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.gui.configuration.resource.ExistingResourceConfigurationUIBean;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ListConfigurationUpdateUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ListConfigurationUpdateUIBean";

    private Resource resource;
    private Integer selectedResourceConfiguration;

    public ListConfigurationUpdateUIBean() {
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListResourcesDataModel(PageControlView.ConfigurationHistory, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    public Integer getSelectedResourceConfiguration() {
        return selectedResourceConfiguration;
    }

    public void setSelectedResourceConfiguration(Integer selectedResourceConfiguration) {
        this.selectedResourceConfiguration = selectedResourceConfiguration;
        FacesContextUtility.getFacesContext().getExternalContext().getRequestMap().remove(
            ViewResourceConfigurationUpdateUIBean.MANAGED_BEAN_NAME);
    }

    public String selectConfigurationToView() {
        AbstractResourceConfigurationUpdate selected;
        selected = (AbstractResourceConfigurationUpdate) FacesContextUtility.getRequest().getAttribute("item");
        setSelectedResourceConfiguration((selected != null) ? selected.getId() : null);
        return "success";
    }

    public String rollback() {
        String rollbackTo = null;

        try {
            rollbackTo = FacesContextUtility.getRequest().getParameter("selectedItems");

            Subject subject = EnterpriseFacesContextUtility.getSubject();
            Resource resource = EnterpriseFacesContextUtility.getResource();
            ConfigurationManagerLocal manager = LookupUtil.getConfigurationManager();

            manager.rollbackResourceConfiguration(subject, resource.getId(), Integer.parseInt(rollbackTo));

            // We've just updated the current Configuration, so clear the current Configuration that is cached in the
            // Session, so the config view/edit pages will not display a stale version of the Configuration.
            ExistingResourceConfigurationUIBean configUIBean = FacesContextUtility.getManagedBean(ExistingResourceConfigurationUIBean.class);
            configUIBean.clearConfiguration();

            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Rolled back resource configuration to version "
                + rollbackTo);
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "Failed to rollback resource configuration to version " + rollbackTo + ". Cause: "
                    + ThrowableUtil.getAllMessages(e, true));
        }

        return "success";
    }

    public String delete() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        ConfigurationManagerLocal manager = LookupUtil.getConfigurationManager();

        String[] selectedItems = FacesContextUtility.getRequest().getParameterValues("selectedItems");

        List<String> success = new ArrayList<String>();
        Map<String, String> failure = new HashMap<String, String>();

        String next = null;
        Integer doomed;
        for (int i = 0; i < selectedItems.length; i++) {
            try {
                next = selectedItems[i];
                doomed = Integer.valueOf(next);

                manager.purgeResourceConfigurationUpdate(subject, doomed, true);

                success.add(next);
            } catch (Exception e) {
                failure.put(next, ThrowableUtil.getAllMessages(e, true));
            }
        }

        if (success.size() > 0) {
            // one success message for all successful deletions
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO,
                "Deleted resource configuration update versions: " + StringUtility.getListAsDelimitedString(success));
        }

        for (Map.Entry<String, String> error : failure.entrySet()) {
            // one message per failed deletion (hopefully rare)
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "Failed to delete resource configuration update version " + error.getKey() + ". Cause: "
                    + error.getValue());
        }

        return "success";
    }

    public String compare() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "The 'compare' feature is not yet implemented.");

        return "success";
    }

    private class ListResourcesDataModel extends PagedListDataModel<ResourceConfigurationUpdate> {
        public ListResourcesDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<ResourceConfigurationUpdate> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            Resource requestResource = EnterpriseFacesContextUtility.getResourceIfExists();
            ConfigurationManagerLocal manager = LookupUtil.getConfigurationManager();

            if (requestResource == null) {
                requestResource = resource; // request not associated with a resource - use the resource we used before
            } else {
                resource = requestResource; // request switched the resource this UI bean is using
            }

            PageList<ResourceConfigurationUpdate> result;
            result = manager.getResourceConfigurationUpdates(subject, requestResource.getId(), pc);

            return result;
        }
    }
}