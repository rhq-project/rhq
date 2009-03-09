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
package org.rhq.enterprise.gui.common.framework;

import javax.faces.model.DataModel;
import javax.faces.model.SelectItem;

import org.richfaces.component.UIDataTable;
import org.richfaces.component.UIDatascroller;

import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;

public abstract class PagedDataTableUIBean extends EnterpriseFacesContextUIBean {
    private PageControl pageControl;
    protected DataModel dataModel;
    private UIDataTable dataTable;
    private UIDatascroller datascroller;
    private SelectItem[] pageSizes = new SelectItem[] { new SelectItem("15", "15"), new SelectItem("30", "30"),
        new SelectItem("45", "45") };

    public PageControl getPageControl(WebUser user, PageControlView view) {
        if (pageControl == null) {
            pageControl = user.getWebPreferences().getPageControl(view, getMinimumPageSize());
        }

        return pageControl;
    }

    public void setPageControl(WebUser user, PageControlView view, PageControl pageControl) {
        WebUserPreferences preferences = user.getWebPreferences();
        preferences.setPageControl(view, pageControl);
        this.pageControl = pageControl;
    }

    public UIDataTable getDataTable() {
        return dataTable;
    }

    public void setDataTable(UIDataTable dataTable) {
        this.dataTable = dataTable;
    }

    public void setDataModel(DataModel dataModel) {
        this.dataModel = dataModel;
    }

    public abstract DataModel getDataModel();

    public UIDatascroller getDatascroller() {
        return datascroller;
    }

    public void setDatascroller(UIDatascroller datascroller) {
        this.datascroller = datascroller;

        /* this can be externalized later, but at least
         * all PagedDataTableUIBeans will be consistent now
         */
        this.datascroller.setMaxPages(5);
    }

    public SelectItem[] getPageSizes() {
        return pageSizes;
    }

    public int getMinimumPageSize() {
        return 15;
    }
}