/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.enterprise.gui.coregui.client.alert.definitions;

import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.enterprise.gui.coregui.client.alert.AlertFormatUtility;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.Table.SelectionEnablement;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author John Mazzitelli
 */
public class ConditionsEditor extends LocatableVLayout {

    private final ConditionsAlertDefinitionForm form;
    private Table table;

    public ConditionsEditor(String locatorId, ConditionsAlertDefinitionForm form) {
        super(locatorId);
        this.form = form;
    }

    @Override
    protected void onInit() {
        super.onInit();

        table = new Table("conditionsTable");
        table.setShowHeader(false);
        table.setDataSource(new ConditionDataSource());

        table.addTableAction(this.extendLocatorId("add"), "Add", SelectionEnablement.ALWAYS, null, new TableAction() {
            @Override
            public void executeAction(ListGridRecord[] selection) {
                // TODO Auto-generated method stub
            }
        });
        table.addTableAction(this.extendLocatorId("delete"), "Delete", SelectionEnablement.ANY, "Are you sure?",
            new TableAction() {
                @Override
                public void executeAction(ListGridRecord[] selection) {
                    // TODO Auto-generated method stub
                }
            });

        addMember(table);
    }

    public void setEditable(boolean editable) {
        table.setTableActionDisableOverride(!editable);
    }

    private class ConditionDataSource extends RPCDataSource<AlertCondition> {
        private static final String FIELD_OBJECT = "obj";
        private static final String FIELD_CONDITION = "condition";

        public ConditionDataSource() {
            DataSourceTextField conditionField = new DataSourceTextField(FIELD_CONDITION, "Condition");
            addField(conditionField);
        }

        @Override
        public AlertCondition copyValues(ListGridRecord from) {
            return (AlertCondition) from.getAttributeAsObject(FIELD_OBJECT);
        }

        @Override
        public ListGridRecord copyValues(AlertCondition from) {
            ListGridRecord record = new ListGridRecord();
            record.setAttribute(FIELD_CONDITION, AlertFormatUtility.formatAlertConditionForDisplay(from));
            record.setAttribute(FIELD_OBJECT, from);
            return record;
        }

        @Override
        protected void executeFetch(DSRequest request, DSResponse response) {
            response.setData(buildRecords(ConditionsEditor.this.form.getAlertDefinition().getConditions()));
            processResponse(request.getRequestId(), response);
        }
    }
}
