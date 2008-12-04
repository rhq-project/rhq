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
package org.rhq.enterprise.gui.ha;

import javax.faces.model.DataModel;

import org.rhq.core.domain.cloud.PartitionEvent;
import org.rhq.core.domain.cloud.PartitionEventDetails;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.cloud.PartitionEventManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jason Dobies
 */
public class ViewPartitionEventUIBean extends PagedDataTableUIBean {

    public static final String MANAGED_BEAN_NAME = "ViewPartitionEventUIBean";

    private PartitionEventManagerLocal partitionManager = LookupUtil.getPartitionEventManager();

    private PartitionEvent partitionEvent;

    public PartitionEvent getPartitionEvent() {
        if (partitionEvent == null) {
            int eventId = FacesContextUtility.getRequiredRequestParameter("eventId", Integer.class);
            partitionEvent = partitionManager.getPartitionEvent(getSubject(), eventId);
        }

        return partitionEvent;
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new PartitionEventDetailsDataModel(PageControlView.PartitionEventsDetailsView,
                MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class PartitionEventDetailsDataModel extends PagedListDataModel<PartitionEventDetails> {

        private PartitionEventDetailsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<PartitionEventDetails> fetchPage(PageControl pc) {
            EnterpriseFacesContextUtility.getSubject();

            PageList<PartitionEventDetails> eventDetailsPageList = partitionManager.getPartitionEventDetails(
                getSubject(), getPartitionEvent().getId(), pc);

            return eventDetailsPageList;
        }
    }
}
