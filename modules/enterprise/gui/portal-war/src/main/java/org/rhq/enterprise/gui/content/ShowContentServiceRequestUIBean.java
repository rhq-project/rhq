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
import javax.servlet.http.HttpServletRequest;
import org.rhq.core.domain.content.ContentServiceRequest;
import org.rhq.core.domain.content.InstalledPackageHistory;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Bean responsible for displaying the details of a specific content service request. This includes making available
 * the request itself, in addition to providing a pagable model for the package audit produced by the request.
 *
 * @author Jason Dobies
 */
public class ShowContentServiceRequestUIBean extends PagedDataTableUIBean {

    private ContentServiceRequest contentServiceRequest;
    private int selectedRequestId;

    public ContentServiceRequest getContentServiceRequest() {
        if (contentServiceRequest == null) {
            ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();
            contentServiceRequest = contentUIManager.getContentServiceRequest(getSelectedRequestId());
        }

        return contentServiceRequest;
    }

    public void setContentServiceRequest(ContentServiceRequest contentServiceRequest) {
        this.contentServiceRequest = contentServiceRequest;
    }

    public int getSelectedRequestId() {
        if (selectedRequestId == 0) {
            HttpServletRequest request = FacesContextUtility.getRequest();
            selectedRequestId = Integer.parseInt(request.getParameter("selectedRequestId"));
        }

        return selectedRequestId;
    }

    public void setSelectedRequestId(int selectedRequestId) {
        this.selectedRequestId = selectedRequestId;
    }

    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel =
                new ShowContentServiceRequestDataModel(PageControlView.InstalledPackageHistoryList,
                    "ShowContentServiceRequestUIBean");
        }

        return dataModel;
    }

    private class ShowContentServiceRequestDataModel extends PagedListDataModel<InstalledPackageHistory> {

        private ShowContentServiceRequestDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        public PageList<InstalledPackageHistory> fetchPage(PageControl pc) {
            int requestId = getSelectedRequestId();

            ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();
            PageList<InstalledPackageHistory> history = contentUIManager.getInstalledPackageHistory(requestId, pc);

            return history;
        }
    }
}
