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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.definitions;

import java.util.Set;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoadedListener;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoader;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.TableUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class GroupDefinitionListView extends TableSection {
    private static final String TITLE = MSG.view_dynagroup_definitions();

    public GroupDefinitionListView(String locatorId, String headerIcon) {
        super(locatorId, TITLE);

        setHeaderIcon(headerIcon);

        setDataSource(GroupDefinitionDataSource.getInstance());
    }

    @Override
    protected void configureTable() {
        ListGridField idField = new ListGridField("id", MSG.common_title_id(), 50);
        ListGridField nameField = new ListGridField("name", MSG.common_title_name(), 150);
        ListGridField descriptionField = new ListGridField("description", MSG.common_title_description());
        ListGridField expressionField = new ListGridField("expression", MSG.view_dynagroup_expressionSet(), 250);
        expressionField.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                return value.toString().replaceAll("\\n", "<br/>");
            }
        });

        ListGridField lastCalculationTimeField = new ListGridField("lastCalculationTime", MSG
            .view_dynagroup_lastCalculationTime(), 175);
        //lastCalculationTimeField.setAlign(Alignment.CENTER);
        lastCalculationTimeField.setCellFormatter(new TimestampCellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                if (value == null) {
                    return MSG.common_val_never();
                }
                return super.format(value, record, rowNum, colNum);
            }
        });

        ListGridField nextCalculationTimeField = new ListGridField("nextCalculationTime", MSG
            .view_dynagroup_nextCalculationTime(), 175);
        //nextCalculationTimeField.setAlign(Alignment.CENTER);
        nextCalculationTimeField.setCellFormatter(new TimestampCellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                if (value == null || "0".equals(value.toString())) {
                    return MSG.common_val_na();
                }
                return super.format(value, record, rowNum, colNum);
            }
        });

        getListGrid().setFields(idField, nameField, descriptionField, expressionField, lastCalculationTimeField,
            nextCalculationTimeField);

        addTableAction(extendLocatorId("Delete"), MSG.common_button_delete(), null, new AbstractTableAction(
            TableActionEnablement.ANY) {
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                final int[] groupDefinitionIds = TableUtility.getIds(selection);
                ResourceGroupGWTServiceAsync groupManager = GWTServiceLookup.getResourceGroupService();
                groupManager.deleteGroupDefinitions(groupDefinitionIds, new AsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_dynagroup_deleteSuccessfulSelection(String
                                .valueOf(groupDefinitionIds.length)), Severity.Info));
                        GroupDefinitionListView.this.refresh();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_dynagroup_deleteFailureSelection(), caught);
                    }
                });
            }
        });

        addTableAction(extendLocatorId("New"), MSG.common_button_new(), null, new AbstractTableAction() {
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                newDetails();
            }
        });

        addTableAction(extendLocatorId("Recalculate"), MSG.view_dynagroup_recalculate(), null, new AbstractTableAction(
            TableActionEnablement.ANY) {
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                final int[] groupDefinitionIds = TableUtility.getIds(selection);
                ResourceGroupGWTServiceAsync resourceGroupManager = GWTServiceLookup.getResourceGroupService();

                resourceGroupManager.recalculateGroupDefinitions(groupDefinitionIds, new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_dynagroup_recalcFailureSelection(), caught);
                    }

                    public void onSuccess(Void result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_dynagroup_recalcSuccessfulSelection(String
                                .valueOf(groupDefinitionIds.length)), Severity.Info));
                        GroupDefinitionListView.this.refresh();
                    }
                });
            }
        });
    }

    @Override
    public Canvas getDetailsView(int id) {
        final SingleGroupDefinitionView singleGroupDefinitionView = new SingleGroupDefinitionView(this
            .extendLocatorId("Details"));
        return singleGroupDefinitionView;
    }

    @Override
    public void renderView(final ViewPath viewPath) {

        new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {
            @Override
            public void onPermissionsLoaded(Set<Permission> permissions) {
                if (permissions != null && permissions.contains(Permission.MANAGE_INVENTORY)) {
                    GroupDefinitionListView.super.renderView(viewPath);
                } else {
                    handleAuthorizationFailure();
                }
            }

            private void handleAuthorizationFailure() {
                CoreGUI.getErrorHandler().handleError(MSG.view_dynagroup_permDenied());
                History.back();
            }
        });
    }
}