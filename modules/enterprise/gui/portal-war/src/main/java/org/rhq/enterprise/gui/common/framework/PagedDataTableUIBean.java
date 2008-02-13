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
import org.richfaces.component.UIDataTable;
import org.richfaces.component.UIDatascroller;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.legacy.WebUser;

public abstract class PagedDataTableUIBean {
    protected PageControl pageControl;
    protected DataModel dataModel;
    protected UIDataTable dataTable;
    protected UIDatascroller datascroller;

    public PageControl getPageControl(WebUser user, PageControlView view) {
        if (pageControl == null) {
            pageControl = user.getPageControl(view);
        }

        return pageControl;
    }

    public void setPageControl(WebUser user, PageControlView view, PageControl pageControl) {
        user.setPageControl(view, pageControl);
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
}