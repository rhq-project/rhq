/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_ALERTS;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_RESOURCE_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cloud.StorageNode.OperationMode;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;
import org.rhq.enterprise.gui.coregui.client.components.table.AuthorizedTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;
import org.rhq.enterprise.gui.coregui.client.util.async.Command;
import org.rhq.enterprise.gui.coregui.client.util.async.CountDownLatch;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * Shows the table of all storage nodes.
 *
 * @author Jirka Kremser
 */
public class StorageNodeTableView extends TableSection<StorageNodeDatasource> {

    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_TOPOLOGY_VIEW_ID + "/" + StorageNodeAdminView.VIEW_ID;

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
        setDataSource(StorageNodeDatasource.instance());
    }

    @Override
    protected void doOnDraw() {
        super.doOnDraw();
        scheduleUnacknowledgedAlertsPollingJob(getListGrid());
    }

    @Override
    protected void configureTable() {
        super.configureTable();
        List<ListGridField> fields = getDataSource().getListGridFields();
        ListGrid listGrid = getListGrid();
        listGrid.setAutoSaveEdits(false);
        listGrid.setFields(fields.toArray(new ListGridField[fields.size()]));
        listGrid.sort(FIELD_ADDRESS.propertyName(), SortDirection.ASCENDING);
        listGrid.setHoverWidth(200);
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
                            rawUrl = LinkManager.getResourceLink(record.getAttributeAsInt(FIELD_RESOURCE_ID
                                .propertyName()));
                        } catch (NumberFormatException nfe) {
                            rawUrl = MSG.common_label_none();
                        }

                        String formattedValue = StringUtility.escapeHtml(rawUrl);
                        String label = StringUtility.escapeHtml("Link to Resource");
                        return LinkManager.getHref(formattedValue, label);
                    }
                });
            }
        }
    }

    private void scheduleUnacknowledgedAlertsPollingJob(final ListGrid listGrid) {
        new Timer() {
            public void run() {
                Log.info("Running the job fetching the number of unack alerts for particular storage nodes...");
                final ListGridRecord[] records = listGrid.getRecords();
                List<Integer> storageNodeIds = new ArrayList<Integer>(records.length);
                for (ListGridRecord record : records) {
                    // todo: get the resource ids and create a method on SLSB that accepts resource ids to make it faster
                    storageNodeIds.add(record.getAttributeAsInt(FIELD_ID));
                }
                GWTServiceLookup.getStorageService().findNotAcknowledgedStorageNodeAlertsCounts(storageNodeIds,
                    new AsyncCallback<List<Integer>>() {
                        @Override
                        public void onSuccess(List<Integer> result) {
                            for (int i = 0; i < records.length; i++) {
                                int value = result.get(i);
                                records[i].setAttribute(FIELD_ALERTS.propertyName(),
                                    StorageNodeAdminView.getAlertsString("New Alerts", value));
                                listGrid.setData(records);
                            }
                            schedule(15 * 1000);
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            schedule(60 * 1000);
                            // todo:
                            SC.say("fooo");
                        }
                    });
            }
        }.schedule(15 * 1000);
        Log.info("Polling job fetching the number of unack alerts for particular storage nodes has been scheduled");
    }

    @Override
    protected ListGrid createListGrid() {
        ListGrid listGrid = new ListGrid() {
            @Override
            protected Canvas getExpansionComponent(final ListGridRecord record) {
                int id = record.getAttributeAsInt(FIELD_ID);
                return new StorageNodeLoadComponent(id, null);
            }
        };
        listGrid.setCanExpandRecords(true);
        //        listGrid.setAutoFetchData(true);

        return listGrid;
    }

    @Override
    public Canvas getDetailsView(Integer id) {
        HTMLFlow header = new HTMLFlow("");
        setHeader(header);
        return new StorageNodeDetailView(id, header);
    }

    private void showCommonActions() {
        addInvokeOperationsAction();
    }

    private void addInvokeOperationsAction() {
        Map<String, Object> operationsMap = new LinkedHashMap<String, Object>();
        operationsMap.put("Start", "start");
        operationsMap.put("Shutdown", "shutdown");
        operationsMap.put("Restart", "restart");
        operationsMap.put("Disable Debug Mode", "stopRPCServer");
        operationsMap.put("Enable Debug Mode", "startRPCServer");
        //        operationsMap.put("Decommission", "decommission");

        addTableAction(MSG.common_title_operation(), null, operationsMap, new AuthorizedTableAction(this,
            TableActionEnablement.ANY, Permission.MANAGE_SETTINGS) {

            @Override
            public boolean isEnabled(ListGridRecord[] selection) {
                return StorageNodeTableView.this.isEnabled(super.isEnabled(selection), selection);
            };

            @Override
            public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                final String operationName = (String) actionValue;
                final List<String> selectedAddresses = getSelectedAddresses(selections);
                //                String message = MSG.view_adminTopology_message_setModeConfirm(selectedAddresses.toString(), mode.name());
                SC.ask("Are you sure, you want to run operation " + operationName + "?", new BooleanCallback() {
                    public void execute(Boolean confirmed) {
                        if (confirmed) {
                            final CountDownLatch latch = CountDownLatch.create(selections.length, new Command() {
                                @Override
                                public void execute() {
                                    //                                    Message msg = new Message(MSG.view_adminTopology_message_setMode(
                                    //                                      String.valueOf(selections.length), mode.name()), Message.Severity.Info);
                                    Message msg = new Message("Operation" + operationName
                                        + " was successfully scheduled for resources with ids"
                                        + Arrays.asList(getSelectedIds(selections)), Message.Severity.Info);
                                    CoreGUI.getMessageCenter().notify(msg);
                                    refreshTableInfo();
                                }
                            });
                            boolean isStopStartOrRestart = Arrays.asList("start", "shutdown", "restart").contains(
                                operationName);
                            for (ListGridRecord storageNodeRecord : selections) {
                                // NFE should never happen, because of the condition for table action enablement
                                int resourceId = storageNodeRecord.getAttributeAsInt(FIELD_RESOURCE_ID.propertyName());
                                if (isStopStartOrRestart) {
                                    // start, stop or restart the storage node
                                    GWTServiceLookup.getOperationService().scheduleResourceOperation(resourceId,
                                        operationName, null, "Run by Storage Node Administrations UI", 0,
                                        new AsyncCallback<Void>() {
                                            public void onSuccess(Void result) {
                                                latch.countDown();
                                            }

                                            public void onFailure(Throwable caught) {
                                                CoreGUI.getErrorHandler().handleError(
                                                    "Scheduling operation " + operationName
                                                        + " failed for resources with ids"
                                                        + Arrays.asList(getSelectedIds(selections)) + " "
                                                        + caught.getMessage(), caught);
                                                latch.countDown();
                                                refreshTableInfo();
                                            }
                                        });
                                } else {
                                    // invoke the operation on the storage service resource
                                    GWTServiceLookup.getStorageService().invokeOperationOnStorageService(resourceId,
                                        operationName, new AsyncCallback<Void>() {
                                            public void onSuccess(Void result) {
                                                latch.countDown();
                                            }

                                            public void onFailure(Throwable caught) {
                                                CoreGUI.getErrorHandler().handleError(
                                                    "Scheduling operation " + operationName
                                                        + " failed for resources with ids"
                                                        + Arrays.asList(getSelectedIds(selections)) + " "
                                                        + caught.getMessage(), caught);
                                                latch.countDown();
                                                refreshTableInfo();
                                            }
                                        });
                                }
                            }
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

    private List<String> getSelectedAddresses(ListGridRecord[] selections) {
        if (selections == null) {
            return new ArrayList<String>(0);
        }
        List<String> ids = new ArrayList<String>(selections.length);
        for (ListGridRecord selection : selections) {
            ids.add(selection.getAttributeAsString(FIELD_ADDRESS.propertyName()));
        }
        return ids;
    }

    private boolean isEnabled(boolean parentsOpinion, ListGridRecord[] selection) {
        if (!parentsOpinion) {
            return false;
        }
        for (ListGridRecord storageNodeRecord : selection) {
            if (storageNodeRecord.getAttribute(FIELD_RESOURCE_ID.propertyName()) == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected String getBasePath() {
        return VIEW_PATH;
    }
}
