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
package org.rhq.enterprise.gui.inventory.resource.history;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
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
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Joseph Marques
 */
public class ListPluginConfigurationUpdateUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ListPluginConfigurationUpdateUIBean";

    private Resource resource;
    private Integer selectedResourceConfiguration;

    public ListPluginConfigurationUpdateUIBean() {
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListResourcesDataModel(PageControlView.PluginConfigurationHistory, MANAGED_BEAN_NAME);
        }

        return dataModel;
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

                manager.purgePluginConfigurationUpdate(subject, doomed, true);

                success.add(next);
            } catch (Exception e) {
                failure.put(next, ThrowableUtil.getAllMessages(e, true));
            }
        }

        if (success.size() > 0) {
            // one success message for all successful deletions
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Deleted plugin configuration update versions: "
                + StringUtility.getListAsDelimitedString(success));
        }

        for (Map.Entry<String, String> error : failure.entrySet()) {
            // one message per failed deletion (hopefully rare)
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "Failed to delete plugin configuration update version " + error.getKey() + ". Cause: "
                    + error.getValue());
        }

        return "success";
    }

    private class ListResourcesDataModel extends PagedListDataModel<PluginConfigurationUpdate> {
        public ListResourcesDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<PluginConfigurationUpdate> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            Resource requestResource = EnterpriseFacesContextUtility.getResourceIfExists();
            ConfigurationManagerLocal manager = LookupUtil.getConfigurationManager();

            if (requestResource == null) {
                requestResource = resource; // request not associated with a resource - use the resource we used before
            } else {
                resource = requestResource; // request switched the resource this UI bean is using
            }

            PageList<PluginConfigurationUpdate> result;
            result = manager.getPluginConfigurationUpdates(subject, requestResource.getId(), null, null, pc);

            return result;
        }
    }
}