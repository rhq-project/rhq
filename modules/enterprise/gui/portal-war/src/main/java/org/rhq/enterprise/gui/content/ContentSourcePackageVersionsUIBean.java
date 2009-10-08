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
import org.rhq.core.domain.content.PackageVersionContentSource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ContentSourcePackageVersionsUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ContentSourcePackageVersionsUIBean";

    public ContentSourcePackageVersionsUIBean() {
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ContentSourcePackageVersionsDataModel(PageControlView.ContentSourcePackageVersionsList,
                MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ContentSourcePackageVersionsDataModel extends PagedListDataModel<PackageVersionContentSource> {
        public ContentSourcePackageVersionsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        @SuppressWarnings("unchecked")
        public PageList<PackageVersionContentSource> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            int id = Integer.valueOf(FacesContextUtility.getRequiredRequestParameter("id"));
            ContentSourceManagerLocal manager = LookupUtil.getContentSourceManager();

            PageList<PackageVersionContentSource> results;
            results = manager.getPackageVersionsFromContentSource(subject, id, pc);
            return results;
        }
    }
}