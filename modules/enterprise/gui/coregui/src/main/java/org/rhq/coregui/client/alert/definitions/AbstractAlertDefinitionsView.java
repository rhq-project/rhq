/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.coregui.client.alert.definitions;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.table.AbstractTableAction;
import org.rhq.coregui.client.components.table.EscapedHtmlCellFormatter;
import org.rhq.coregui.client.components.table.TableActionEnablement;
import org.rhq.coregui.client.components.table.TableSection;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;

/**
 * Superclass to the different alert definition views. This should be subclassed
 * to obtain resource, group, and template alert definition views. 
 * 
 * @author John Mazzitelli
 */
public abstract class AbstractAlertDefinitionsView extends TableSection<AbstractAlertDefinitionsDataSource> {

    public AbstractAlertDefinitionsView(String tableTitle, Criteria initialCriteria) {
        super(tableTitle, initialCriteria);

        setEscapeHtmlInDetailsLinkColumn(true);
    }

    @Override
    protected void configureTable() {
        ListGrid listGrid = getListGrid();

        AbstractAlertDefinitionsDataSource ds = getAlertDefinitionDataSource();
        setDataSource(ds);
        listGrid.setDataSource(ds);
        listGrid.setFields(ds.getListGridFields().toArray(new ListGridField[0]));

        listGrid.setUseAllDataSourceFields(true);
        listGrid.setWrapCells(true);
        listGrid.setFixedRecordHeights(false);
        //listGrid.getField("id").setWidth(55);

        // name and description are user-editable, so escape HTML to prevent XSS attacks
        ListGridField nameField = listGrid.getField(AbstractAlertDefinitionsDataSource.FIELD_NAME);
        nameField.setCellFormatter(new EscapedHtmlCellFormatter());
        ListGridField descriptionField = listGrid.getField(AbstractAlertDefinitionsDataSource.FIELD_DESCRIPTION);
        descriptionField.setCellFormatter(new EscapedHtmlCellFormatter());

        final boolean isAuthorized = isAuthorizedToModifyAlertDefinitions();

        addTableAction(MSG.common_button_new(), null, ButtonColor.BLUE, new AbstractTableAction() {
            public boolean isEnabled(ListGridRecord[] selection) {
                return super.isEnabled(selection) && isAuthorized;
            }

            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                newButtonPressed(selection);
            }
        });

        addTableAction(MSG.common_button_enable(), MSG.view_alert_definitions_enable_confirm(),
            new AbstractTableAction(TableActionEnablement.ANY) {
                public boolean isEnabled(ListGridRecord[] selection) {
                    return super.isEnabled(selection) && isAuthorized;
                }

                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    enableButtonPressed(selection);
                    refresh();
                }
            });
        addTableAction(MSG.common_button_disable(), MSG.view_alert_definitions_disable_confirm(),
            new AbstractTableAction(TableActionEnablement.ANY) {
                public boolean isEnabled(ListGridRecord[] selection) {
                    return super.isEnabled(selection) && isAuthorized;
                }

                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    disableButtonPressed(selection);
                    refresh();
                }
            });
        addTableAction(MSG.common_button_delete(), MSG.view_alert_definitions_delete_confirm(), ButtonColor.RED,
            new AbstractTableAction(TableActionEnablement.ANY) {
                public boolean isEnabled(ListGridRecord[] selection) {
                    return super.isEnabled(selection) && isAuthorized;
                }

                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    deleteButtonPressed(selection);
                }
            });

        super.configureTable();
    }

    @Override
    public Canvas getDetailsView(ListGridRecord record) {
        if (record == null) {
            return getDetailsView(0);
        }

        AlertDefinition alertDef = getAlertDefinitionDataSource().copyValues(record);
        SingleAlertDefinitionView singleAlertDefinitionView = new SingleAlertDefinitionView(this, alertDef);
        return singleAlertDefinitionView;
    }

    @Override
    public SingleAlertDefinitionView getDetailsView(final Integer id) {
        final SingleAlertDefinitionView singleAlertDefinitionView = new SingleAlertDefinitionView(this);

        if (id == 0) {
            // create an empty one with all defaults
            AlertDefinition newAlertDef = new AlertDefinition();
            newAlertDef.setDeleted(false);
            newAlertDef.setEnabled(true);
            newAlertDef.setPriority(AlertPriority.MEDIUM);
            newAlertDef.setParentId(0);
            newAlertDef.setConditionExpression(BooleanExpression.ANY);
            newAlertDef.setWillRecover(false);
            newAlertDef.setRecoveryId(0);
            newAlertDef.setAlertDampening(new AlertDampening(AlertDampening.Category.NONE));
            newAlertDef.setNotifyFiltered(false);
            newAlertDef.setControlFiltered(false);
            singleAlertDefinitionView.setAlertDefinition(newAlertDef);
            singleAlertDefinitionView.makeEditable();
        } else {
            final AlertDefinitionCriteria criteria = getDetailCriteria();
            criteria.addFilterId(id);
            criteria.fetchGroupAlertDefinition(true);
            criteria.fetchConditions(true);
            criteria.fetchAlertNotifications(true);
            GWTServiceLookup.getAlertDefinitionService().findAlertDefinitionsByCriteria(criteria,
                new AsyncCallback<PageList<AlertDefinition>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(
                            MSG.view_alert_definitions_loadFailed_single(String.valueOf(id)), caught);
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

    /**
     * Override to add any criteria that must be present when fetching the alert definition detail.
     * @return
     */
    protected AlertDefinitionCriteria getDetailCriteria() {
        return new AlertDefinitionCriteria();
    }
    
    
    
    protected abstract void commitAlertDefinition(final AlertDefinition alertDefinition, boolean resetMatching, final AsyncCallback<AlertDefinition> resultReceiver);

    protected abstract ResourceType getResourceType();

    protected abstract AbstractAlertDefinitionsDataSource getAlertDefinitionDataSource();

    protected abstract boolean isAuthorizedToModifyAlertDefinitions();

    protected abstract void newButtonPressed(ListGridRecord[] selection);

    protected abstract void deleteButtonPressed(ListGridRecord[] selection);

    protected abstract void enableButtonPressed(ListGridRecord[] selection);

    protected abstract void disableButtonPressed(ListGridRecord[] selection);
}
