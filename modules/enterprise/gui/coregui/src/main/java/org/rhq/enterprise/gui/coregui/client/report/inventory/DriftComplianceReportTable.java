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

package org.rhq.enterprise.gui.coregui.client.report.inventory;

import java.util.HashMap;
import java.util.List;

import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.grid.ListGridField;

import org.rhq.enterprise.gui.coregui.client.ImageManager;

/**
 * @author jsanda
 */
public class DriftComplianceReportTable extends InventorySummaryReportTable {

    public DriftComplianceReportTable(String locatorId) {
        super(locatorId);
        setDataSource(new DriftComplianceDataSource());
    }

    @Override
    protected List<ListGridField> createListGridFields() {
        List<ListGridField> fields = super.createListGridFields();
        fields.add(fields.size() - 1, createInComplianceField());

        return fields;
    }

    protected ListGridField createInComplianceField() {
        ListGridField field = new ListGridField(DriftComplianceDataSource.IN_COMPLIANCE, MSG
            .common_title_in_compliance());
        HashMap<String, String> complianceIcons = new HashMap<String, String>();
        complianceIcons.put("true", ImageManager.getAvailabilityIcon(true));
        complianceIcons.put("false", ImageManager.getAvailabilityIcon(false));
        field.setValueIcons(complianceIcons);
        field.setType(ListGridFieldType.ICON);
        field.setCanSortClientOnly(true);
        field.setWidth(100);

        return field;
    }

    @Override
    protected String getReportNameForResourceTypeURL() {
        return "DriftCompliance";
    }

    @Override
    protected String getReportNameForDownloadURL() {
        return "driftCompliance";
    }
}
