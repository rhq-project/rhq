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
package org.rhq.enterprise.gui.inventory.resource;

import javax.faces.model.DataModel;

import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * The JSF managed bean for the Groups Containing this Resource section of the Resource Inventory page
 * (/rhq/resource/inventory/view.xhtml).
 *
 * @author Ian Springer
 */
public class ListContainingGroupsUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ListContainingGroupsUIBean";

    public ListContainingGroupsUIBean() {
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListContainingGroupsDataModel(PageControlView.ContainingGroupsList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    protected class ListContainingGroupsDataModel extends PagedListDataModel<ResourceGroupComposite> {
        ListContainingGroupsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        private ResourceGroupManagerLocal resourceGroupManager = LookupUtil.getResourceGroupManager();

        @Override
        public PageList<ResourceGroupComposite> fetchPage(PageControl pageControl) {
            PageList<ResourceGroupComposite> resourceGroupComposites = this.resourceGroupManager
                .getResourceGroupMembers(getSubject(), null, null, null, null, getResource().getId(), pageControl);
            //.getResourceGroupsForResource(EnterpriseFacesContextUtility.getSubject(), resourceId, pageControl);
            return resourceGroupComposites;
        }
    }
}