/*
 * RHQ Management Platform
 * Copyright (C) 2009-2010 Red Hat, Inc.
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
package org.rhq.enterprise.server.perspective;

import org.jboss.seam.annotations.In;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.gui.model.DefaultPageControlSettingsUIBean;

/**
 * A base class for Seam components that utilize the RHQ remote API to retrieve a paged data set of objects, which are
 * typically rendered using a dataTable component.
 *
 * @author Ian Springer
 */
public class AbstractPagedDataPerspectiveUIBean extends AbstractPerspectiveUIBean {
    @In(value = "DefaultPageControlSettingsUIBean", create = true)
    private DefaultPageControlSettingsUIBean defaultPageControlSettings;

    private PageControl pageControl;

    public PageControl getPageControl() {
        if (this.pageControl == null) {
            this.pageControl = getDefaultPageControl();
        }
        return pageControl;
    }

    public void setPageControl(PageControl pageControl) {
        this.pageControl = pageControl;
    }

    protected PageControl getDefaultPageControl() {
        return new PageControl(0, this.defaultPageControlSettings.getDefaultPageSize());
    }
}
