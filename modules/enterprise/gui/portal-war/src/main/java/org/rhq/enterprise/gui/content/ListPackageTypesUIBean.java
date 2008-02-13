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

import java.util.List;
import javax.faces.model.DataModel;
import javax.faces.model.SelectItem;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
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
public class ListPackageTypesUIBean extends PagedDataTableUIBean {
    // Public  --------------------------------------------

    public List<PackageType> getPackageTypesForResourceType() {
        Resource resource = EnterpriseFacesContextUtility.getResource();

        ContentUIManagerLocal contentUIManager = LookupUtil.getContentUIManager();
        List<PackageType> packageTypes = contentUIManager.getPackageTypes(resource.getResourceType().getId());

        return packageTypes;
    }

    public SelectItem[] getSelectablePackageTypes() {
        List<PackageType> packageTypes = getPackageTypesForResourceType();
        SelectItem[] selection = new SelectItem[packageTypes.size()];

        int index = 0;
        for (PackageType type : packageTypes) {
            selection[index++] = new SelectItem(type, type.getDisplayName());
        }

        return selection;
    }

    // PagedDataTableUIBean Implementation  --------------------------------------------

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListPackageTypesDataModel(PageControlView.PackageTypesList, "ListPackageTypesUIBean");
        }

        return dataModel;
    }

    // Inner Classes  --------------------------------------------

    private class ListPackageTypesDataModel extends PagedListDataModel<PackageType> {
        // Constructors  --------------------------------------------

        public ListPackageTypesDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        // PagedListDataModel Implementation  --------------------------------------------

        @Override
        public PageList<PackageType> fetchPage(PageControl pc) {
            Resource resource = EnterpriseFacesContextUtility.getResource();
            ResourceType resourceType = resource.getResourceType();
            ContentUIManagerLocal manager = LookupUtil.getContentUIManager();

            PageList<PackageType> pageList = manager.getPackageTypes(resourceType.getId(), pc);

            return pageList;
        }
    }
}