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

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.Table.SelectionEnablement;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * Superclass to the different alert definition views. This should be subclassed
 * to obtain resource, group, and template alert definition views. 
 * 
 * @author John Mazzitelli
 */
public abstract class AbstractAlertDefinitionsView extends LocatableVLayout {

    private SingleAlertDefinitionView singleAlertDefinitionView;
    private Table alertDefinitionsTable;

    public AbstractAlertDefinitionsView(String locatorId) {
        super(locatorId);
        setWidth100();
        setHeight100();
        setMembersMargin(10);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        Criteria criteria = getCriteria();
        alertDefinitionsTable = new Table(extendLocatorId("AlertDef"), getTableTitle(), criteria);
        alertDefinitionsTable.setDataSource(getAlertDefinitionDataSource());
        alertDefinitionsTable.getListGrid().setUseAllDataSourceFields(true);

        alertDefinitionsTable.getListGrid().addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                AlertDefinition alertDef = null;
                ListGridRecord selectedRecord = null;
                ListGridRecord[] allSelections = selectionEvent.getSelection();
                if (allSelections != null && allSelections.length == 1) {
                    selectedRecord = allSelections[0];
                }
                if (selectedRecord != null) {
                    alertDef = ((AbstractAlertDefinitionsDataSource) alertDefinitionsTable.getDataSource())
                        .copyValues(selectedRecord);
                    showSingleAlertDefinitionView(alertDef);
                } else {
                    hideSingleAlertDefinitionView();
                }
                markForRedraw();
            }
        });

        boolean permitted = isAllowedToModifyAlerts();

        alertDefinitionsTable.addTableAction(extendLocatorId("New"), "New", (permitted) ? SelectionEnablement.ALWAYS
            : SelectionEnablement.NEVER, null, new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                newButtonPressed(selection);
                CoreGUI.refresh();
            }
        });

        alertDefinitionsTable.addTableAction(extendLocatorId("Enable"), "Enable", (permitted) ? SelectionEnablement.ANY
            : SelectionEnablement.NEVER, "Are You Sure?", new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                enableButtonPressed(selection);
                CoreGUI.refresh();
            }
        });

        alertDefinitionsTable.addTableAction(extendLocatorId("Disable"), "Disable",
            (permitted) ? SelectionEnablement.ANY : SelectionEnablement.NEVER, "Are You Sure?", new TableAction() {
                public void executeAction(ListGridRecord[] selection) {
                    disableButtonPressed(selection);
                    CoreGUI.refresh();
                }
            });

        alertDefinitionsTable.addTableAction(extendLocatorId("Delete"), "Delete", (permitted) ? SelectionEnablement.ANY
            : SelectionEnablement.NEVER, "Are You Sure?", new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                deleteButtonPressed(selection);
                CoreGUI.refresh();
            }
        });

        addMember(alertDefinitionsTable);

        singleAlertDefinitionView = buildSingleAlertDefinitionView();
        singleAlertDefinitionView.hide();
        singleAlertDefinitionView.setWidth100();
        singleAlertDefinitionView.setHeight100();
        singleAlertDefinitionView.setMargin(10);
        addMember(singleAlertDefinitionView);
    }

    protected SingleAlertDefinitionView getSingleAlertDefinitionView() {
        return singleAlertDefinitionView;
    }

    protected void showSingleAlertDefinitionView(AlertDefinition alertDef) {
        alertDefinitionsTable.setHeight("33%");
        alertDefinitionsTable.setShowResizeBar(true);
        singleAlertDefinitionView.setHeight("67%");
        singleAlertDefinitionView.show();
        singleAlertDefinitionView.setAlertDefinition(alertDef);
    }

    protected void hideSingleAlertDefinitionView() {
        alertDefinitionsTable.setHeight100();
        alertDefinitionsTable.setShowResizeBar(false);
        singleAlertDefinitionView.hide();
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
