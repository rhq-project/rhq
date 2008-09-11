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
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cluster.PartitionEvent;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.cluster.PartitionEventManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jason Dobies
 */
public class ListPartitionEventsUIBean extends PagedDataTableUIBean {

    public static final String MANAGED_BEAN_NAME = "ListPartitionEventsUIBean";

    private PartitionEventManagerLocal partitionEventManager = LookupUtil.getPartitionEventManager();

    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListPartitionEventsDataModel(PageControlView.ListPartitionEventsView, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ListPartitionEventsDataModel extends PagedListDataModel<PartitionEvent> {

        private ListPartitionEventsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        public PageList<PartitionEvent> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();

            PageList<PartitionEvent> list = partitionEventManager.getPartitionEvents(subject, pc);

            return list;
        }
    }
}
