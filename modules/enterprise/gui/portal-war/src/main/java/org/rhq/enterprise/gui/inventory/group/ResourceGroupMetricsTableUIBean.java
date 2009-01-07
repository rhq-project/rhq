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

import javax.faces.model.DataModel;

import org.rhq.core.domain.resource.composite.ResourceHealthComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.inventory.MetricsTableUIBean;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This class supports paged Resource Group metric table display, specifically, the Health Summary of the group members.   
 * 
 * @author jay shaughnessy
 */
public class ResourceGroupMetricsTableUIBean extends PagedDataTableUIBean {

    public static final String MANAGED_BEAN_NAME = "ResourceGroupMetricsTableUIBean";

    MetricsTableUIBean metricsTableUIBean;

    public ResourceGroupMetricsTableUIBean() {
        metricsTableUIBean = new MetricsTableUIBean();
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ResourceGroupHealthSummaryDataModel(PageControlView.ResourceGroupHealthSummary,
                MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    protected class ResourceGroupHealthSummaryDataModel extends PagedListDataModel<ResourceHealthComposite> {
        private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

        public ResourceGroupHealthSummaryDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<ResourceHealthComposite> fetchPage(PageControl pageControl) {
            WebUser user = EnterpriseFacesContextUtility.getWebUser();

            PageList<ResourceHealthComposite> groupMemberHealthSummaries = resourceManager.getResourceHealth(user
                .getSubject(), metricsTableUIBean.getResourceGroupMemberIds(user), pageControl);

            return groupMemberHealthSummaries;
        }
    }

}