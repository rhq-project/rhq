/*
 * RHQ Management Platform
 * Copyright 2010-2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.coregui.client.inventory.common.detail.operation.schedule;

import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.coregui.client.components.table.EscapedHtmlCellFormatter;
import org.rhq.coregui.client.components.table.TableAction;
import org.rhq.coregui.client.components.table.TableSection;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.components.table.Table.TableActionInfo.ButtonColor;

/**
 * @author Ian Springer
 */
public abstract class AbstractOperationScheduleListView extends TableSection<AbstractOperationScheduleDataSource> {

    public AbstractOperationScheduleListView(AbstractOperationScheduleDataSource dataSource, String title) {
        super(title);

        setDataSource(dataSource);
    }

    protected abstract boolean hasControlPermission();

    @Override
    protected void configureTable() {
        ListGridField idField = new ListGridField(AbstractOperationScheduleDataSource.Field.ID, 70);

        ListGridField operationField = new ListGridField(
            AbstractOperationScheduleDataSource.Field.OPERATION_DISPLAY_NAME, 180);

        ListGridField subjectField = new ListGridField(AbstractOperationScheduleDataSource.Field.SUBJECT, 110);

        ListGridField nextFireTimeField = new ListGridField(AbstractOperationScheduleDataSource.Field.NEXT_FIRE_TIME,
            190);
        TimestampCellFormatter.prepareDateField(nextFireTimeField);

        ListGridField descriptionField = new ListGridField(AbstractOperationScheduleDataSource.Field.DESCRIPTION);
        descriptionField.setCellFormatter(new EscapedHtmlCellFormatter());

        setListGridFields(true, idField, operationField, subjectField, nextFireTimeField, descriptionField);

        addTableAction(MSG.common_button_new(), ButtonColor.BLUE, new TableAction() {
            public boolean isEnabled(ListGridRecord[] selection) {
                return hasControlPermission();
            }

            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                newDetails();
            }
        });

        addTableAction(MSG.common_button_delete(), getDeleteConfirmMessage(), ButtonColor.RED, new TableAction() {
            public boolean isEnabled(ListGridRecord[] selection) {
                int count = selection.length;
                return ((count >= 1) && hasControlPermission());
            }

            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                deleteSelectedRecords();
            }
        });

        super.configureTable();
    }

    @Override
    protected String getDetailsLinkColumnName() {
        return AbstractOperationScheduleDataSource.Field.OPERATION_DISPLAY_NAME;
    }

}
