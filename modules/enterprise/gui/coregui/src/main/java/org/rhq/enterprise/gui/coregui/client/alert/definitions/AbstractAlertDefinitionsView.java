/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.alert.definitions;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.Table.SelectionEnablement;

/**
 * Superclass to the different alert definition views. This should be subclassed
 * to obtain resource, group, and template alert definition views. 
 * 
 * @author John Mazzitelli
 */
public abstract class AbstractAlertDefinitionsView extends VLayout {

    private SingleAlertDefinitionView singleAlertDefinitionView;

    public AbstractAlertDefinitionsView() {
        setWidth100();
        setHeight100();
        setMembersMargin(10);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        Criteria criteria = getCriteria();
        final Table table = new Table(getTableTitle(), criteria);
        table.setDataSource(getAlertDefinitionDataSource());
        table.getListGrid().setUseAllDataSourceFields(true);

        table.getListGrid().addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                AlertDefinition alertDef = null;
                ListGridRecord selectedRecord = null;
                ListGridRecord[] allSelections = selectionEvent.getSelection();
                if (allSelections != null && allSelections.length == 1) {
                    selectedRecord = allSelections[0];
                }
                if (selectedRecord != null) {
                    alertDef = ((AbstractAlertDefinitionsDataSource) table.getDataSource()).copyValues(selectedRecord);
                    table.setHeight("33%");
                    table.setShowResizeBar(true);
                    singleAlertDefinitionView.setHeight("67%");
                    singleAlertDefinitionView.show();
                    singleAlertDefinitionView.setAlertDefinition(alertDef);
                } else {
                    table.setHeight100();
                    table.setShowResizeBar(false);
                    singleAlertDefinitionView.hide();
                }
                markForRedraw();
            }
        });

        boolean permitted = isAllowedToModifyAlerts();

        table.addTableAction("New", (permitted) ? SelectionEnablement.ALWAYS : SelectionEnablement.NEVER, null,
            new TableAction() {
                public void executeAction(ListGridRecord[] selection) {
                    newButtonPressed(selection);
                    CoreGUI.refresh();
                }
            });

        table.addTableAction("Enable", (permitted) ? SelectionEnablement.ANY : SelectionEnablement.NEVER,
            "Are You Sure?", new TableAction() {
                public void executeAction(ListGridRecord[] selection) {
                    enableButtonPressed(selection);
                    CoreGUI.refresh();
                }
            });

        table.addTableAction("Disable", (permitted) ? SelectionEnablement.ANY : SelectionEnablement.NEVER,
            "Are You Sure?", new TableAction() {
                public void executeAction(ListGridRecord[] selection) {
                    disableButtonPressed(selection);
                }
            });

        table.addTableAction("Delete", (permitted) ? SelectionEnablement.ANY : SelectionEnablement.NEVER,
            "Are You Sure?", new TableAction() {
                public void executeAction(ListGridRecord[] selection) {
                    deleteButtonPressed(selection);
                    CoreGUI.refresh();
                }
            });

        addMember(table);

        this.singleAlertDefinitionView = buildSingleAlertDefinitionView();
        this.singleAlertDefinitionView.hide();
        this.singleAlertDefinitionView.setWidth100();
        this.singleAlertDefinitionView.setHeight100();
        this.singleAlertDefinitionView.setMargin(10);
        addMember(this.singleAlertDefinitionView);
    }

    protected SingleAlertDefinitionView getSingleAlertDefinitionView() {
        return this.singleAlertDefinitionView;
    }

    protected SingleAlertDefinitionView buildSingleAlertDefinitionView() {
        SingleAlertDefinitionView singleAlertDefinitionView = new SingleAlertDefinitionView();
        return singleAlertDefinitionView;
    }

    protected abstract String getTableTitle();

    protected abstract Criteria getCriteria();

    protected abstract AbstractAlertDefinitionsDataSource getAlertDefinitionDataSource();

    protected abstract boolean isAllowedToModifyAlerts();

    protected abstract void newButtonPressed(ListGridRecord[] selection);

    protected abstract void deleteButtonPressed(ListGridRecord[] selection);

    protected abstract void enableButtonPressed(ListGridRecord[] selection);

    protected abstract void disableButtonPressed(ListGridRecord[] selection);
}
