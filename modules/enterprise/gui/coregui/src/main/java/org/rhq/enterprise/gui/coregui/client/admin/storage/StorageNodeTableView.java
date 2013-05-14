/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.admin.storage;

import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_ADDRESS;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_RESOURCE_ID;

import java.util.ArrayList;
import java.util.List;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cloud.StorageNode.OperationMode;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.IconEnum;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;
import org.rhq.enterprise.gui.coregui.client.components.table.AuthorizedTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.view.HasViewName;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * Shows the table of all storage nodes.
 *
 * @author Jirka Kremser
 */
public class StorageNodeTableView extends
    TableSection<StorageNodeDatasource> implements HasViewName {

    public static final ViewName VIEW_ID = new ViewName("StorageNodes", "td(i18n) Storage Nodes", IconEnum.STORAGE_NODE);

    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_TOPOLOGY_VIEW_ID + "/" + VIEW_ID;

    public StorageNodeTableView() {
        super(null);
        setHeight100();
        setWidth100();
        Criteria criteria = new Criteria();
        String[] modes = new String[OperationMode.values().length];
        int i = 0;
        for (OperationMode value : OperationMode.values()) {
            modes[i++] = value.name();
        }
        criteria.addCriteria(StorageNodeDatasource.FILTER_OPERATION_MODE, modes);
        setInitialCriteria(criteria);
        setDataSource(new StorageNodeDatasource());
    }

    @Override
    protected void configureTable() {
        super.configureTable();
        List<ListGridField> fields = getDataSource().getListGridFields();
        ListGrid listGrid = getListGrid();
        listGrid.setFields(fields.toArray(new ListGridField[fields.size()]));
        listGrid.sort(FIELD_ADDRESS.propertyName(), SortDirection.ASCENDING);
        showCommonActions();

        for (ListGridField field : fields) {
            // adding the cell formatter for name field (clickable link)
            if (field.getName() == FIELD_ADDRESS.propertyName()) {
                field.setCellFormatter(new CellFormatter() {
                    @Override
                    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                        if (value == null) {
                            return "";
                        }
                        String detailsUrl = "#" + VIEW_PATH + "/" + getId(record);
                        String formattedValue = StringUtility.escapeHtml(value.toString());
                        return LinkManager.getHref(detailsUrl, formattedValue);

                    }
                });
            } else if (field.getName() == FIELD_RESOURCE_ID.propertyName()) {
                // adding the cell formatter for resource id field (clickable link)
                field.setCellFormatter(new CellFormatter() {
                    @Override
                    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                        if (value == null || value.toString().isEmpty()) {
                            return "";
                        }
                        String rawUrl = null;
                        try {
                            rawUrl = LinkManager.getResourceLink(record.getAttributeAsInt(FIELD_RESOURCE_ID.propertyName()));
                        } catch (NumberFormatException nfe) {
                            Message msg = new Message("td(i18n) nfe", Message.Severity.Warning);
                            CoreGUI.getMessageCenter().notify(msg);
                        }
                        String formattedValue = StringUtility.escapeHtml(rawUrl);
                        return formattedValue;
                    }
                });
            }
        }
    }

    @Override
    public Canvas getDetailsView(Integer id) {
        return new StorageNodeDetailView(id);
    }

    private void showCommonActions() {
        addChangeOperationModeAction(OperationMode.NORMAL, MSG.view_adminTopology_server_setNormal());
        addChangeOperationModeAction(OperationMode.MAINTENANCE, MSG.view_adminTopology_server_setMaintenance());

        addTableAction(MSG.view_adminTopology_server_removeSelected(), null, new AuthorizedTableAction(this,
            TableActionEnablement.ANY, Permission.MANAGE_SETTINGS) {
            public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                List<String> selectedNames = getSelectedNames(selections);
                String message = MSG.view_adminTopology_message_removeServerConfirm(selectedNames.toString());
                SC.ask(message, new BooleanCallback() {
                    public void execute(Boolean confirmed) {
                        if (confirmed) {
                            SC.say("You've selected:\n\n" + getSelectedNames(selections));
//                            int[] selectedIds = getSelectedIds(selections);
//                            GWTServiceLookup.getTopologyService().deleteServers(selectedIds, new AsyncCallback<Void>() {
//                                public void onSuccess(Void arg0) {
//                                    Message msg = new Message(MSG.view_adminTopology_message_removedServer(String
//                                        .valueOf(selections.length)), Message.Severity.Info);
//                                    CoreGUI.getMessageCenter().notify(msg);
//                                    refresh();
//                                }
//
//                                public void onFailure(Throwable caught) {
//                                    CoreGUI.getErrorHandler().handleError(
//                                        MSG.view_adminTopology_message_removeServerFail(String
//                                            .valueOf(selections.length)) + " " + caught.getMessage(), caught);
//                                    refreshTableInfo();
//                                }
//
//                            });
                        }
                    }
                });
            }
        });
    }

    private void addChangeOperationModeAction(final OperationMode mode, String label) {
        addTableAction(label, null, new AuthorizedTableAction(this, TableActionEnablement.ANY,
            Permission.MANAGE_SETTINGS) {
            public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                List<String> selectedNames = getSelectedNames(selections);
                String message = MSG.view_adminTopology_message_setModeConfirm(selectedNames.toString(), mode.name());
                SC.ask(message, new BooleanCallback() {
                    public void execute(Boolean confirmed) {
                        if (confirmed) {
                            SC.say("You've selected:\n\n" + getSelectedNames(selections));
//                            int[] selectedIds = getSelectedIds(selections);
//                            GWTServiceLookup.getTopologyService().updateServerMode(selectedIds, mode,
//                                new AsyncCallback<Void>() {
//                                    public void onSuccess(Void result) {
//                                        Message msg = new Message(MSG.view_adminTopology_message_setMode(
//                                            String.valueOf(selections.length), mode.name()), Message.Severity.Info);
//                                        CoreGUI.getMessageCenter().notify(msg);
//                                        refresh();
//                                    }
//
//                                    public void onFailure(Throwable caught) {
//                                        CoreGUI.getErrorHandler().handleError(
//                                            MSG.view_adminTopology_message_setModeFail(
//                                                String.valueOf(selections.length), mode.name())
//                                                + " " + caught.getMessage(), caught);
//                                        refreshTableInfo();
//                                    }
//
//                                });
                        } else {
                            refreshTableInfo();
                        }
                    }
                });
            }
        });
    }

    private int[] getSelectedIds(ListGridRecord[] selections) {
        if (selections == null) {
            return new int[0];
        }
        int[] ids = new int[selections.length];
        int i = 0;
        for (ListGridRecord selection : selections) {
            ids[i++] = selection.getAttributeAsInt(FIELD_ID);
        }
        return ids;
    }

    private List<String> getSelectedNames(ListGridRecord[] selections) {
        if (selections == null) {
            return new ArrayList<String>(0);
        }
        List<String> ids = new ArrayList<String>(selections.length);
        for (ListGridRecord selection : selections) {
            ids.add(selection.getAttributeAsString(FIELD_NAME));
        }
        return ids;
    }

    @Override
    public ViewName getViewName() {
        return VIEW_ID;
    }

    @Override
    protected String getBasePath() {
        return VIEW_PATH;
    }
}
