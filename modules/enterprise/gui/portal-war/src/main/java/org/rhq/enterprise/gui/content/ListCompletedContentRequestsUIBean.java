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
package org.rhq.enterprise.gui.content;

import javax.faces.model.DataModel;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.ContentRequestStatus;
import org.rhq.core.domain.content.ContentServiceRequest;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jason Dobies
 */
public class ListCompletedContentRequestsUIBean extends PagedDataTableUIBean {
    // Attributes  --------------------------------------------

    private Resource resource;

    // Constructors  --------------------------------------------

    public ListCompletedContentRequestsUIBean() {
    }

    // Public  --------------------------------------------

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListContentRequestsDataModel(PageControlView.ContentCompletedRequestsList,
                "ListCompletedContentRequestsUIBean");
        }

        return dataModel;
    }

    // Inner Classes  --------------------------------------------

    /**
     * Data model for the resource's artifact requests.
     */
    private class ListContentRequestsDataModel extends PagedListDataModel<ContentServiceRequest> {
        public ListContentRequestsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<ContentServiceRequest> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            Resource requestResource = EnterpriseFacesContextUtility.getResourceIfExists();
            ContentUIManagerLocal manager = LookupUtil.getContentUIManager();

            if (requestResource == null) {
                requestResource = resource; // request not associated with a resource - use the resource we used before
            } else {
                resource = requestResource; // request switched the resource this UI bean is using
            }

            PageList<ContentServiceRequest> pageList = manager.getContentRequestsWithNotStatus(subject, requestResource
                .getId(), ContentRequestStatus.IN_PROGRESS, pc);

            return pageList;
        }
    }
}