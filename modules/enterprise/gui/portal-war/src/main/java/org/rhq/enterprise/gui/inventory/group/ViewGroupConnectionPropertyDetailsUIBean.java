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
package org.rhq.enterprise.gui.inventory.group;

import java.util.Map;

import javax.faces.model.DataModel;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.composite.ConfigurationUpdateComposite;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.configuration.propset.ConfigurationSet;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ViewGroupConnectionPropertyDetailsUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ViewGroupConnectionPropertyDetailsUIBean";
    public static final String VIEW_ID = "/rhq/group/inventory/view-plugin-configuration-update-details.xhtml";

    private ResourceGroup resourceGroup;
    private Map<Integer, Configuration> pluginConfigurations;
    private ConfigurationSet configurationSet;

    private ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListGroupConnectionPropertyUpdateDetailsDataModel(
                PageControlView.GroupConnectionPropertyUpdateDetails, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    public ConfigurationSet getConfigurationSet() {
        if (this.configurationSet == null) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            ResourceGroup group = EnterpriseFacesContextUtility.getResourceGroup();
            int groupPluginConfigurationUpdateId = getPluginResourceConfigurationUpdateId();

            this.pluginConfigurations = this.configurationManager.getPluginConfigurationMapForGroupUpdate(subject,
                groupPluginConfigurationUpdateId);
            this.configurationSet = GroupPluginConfigurationUtility.buildConfigurationSet(subject, group,
                this.pluginConfigurations);
        }
        return configurationSet;
    }

    private int getPluginResourceConfigurationUpdateId() {
        return FacesContextUtility.getRequiredRequestParameter("apcuId", Integer.class);
    }

    private class ListGroupConnectionPropertyUpdateDetailsDataModel extends
        PagedListDataModel<ConfigurationUpdateComposite> {
        private ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

        public ListGroupConnectionPropertyUpdateDetailsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<ConfigurationUpdateComposite> fetchPage(PageControl pc) {
            ResourceGroup requestResourceGroup = EnterpriseFacesContextUtility.getResourceGroup();

            if (requestResourceGroup == null) {
                requestResourceGroup = resourceGroup;
            } else {
                resourceGroup = requestResourceGroup;
            }

            int groupPluginConfigurationUpdateId = FacesContextUtility.getRequiredRequestParameter("apcuId",
                Integer.class);
            PageList<ConfigurationUpdateComposite> childUpdates = configurationManager
                .findPluginConfigurationUpdateCompositesByParentId(groupPluginConfigurationUpdateId, pc);

            return childUpdates;
        }
    }
}