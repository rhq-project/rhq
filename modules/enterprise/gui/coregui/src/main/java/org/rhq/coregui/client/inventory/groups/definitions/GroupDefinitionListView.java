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
package org.rhq.coregui.client.inventory.groups.definitions;

import java.util.Date;
import java.util.Set;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.group.DuplicateExpressionTypeException;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.PermissionsLoadedListener;
import org.rhq.coregui.client.PermissionsLoader;
import org.rhq.coregui.client.ViewPath;
import org.rhq.coregui.client.components.table.AbstractTableAction;
import org.rhq.coregui.client.components.table.EscapedHtmlCellFormatter;
import org.rhq.coregui.client.components.table.IconField;
import org.rhq.coregui.client.components.table.TableActionEnablement;
import org.rhq.coregui.client.components.table.TableSection;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.coregui.client.util.TableUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class GroupDefinitionListView extends TableSection<GroupDefinitionDataSource> {

    public GroupDefinitionListView() {
        super(null);

        setDataSource(GroupDefinitionDataSource.getInstance());
        setEscapeHtmlInDetailsLinkColumn(true);
    }

    @Override
    protected void configureTable() {
        ListGridField idField = new ListGridField("id", MSG.common_title_id());
        idField.setType(ListGridFieldType.INTEGER);
        idField.setWidth(50);

        IconField originField = new IconField("cannedExpression");
        originField.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                String icon = "global/User_16.png";
                if (value!=null) {
                    icon = "global/Plugin_16.png";
                }
                return "<img class='tableImage' src=\"" + ImageManager.getFullImagePath(icon) + "\" />";
            }
        });
        originField.setShowHover(true);
        originField.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                String expr = record.getAttribute("cannedExpression");
                String displayName = MSG.view_dynagroup_originHoverUser();
                if (expr!=null) {
                    displayName = MSG.view_dynagroup_originHoverPlugin(expr.replaceAll(":.*", ""));
                }
                return displayName;
            }
        });

        ListGridField nameField = new ListGridField("name", MSG.common_title_name(), 150);
        nameField.setCellFormatter(new EscapedHtmlCellFormatter());
        ListGridField descriptionField = new ListGridField("description", MSG.common_title_description());
        descriptionField.setCellFormatter(new EscapedHtmlCellFormatter());
        ListGridField expressionField = new ListGridField("expression", MSG.view_dynagroup_expressionSet(), 250);
        expressionField.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                return value.toString().replaceAll("\\n", "<br/>");
            }
        });

        ListGridField lastCalculationTimeField = new ListGridField("lastCalculationTime",
            MSG.view_dynagroup_lastCalculationTime(), 175);
        //lastCalculationTimeField.setAlign(Alignment.CENTER);
        lastCalculationTimeField.setCellFormatter(new TimestampCellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                if (value == null) {
                    return MSG.common_val_never();
                }
                return super.format(value, record, rowNum, colNum);
            }
        });
        lastCalculationTimeField.setShowHover(true);
        lastCalculationTimeField.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                String attribValue = record.getAttribute("lastCalculationTime");
                if (attribValue != null) {
                    return TimestampCellFormatter.getHoverDateString(new Date(Long.valueOf(attribValue).longValue()));
                } else {
                    return null;
                }
            }
        });

        ListGridField nextCalculationTimeField = new ListGridField("nextCalculationTime",
            MSG.view_dynagroup_nextCalculationTime(), 175);
        nextCalculationTimeField.setCellFormatter(new TimestampCellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                if (value == null || "0".equals(value.toString())) {
                    return MSG.common_val_na();
                }
                return super.format(value, record, rowNum, colNum);
            }
        });
        nextCalculationTimeField.setShowHover(true);
        nextCalculationTimeField.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                String attribValue = record.getAttribute("nextCalculationTime");
                if (attribValue != null && !("0".equals(attribValue.toString()))) {
                    return TimestampCellFormatter.getHoverDateString(new Date(Long.valueOf(attribValue).longValue()));
                } else {
                    return null;
                }
            }
        });

        setListGridFields(idField, originField, nameField, descriptionField, expressionField, lastCalculationTimeField,
            nextCalculationTimeField);

        addTableAction(MSG.common_button_delete(), MSG.common_msg_areYouSure(), ButtonColor.RED, new AbstractTableAction(
            TableActionEnablement.ANY) {
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                final int[] groupDefinitionIds = TableUtility.getIds(selection);
                ResourceGroupGWTServiceAsync groupManager = GWTServiceLookup.getResourceGroupService(60000);
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

        addTableAction(MSG.common_button_new(), null, ButtonColor.BLUE, new AbstractTableAction() {
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                newDetails();
            }
        });

        addTableAction(MSG.view_dynagroup_recalculate(), null, ButtonColor.GRAY, new AbstractTableAction(TableActionEnablement.ANY) {
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                final int[] groupDefinitionIds = TableUtility.getIds(selection);
                ResourceGroupGWTServiceAsync resourceGroupManager = GWTServiceLookup.getResourceGroupService();

                resourceGroupManager.recalculateGroupDefinitions(groupDefinitionIds, new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        if (caught instanceof DuplicateExpressionTypeException) {
                            CoreGUI.getMessageCenter().notify(
                                new Message(caught.getMessage(), Message.Severity.Warning));
                        } else {
                            CoreGUI.getErrorHandler().handleError(MSG.view_dynagroup_recalcFailureSelection(), caught);
                        }
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

        super.configureTable();
    }

    @Override
    public Canvas getDetailsView(Integer id) {
        final SingleGroupDefinitionView singleGroupDefinitionView = new SingleGroupDefinitionView();
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
