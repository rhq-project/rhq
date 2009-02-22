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
package org.rhq.enterprise.gui.configuration.group;

import javax.faces.model.DataModel;

import org.rhq.core.domain.configuration.composite.ConfigurationUpdateComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Joseph Marques
 */
public class GroupResourceConfigurationHistoryDetailsUIBean extends PagedDataTableUIBean {

    public static final String MANAGED_BEAN_NAME = "GroupResourceConfigurationHistoryDetailsUIBean";

    private ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListGroupResourceConfigurationUpdateDetailsDataModel(
                PageControlView.GroupResourceConfigurationUpdateDetails,
                GroupResourceConfigurationHistoryDetailsUIBean.MANAGED_BEAN_NAME);
        }
        return dataModel;
    }

    private class ListGroupResourceConfigurationUpdateDetailsDataModel extends
        PagedListDataModel<ConfigurationUpdateComposite> {

        public ListGroupResourceConfigurationUpdateDetailsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<ConfigurationUpdateComposite> fetchPage(PageControl pc) {
            int aggregateResourceConfigurationUpdateId = FacesContextUtility.getRequiredRequestParameter("arcuId",
                Integer.class);
            PageList<ConfigurationUpdateComposite> childUpdates = configurationManager
                .getResourceConfigurationUpdateCompositesByParentId(aggregateResourceConfigurationUpdateId, pc);

            return childUpdates;
        }
    }

}
