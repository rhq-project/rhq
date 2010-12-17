/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.schedule;

import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;

/**
 * @author Ian Springer
 */
public abstract class OperationScheduleListView extends TableSection<OperationScheduleDataSource> {    

    public OperationScheduleListView(String locatorId, OperationScheduleDataSource dataSource, String title) {
        super(locatorId, title);

        setDataSource(dataSource);
    }

    protected abstract boolean hasControlPermission();

    @Override
    protected void configureTable() {
        super.configureTable();

        ListGridField operationField = new ListGridField(OperationScheduleDataSource.Field.OPERATION_DISPLAY_NAME, 150);

        //ListGridField subjectField = new ListGridField(OperationScheduleDataSource.Field.SUBJECT, 150);

        //ListGridField jobTriggerField = new ListGridField(OperationScheduleDataSource.Field.JOB_TRIGGER, 300);

        ListGridField descriptionField = new ListGridField(OperationScheduleDataSource.Field.DESCRIPTION);

        setListGridFields(operationField, descriptionField);

        addTableAction(extendLocatorId("New"), MSG.common_button_new(), new TableAction() {
            public boolean isEnabled(ListGridRecord[] selection) {
                return hasControlPermission();
            }

            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                newDetails();
            }
        });

        addTableAction(extendLocatorId("Unschedule"), MSG.common_button_delete(), getDeleteConfirmMessage(),
            new TableAction() {
                public boolean isEnabled(ListGridRecord[] selection) {
                    int count = selection.length;
                    return ((count >= 1) && hasControlPermission());
                }

                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    // TODO: unschedule the selected schedule items
                }
            });        
    }

}
