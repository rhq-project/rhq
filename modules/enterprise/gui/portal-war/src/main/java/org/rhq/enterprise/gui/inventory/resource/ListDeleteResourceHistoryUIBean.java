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

import org.rhq.core.domain.resource.DeleteResourceHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * JSF Bean used to display a table of delete child resource requests for a parent resource.
 *
 * @author Jason Dobies
 */
public class ListDeleteResourceHistoryUIBean extends PagedDataTableUIBean {
    // Accessors  --------------------------------------------

    public int getDeleteHistoryCount() {
        ResourceFactoryManagerLocal resourceFactoryManager = LookupUtil.getResourceFactoryManager();
        Resource parentResource = EnterpriseFacesContextUtility.getResourceIfExists();

        int resourceHistoryCount = resourceFactoryManager.getDeleteChildResourceHistoryCount(parentResource.getId(),
            null, null);
        return resourceHistoryCount;
    }

    // PagedDataTableUIBean  --------------------------------------------

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListDeleteResourceRequestsDataModel(PageControlView.DeleteResourceHistory,
                "ListDeleteResourceHistoryUIBean");
        }

        return dataModel;
    }

    // Inner Classes  --------------------------------------------

    /**
     * Data model for the resource's resource requests.
     */
    private class ListDeleteResourceRequestsDataModel extends PagedListDataModel<DeleteResourceHistory> {
        public ListDeleteResourceRequestsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<DeleteResourceHistory> fetchPage(PageControl pageControl) {
            Resource parentresource = EnterpriseFacesContextUtility.getResourceIfExists();
            ResourceFactoryManagerLocal resourceFactoryManager = LookupUtil.getResourceFactoryManager();

            pageControl.initDefaultOrderingField("drh.id", PageOrdering.DESC);
            PageList<DeleteResourceHistory> pageList = resourceFactoryManager.getDeleteChildResourceHistory(
                parentresource.getId(), null, null, pageControl);

            return pageList;
        }
    }
}