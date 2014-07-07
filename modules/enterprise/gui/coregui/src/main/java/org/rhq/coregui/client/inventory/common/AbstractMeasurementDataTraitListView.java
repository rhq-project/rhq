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
package org.rhq.coregui.client.inventory.common;

import java.util.ArrayList;
import java.util.List;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.MeasurementDataTraitCriteria;
import org.rhq.coregui.client.components.table.TableAction;
import org.rhq.coregui.client.components.table.TableSection;
import org.rhq.coregui.client.components.table.Table.TableActionInfo.ButtonColor;

/**
 * A view that displays a non-paginated table of {@link org.rhq.core.domain.measurement.MeasurementDataTrait trait}s,
 * along with the ability to sort those traits.
 *
 * @author Ian Springer
 */
public abstract class AbstractMeasurementDataTraitListView extends TableSection<AbstractMeasurementDataTraitDataSource> {
    private static final String TITLE = MSG.view_metric_traits();
    private static final String[] EXCLUDED_FIELD_NAMES = new String[0];

    private static final SortSpecifier[] SORT_SPECIFIERS = new SortSpecifier[] { new SortSpecifier(
        MeasurementDataTraitCriteria.SORT_FIELD_DISPLAY_NAME, SortDirection.ASCENDING)
    //,new SortSpecifier(MeasurementDataTraitCriteria.SORT_FIELD_TIMESTAMP, SortDirection.DESCENDING)
    };

    public AbstractMeasurementDataTraitListView(AbstractMeasurementDataTraitDataSource dataSource, Criteria criteria) {
        super(TITLE, criteria, SORT_SPECIFIERS, EXCLUDED_FIELD_NAMES);
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

        ArrayList<ListGridField> dataSourceFields = getDataSource().getListGridFields();
        listGrid.setFields(dataSourceFields.toArray(new ListGridField[dataSourceFields.size()]));

        listGrid.setSelectionType(SelectionStyle.SINGLE);

        // Set widths and cell formatters on the fields.
        ListGridField displayNameField = listGrid.getField(MeasurementDataTraitCriteria.SORT_FIELD_DISPLAY_NAME);
        displayNameField.setWidth("20%");

        addTableAction(MSG.view_measureTable_getLive(), ButtonColor.BLUE, getLiveValueAction());
    }

    protected abstract TableAction getLiveValueAction();

    protected abstract ListGrid decorateLiveDataGrid(List<ListGridRecord> records);

    @Override
    protected String getDetailsLinkColumnName() {
        return MeasurementDataTraitCriteria.SORT_FIELD_DISPLAY_NAME;
    }

    public void showLiveData(List<ListGridRecord> records) {
        final Window liveDataWindow = new Window();
        liveDataWindow.setTitle(MSG.view_measureTable_live_title());
        liveDataWindow.setShowModalMask(true);
        liveDataWindow.setShowMinimizeButton(false);
        liveDataWindow.setShowMaximizeButton(true);
        liveDataWindow.setShowCloseButton(true);
        liveDataWindow.setShowResizer(true);
        liveDataWindow.setCanDragResize(true);
        liveDataWindow.setDismissOnEscape(true);
        liveDataWindow.setIsModal(true);
        liveDataWindow.setWidth(700);
        liveDataWindow.setHeight(425);
        liveDataWindow.setAutoCenter(true);
        liveDataWindow.centerInPage();
        liveDataWindow.addCloseClickHandler(new CloseClickHandler() {
            @Override
            public void onCloseClick(CloseClickEvent event) {
                liveDataWindow.destroy();
                refreshTableInfo();
            }
        });

        ListGrid liveDataGrid = decorateLiveDataGrid(records);
        liveDataWindow.addItem(liveDataGrid);
        liveDataWindow.show();
    }

}
