/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation version 2 of the License.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package org.rhq.enterprise.gui.coregui.client.report;

import java.util.Set;
import java.util.TreeSet;

import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.ChangedEvent;
import com.smartgwt.client.widgets.grid.events.ChangedHandler;

/**
 * @author jsanda
 */
public class ExportChangeHandler implements ChangedHandler {

    private ListGrid listGrid;

    private String exportAttribute;

    private String idAttribute;

    private Set<Integer> resourceTypeIds = new TreeSet<Integer>();

    public ExportChangeHandler(ListGrid listGrid, String idAttribute, String exportAttribute) {
        this.listGrid = listGrid;
        this.idAttribute = idAttribute;
        this.exportAttribute = exportAttribute;
    }

    public Set<Integer> getResourceTypeIdsForExport() {
        return resourceTypeIds;
    }

    @Override
    public void onChanged(ChangedEvent event) {
        ListGridRecord record = listGrid.getRecord(event.getRowNum());
        Integer id = record.getAttributeAsInt(idAttribute);

        if (resourceTypeIds.contains(id)) {
            record.setAttribute(exportAttribute, false);
            resourceTypeIds.remove(id);
        } else {
            record.setAttribute(exportAttribute, true);
            resourceTypeIds.add(id);
        }
    }

}
