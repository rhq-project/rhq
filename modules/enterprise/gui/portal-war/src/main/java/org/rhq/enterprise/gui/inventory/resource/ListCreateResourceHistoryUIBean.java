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

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.CreateResourceHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * JSF Bean for rendering the create child resource requests for a resource.
 *
 * @author Jason Dobies
 */
public class ListCreateResourceHistoryUIBean extends PagedDataTableUIBean {
    // Accessors  --------------------------------------------

    public int getCreateHistoryCount() {
        ResourceFactoryManagerLocal resourceFactoryManager = LookupUtil.getResourceFactoryManager();
        Resource parentResource = EnterpriseFacesContextUtility.getResourceIfExists();
        int resourceHistoryCount = resourceFactoryManager.getCreateChildResourceHistoryCount(parentResource.getId(),
            null, null);
        return resourceHistoryCount;
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListCreateResourceRequestsDataModel(PageControlView.CreateResourceHistory,
                "ListCreateResourceHistoryUIBean");
        }

        return dataModel;
    }

    // Inner Classes  --------------------------------------------

    /**
     * Data model for the resource's resource requests.
     */
    private class ListCreateResourceRequestsDataModel extends PagedListDataModel<CreateResourceHistory> {
        public ListCreateResourceRequestsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<CreateResourceHistory> fetchPage(PageControl pageControl) {
            Subject user = EnterpriseFacesContextUtility.getSubject();
            Resource parentresource = EnterpriseFacesContextUtility.getResourceIfExists();
            ResourceFactoryManagerLocal resourceFactoryManager = LookupUtil.getResourceFactoryManager();

            PageList<CreateResourceHistory> pageList = resourceFactoryManager.findCreateChildResourceHistory(user,
                parentresource.getId(), null, null, pageControl);

            return pageList;
        }
    }
}