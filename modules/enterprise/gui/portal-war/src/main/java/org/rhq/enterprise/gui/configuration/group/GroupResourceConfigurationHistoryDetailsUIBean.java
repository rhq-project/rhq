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
package org.rhq.enterprise.gui.configuration.group;

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

/**
 * @author Joseph Marques
 */
public class GroupResourceConfigurationHistoryDetailsUIBean extends PagedDataTableUIBean {
    public static final String VIEW_ID = "/rhq/group/configuration/details.xhtml";

    public static final String MANAGED_BEAN_NAME = "GroupResourceConfigurationHistoryDetailsUIBean";

    private ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

    private Map<Integer, Configuration> resourceConfigurations;
    private ConfigurationSet configurationSet;

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListGroupResourceConfigurationUpdateDetailsDataModel(
                PageControlView.GroupResourceConfigurationUpdateDetails,
                GroupResourceConfigurationHistoryDetailsUIBean.MANAGED_BEAN_NAME);
        }
        return dataModel;
    }

    public ConfigurationSet getConfigurationSet() {
        if (configurationSet == null) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            ResourceGroup group = EnterpriseFacesContextUtility.getResourceGroup();
            int groupResourceConfigurationUpdateId = getGroupResourceConfigurationUpdateId();

            this.resourceConfigurations = this.configurationManager
                .getResourceConfigurationMapForAggregateUpdate(groupResourceConfigurationUpdateId);
            this.configurationSet = GroupResourceConfigurationUtility.buildConfigurationSet(subject, group,
                resourceConfigurations);
        }
        return configurationSet;
    }

    private int getGroupResourceConfigurationUpdateId() {
        return FacesContextUtility.getRequiredRequestParameter("arcuId", Integer.class);
    }

    private class ListGroupResourceConfigurationUpdateDetailsDataModel extends
        PagedListDataModel<ConfigurationUpdateComposite> {

        public ListGroupResourceConfigurationUpdateDetailsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<ConfigurationUpdateComposite> fetchPage(PageControl pc) {
            int groupResourceConfigurationUpdateId = getGroupResourceConfigurationUpdateId();
            PageList<ConfigurationUpdateComposite> childUpdates = configurationManager
                .findResourceConfigurationUpdateCompositesByParentId(groupResourceConfigurationUpdateId, pc);

            return childUpdates;
        }
    }

}
