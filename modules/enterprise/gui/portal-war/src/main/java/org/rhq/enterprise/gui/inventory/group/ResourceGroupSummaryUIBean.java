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
import javax.faces.model.SelectItem;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ResourceGroupSummaryUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ResourceGroupSummaryUIBean";

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListResourceGroupMembersDataModel(PageControlView.MiniResourceGroupMemberList,
                MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    protected class ListResourceGroupMembersDataModel extends PagedListDataModel<ResourceWithAvailability> {
        private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

        public ListResourceGroupMembersDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<ResourceWithAvailability> fetchPage(PageControl pageControl) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            ResourceGroup resourceGroup = EnterpriseFacesContextUtility.getResourceGroup();

            PageList<ResourceWithAvailability> results = resourceManager
                .getImplicitResourceWithAvailabilityByResourceGroup(subject, resourceGroup, pageControl);

            return results;
        }
    }

    @Override
    public SelectItem[] getPageSizes() {
        // pageSize will be stuck to 5 items
        return new SelectItem[] { new SelectItem("5", "5") };
    }

    public int getMinimumPageSize() {
        return 5;
    }
}