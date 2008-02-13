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
package org.rhq.enterprise.gui.common.sorting;

import javax.faces.component.UIData;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;

public class SortableColumnHeaderListener implements ActionListener {
    public void processAction(ActionEvent event) throws AbortProcessingException {
        SortableColumnHeader columnHeader = (SortableColumnHeader) event.getComponent();
        String sort = columnHeader.getSort();

        UIData table = FacesComponentUtility.getAncestorOfType(columnHeader, UIData.class);
        PagedListDataModel<?> model = (PagedListDataModel<?>) table.getValue();
        PageControl pc = model.getPageControl();

        pc.sortBy(sort);

        model.setPageControl(pc);
    }
}