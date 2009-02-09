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
package org.rhq.enterprise.gui.inventory.resource.history;

import javax.faces.model.DataModel;

import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;

/**
 * The JSF managed bean for showing resource availability history
 *
 * @author Greg Hinkle
 */
public class ListAvailabilityHistoryUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ListAvailabilityHistoryUIBean";

    public ListAvailabilityHistoryUIBean() {
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListAvailabilityHistoryDataModel(PageControlView.AvailabilityHistoryList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    protected class ListAvailabilityHistoryDataModel extends PagedListDataModel<Availability> {
        ListAvailabilityHistoryDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        private AvailabilityManagerLocal availabilityManager = LookupUtil.getAvailabilityManager();

        @Override
        public PageList<Availability> fetchPage(PageControl pageControl) {
            int resourceId = EnterpriseFacesContextUtility.getResource().getId();
            PageList<Availability> availabilities = this.availabilityManager
                .findByResource(EnterpriseFacesContextUtility.getSubject(), resourceId, pageControl);
            return availabilities;
        }
    }
}