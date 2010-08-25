/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.common;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;

import org.rhq.core.domain.criteria.MeasurementDataTraitCriteria;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;

/**
 * A view that displays a non-paginated table of {@link org.rhq.core.domain.measurement.MeasurementDataTrait trait}s,
 * along with the ability to sort those traits.
 *
 * @author Ian Springer
 */
public abstract class AbstractMeasurementDataTraitListView extends Table {
    private static final SortSpecifier[] SORT_SPECIFIERS = new SortSpecifier[] { new SortSpecifier(
        MeasurementDataTraitCriteria.SORT_FIELD_NAME, SortDirection.ASCENDING) };

    public AbstractMeasurementDataTraitListView(String locatorId, String title,
        AbstractMeasurementDataTraitDataSource dataSource, Criteria criteria, String[] excludedFieldNames) {
        super(locatorId, title, criteria, SORT_SPECIFIERS, excludedFieldNames);
        setDataSource(dataSource);
    }

    @Override
    public AbstractMeasurementDataTraitDataSource getDataSource() {
        return (AbstractMeasurementDataTraitDataSource) super.getDataSource();
    }


    @Override
    protected void configureTable() {
        super.configureTable();

        ListGrid listGrid = getListGrid();
        listGrid.setSelectionType(SelectionStyle.SINGLE);

        // Set widths and cell formatters on the fields.
        listGrid.getField(MeasurementDataTraitCriteria.SORT_FIELD_NAME).setWidth("20%");
        listGrid.getField(MeasurementDataTraitCriteria.SORT_FIELD_VALUE).setWidth("60%");
        ListGridField timestampField = listGrid.getField(MeasurementDataTraitCriteria.SORT_FIELD_TIMESTAMP);
        timestampField.setWidth("20%");
        timestampField.setCellFormatter(new TimestampCellFormatter());
    }
}
