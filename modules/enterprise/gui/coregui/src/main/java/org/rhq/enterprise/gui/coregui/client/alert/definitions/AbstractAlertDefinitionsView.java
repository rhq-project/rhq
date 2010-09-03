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

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

/**
 * Superclass to the different alert definition views. This should be subclassed
 * to obtain resource, group, and template alert definition views. 
 * 
 * @author John Mazzitelli
 */
public abstract class AbstractAlertDefinitionsView extends TableSection {

    public AbstractAlertDefinitionsView(String locatorId, String tableTitle) {
        super(locatorId, tableTitle);
    }

    @Override
    protected void configureTable() {

        ListGrid listGrid = getListGrid();

        AbstractAlertDefinitionsDataSource ds = getAlertDefinitionDataSource();
        setDataSource(ds);
        listGrid.setDataSource(ds);

        Criteria criteria = getCriteria();
        listGrid.setCriteria(criteria);
        listGrid.setUseAllDataSourceFields(true);
        listGrid.setWrapCells(true);
        listGrid.setFixedRecordHeights(false);
        //listGrid.getField("id").setWidth(55);

        boolean permitted = isAllowedToModifyAlertDefinitions();

        addTableAction(extendLocatorId("New"), "New", (permitted) ? SelectionEnablement.ALWAYS
            : SelectionEnablement.NEVER, null, new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                newButtonPressed(selection);
                // I don't think you want this refresh, it will recreate the new alert detail 
                //CoreGUI.refresh();
            }
        });

        addTableAction(extendLocatorId("Enable"), "Enable", (permitted) ? SelectionEnablement.ANY
            : SelectionEnablement.NEVER, "Are You Sure?", new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                enableButtonPressed(selection);
                CoreGUI.refresh();
            }
        });

        addTableAction(extendLocatorId("Disable"), "Disable", (permitted) ? SelectionEnablement.ANY
            : SelectionEnablement.NEVER, "Are You Sure?", new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                disableButtonPressed(selection);
                CoreGUI.refresh();
            }
        });

        addTableAction(extendLocatorId("Delete"), "Delete", (permitted) ? SelectionEnablement.ANY
            : SelectionEnablement.NEVER, "Are You Sure?", new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                deleteButtonPressed(selection);
                CoreGUI.refresh();
            }
        });
    }

    @Override
    public void showDetails(ListGridRecord record) {
        Canvas canvas = getDetailsView(record);
        setDetailsView(canvas);

        Integer id = record.getAttributeAsInt("id");
        History.newItem(getBasePath() + "/" + id.intValue(), false);

        switchToDetailsView();
    }

    @Override
    public Canvas getDetailsView(ListGridRecord record) {
        if (record == null) {
            return getDetailsView(0);
        }

        AlertDefinition alertDef = getAlertDefinitionDataSource().copyValues(record);
        SingleAlertDefinitionView singleAlertDefinitionView = new SingleAlertDefinitionView(this
            .extendLocatorId(alertDef.getName()), alertDef);
        return singleAlertDefinitionView;
    }

    @Override
    public SingleAlertDefinitionView getDetailsView(int id) {
        final SingleAlertDefinitionView singleAlertDefinitionView = new SingleAlertDefinitionView(this
            .extendLocatorId("Empty"));

        if (id == 0) {
            // create an empty one with all defaults
            AlertDefinition newAlertDef = new AlertDefinition();
            newAlertDef.setDeleted(false);
            newAlertDef.setEnabled(true);
            newAlertDef.setNotifyFiltered(false);
            newAlertDef.setParentId(Integer.valueOf(0));
            newAlertDef.setConditionExpression(BooleanExpression.ALL);
            newAlertDef.setPriority(AlertPriority.MEDIUM);
            newAlertDef.setWillRecover(false);
            singleAlertDefinitionView.makeEditable();
        } else {
            final AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
            criteria.addFilterId(id);
            criteria.fetchGroupAlertDefinition(true);
            GWTServiceLookup.getAlertService().findAlertDefinitionsByCriteria(criteria,
                new AsyncCallback<PageList<AlertDefinition>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to load alert definition data", caught);
                    }

                    public void onSuccess(PageList<AlertDefinition> result) {
                        if (result.size() > 0) {
                            singleAlertDefinitionView.setAlertDefinition(result.get(0));
                        }
                    }
                });
        }

        return singleAlertDefinitionView;
    }

    protected abstract Criteria getCriteria();

    protected abstract AbstractAlertDefinitionsDataSource getAlertDefinitionDataSource();

    protected abstract boolean isAllowedToModifyAlertDefinitions();

    protected abstract void newButtonPressed(ListGridRecord[] selection);

    protected abstract void deleteButtonPressed(ListGridRecord[] selection);

    protected abstract void enableButtonPressed(ListGridRecord[] selection);

    protected abstract void disableButtonPressed(ListGridRecord[] selection);
}
