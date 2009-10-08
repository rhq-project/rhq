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
import org.rhq.core.domain.content.InstalledPackageHistory;
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
 * Used to load the entire package audit trail for a resource.
 *
 * @author Jason Dobies
 */
public class AuditTrailUIBean extends PagedDataTableUIBean {

    private Resource resource;

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new AuditTrailDataModel(PageControlView.AuditTrailList, "AuditTrailUIBean");
        }

        return dataModel;
    }

    private class AuditTrailDataModel extends PagedListDataModel<InstalledPackageHistory> {

        private AuditTrailDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        public PageList<InstalledPackageHistory> fetchPage(PageControl pc) {
            Resource requestResource = EnterpriseFacesContextUtility.getResourceIfExists();
            ContentUIManagerLocal manager = LookupUtil.getContentUIManager();

            if (requestResource == null) {
                requestResource = resource; // request not associated with a resource - use the resource we used before
            } else {
                resource = requestResource; // request switched the resource this UI bean is using
            }

            PageList<InstalledPackageHistory> pageList =
                manager.getInstalledPackageHistoryForResource(resource.getId(), pc);

            return pageList;
        }
    }

}
