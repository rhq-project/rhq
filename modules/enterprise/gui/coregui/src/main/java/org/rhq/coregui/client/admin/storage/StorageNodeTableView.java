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
package org.rhq.coregui.client.admin.storage;

import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_ADDRESS;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_ALERTS;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_AVAILABILITY;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_RESOURCE_ID;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_STATUS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNode.OperationMode;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.admin.AdministrationView;
import org.rhq.coregui.client.components.table.AuthorizedTableAction;
import org.rhq.coregui.client.components.table.TableActionEnablement;
import org.rhq.coregui.client.components.table.TableSection;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.StringUtility;
import org.rhq.coregui.client.util.async.Command;
import org.rhq.coregui.client.util.async.CountDownLatch;
import org.rhq.coregui.client.util.message.Message;

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
        //        scheduleUnacknowledgedAlertsPollingJob(getListGrid());
    }

    @Override
    protected void configureTable() {
        super.configureTable();
        List<ListGridField> fields = getDataSource().getListGridFields();

        // this needs to be added here instead of the DS because of the Canvas.imgHTML method
        for (ListGridField field : fields) {
            if (FIELD_AVAILABILITY.propertyName().equals(field.getName())) {
                field.setCellFormatter(new CellFormatter() {
                    public String format(Object value, ListGridRecord listGridRecord, int i, int i1) {
                        return imgHTML(ImageManager
                            .getAvailabilityIconFromAvailType(value == null ? AvailabilityType.UNKNOWN
                                : (AvailabilityType) value));
                    }
                });
            }
        }

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
                        String label = StringUtility.escapeHtml(MSG.view_adminTopology_storageNodes_link());
                        return LinkManager.getHref(formattedValue, label);
                    }
                });
            }
        }
    }

    @SuppressWarnings("unused")
    private void scheduleUnacknowledgedAlertsPollingJob(final ListGrid listGrid) {
        new Timer() {
            public void run() {
                Log.info("Running the job fetching the number of unack alerts for particular storage nodes...");
                final ListGridRecord[] records = listGrid.getRecords();
                List<Integer> storageNodeIds = new ArrayList<Integer>(records.length);
                for (ListGridRecord record : records) {
                    storageNodeIds.add(record.getAttributeAsInt(FIELD_ID));
                }
                GWTServiceLookup.getStorageService().findNotAcknowledgedStorageNodeAlertsCounts(storageNodeIds,
                    new AsyncCallback<List<Integer>>() {
                        @Override
                        public void onSuccess(List<Integer> result) {
                            for (int i = 0; i < records.length; i++) {
                                int value = result.get(i);
                                int storageNodeId = records[i].getAttributeAsInt("id");
                                records[i].setAttribute(
                                    FIELD_ALERTS.propertyName(),
                                    StorageNodeAdminView.getAlertsString(
                                        MSG.view_adminTopology_storageNodes_unackAlerts(), storageNodeId, value));
                                listGrid.setData(records);
                            }
                            schedule(15 * 1000);
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            schedule(60 * 1000);
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
                if (record.getAttribute(FIELD_RESOURCE_ID.propertyName()) == null) {
                    // no resource set
                    return new HTMLFlow(MSG.view_adminTopology_storageNodes_noLoad());
                }
                int id = record.getAttributeAsInt(FIELD_ID);
                return new StorageNodeLoadComponent(id, null);
            }
        };
        listGrid.setCanExpandRecords(true);
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
        addDeployAction();
        addUndeployAction();
    }

    private void addUndeployAction() {
        final ParametrizedMessage question = new ParametrizedMessage() {
            @Override
            public String getMessage(String... param) {
                return MSG.view_adminTopology_storageNodes_msg_undeployConfirm(param[0]);
            }
        };
        final ParametrizedMessage success = new ParametrizedMessage() {
            @Override
            public String getMessage(String... param) {
                return MSG.view_adminTopology_storageNodes_msg_deployStart(param[0]);
            }
        };
        final ParametrizedMessage failure = new ParametrizedMessage() {
            @Override
            public String getMessage(String... param) {
                return MSG.view_adminTopology_storageNodes_msg_undeployFailed(param[0], param[1]);
            }
        };

        addTableAction(MSG.view_adminTopology_storageNodes_run_undeploySelected(), null, new AuthorizedTableAction(
            this, TableActionEnablement.SINGLE, Permission.MANAGE_SETTINGS) {

            @Override
            public boolean isEnabled(ListGridRecord[] selection) {
                return StorageNodeTableView.this.isUndeployable(super.isEnabled(selection), selection);
            }

            @Override
            public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                executeBulkAction(selections, actionValue, question, success, failure, StorageNodeOperation.UNDEPLOY);
            }
        });
    }

    private void addDeployAction() {
        final ParametrizedMessage question = new ParametrizedMessage() {
            @Override
            public String getMessage(String... param) {
                return MSG.view_adminTopology_storageNodes_msg_deployConfirm(param[0]);
            }
        };
        final ParametrizedMessage success = new ParametrizedMessage() {
            @Override
            public String getMessage(String... param) {
                return MSG.view_adminTopology_storageNodes_msg_undeployStart(param[0]);
            }
        };
        final ParametrizedMessage failure = new ParametrizedMessage() {
            @Override
            public String getMessage(String... param) {
                return MSG.view_adminTopology_storageNodes_msg_deployFailed(param[0], param[1]);
            }
        };

        addTableAction(MSG.view_adminTopology_storageNodes_run_deploySelected(), null, new AuthorizedTableAction(this,
            TableActionEnablement.SINGLE, Permission.MANAGE_SETTINGS) {

            @Override
            public boolean isEnabled(ListGridRecord[] selection) {
                return StorageNodeTableView.this.isDeployable(super.isEnabled(selection), selection);
            }

            @Override
            public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                executeBulkAction(selections, actionValue, question, success, failure, StorageNodeOperation.DEPLOY);
            }
        });
    }

    private void addInvokeOperationsAction() {
        Map<String, Object> operationsMap = new LinkedHashMap<String, Object>();
        operationsMap.put(MSG.common_title_start(), "start");
        operationsMap.put(MSG.view_adminTopology_storageNodes_run_shutdown(), "shutdown");
        operationsMap.put(MSG.view_adminTopology_storageNodes_run_restart(), "restart");
        operationsMap.put(MSG.view_adminTopology_storageNodes_run_disableDebug(), "stopRPCServer");
        operationsMap.put(MSG.view_adminTopology_storageNodes_run_enableDebug(), "startRPCServer");

        addTableAction(MSG.common_title_operation(), null, operationsMap, new AuthorizedTableAction(this,
            TableActionEnablement.ANY, Permission.MANAGE_SETTINGS) {

            @Override
            public boolean isEnabled(ListGridRecord[] selection) {
                return StorageNodeTableView.this.isEnabled(super.isEnabled(selection), selection);
            }

            @Override
            public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                ParametrizedMessage question = new ParametrizedMessage() {
                    @Override
                    public String getMessage(String... param) {
                        return MSG.view_adminTopology_storageNodes_msg_commonOpConfirm(param[0], param[1]);
                    }
                };
                ParametrizedMessage success = new ParametrizedMessage() {
                    @Override
                    public String getMessage(String... param) {
                        return MSG.view_adminTopology_storageNodes_msg_commonOpFailed(param[0], param[1]);
                    }
                };
                ParametrizedMessage failure = new ParametrizedMessage() {
                    @Override
                    public String getMessage(String... param) {
                        return MSG.view_adminTopology_storageNodes_msg_commonOpStart(param[0], param[1]);
                    }
                };
                executeBulkAction(selections, actionValue, question, success, failure, StorageNodeOperation.OTHER);
            }
        });
    }

    private enum StorageNodeOperation {
        DEPLOY, UNDEPLOY, OTHER
    }

    private interface ParametrizedMessage {
        String getMessage(String... param);
    }

    private void executeBulkAction(final ListGridRecord[] selections, Object actionValue, ParametrizedMessage question,
        final ParametrizedMessage success, final ParametrizedMessage failure, final StorageNodeOperation operationType) {
        final String operationName = (String) actionValue;
        final List<String> selectedAddresses = getSelectedAddresses(selections);
        String areYouSureQuestion = operationType == StorageNodeOperation.OTHER ? question.getMessage(operationName,
            selectedAddresses.toString()) : question.getMessage(selectedAddresses.toString());
        SC.ask(areYouSureQuestion, new BooleanCallback() {
            public void execute(Boolean confirmed) {
                if (confirmed) {
                    final CountDownLatch latch = CountDownLatch.create(selections.length, new Command() {
                        @Override
                        public void execute() {
                            String msgString = null;
                            if (operationType == StorageNodeOperation.OTHER) {
                                msgString = success.getMessage(operationName, selectedAddresses.toString());
                            } else {
                                msgString = success.getMessage(selectedAddresses.toString());
                            }
                            Message msg = new Message(msgString, Message.Severity.Info);
                            CoreGUI.getMessageCenter().notify(msg);
                            refreshTableInfo();
                        }
                    });
                    boolean isStopStartOrRestart = Arrays.asList("start", "shutdown", "restart")
                        .contains(operationName);
                    for (ListGridRecord storageNodeRecord : selections) {
                        // NFE should never happen, because of the condition for table action enablement
                        int resourceId = storageNodeRecord.getAttributeAsInt(FIELD_RESOURCE_ID.propertyName());
                        if (isStopStartOrRestart) {
                            // start, stop or restart the storage node
                            GWTServiceLookup.getOperationService().scheduleResourceOperation(resourceId, operationName,
                                null, "Run by Storage Node Administrations UI", 0, new AsyncCallback<Void>() {
                                    public void onSuccess(Void result) {
                                        latch.countDown();
                                    }

                                    public void onFailure(Throwable caught) {
                                        String msg = failure.getMessage(operationName,
                                            selectedAddresses + " " + caught.getMessage());
                                        CoreGUI.getErrorHandler().handleError(msg, caught);
                                        latch.countDown();
                                        refreshTableInfo();
                                    }
                                });
                        } else {
                            if (operationType != StorageNodeOperation.OTHER) { // (un)deploy
                                AsyncCallback<Void> callback = new AsyncCallback<Void>() {
                                    public void onSuccess(Void result) {
                                        latch.countDown();
                                    }

                                    public void onFailure(Throwable caught) {
                                        String msg = failure.getMessage(
                                            selectedAddresses.toString(),
                                            Arrays.asList(getSelectedIds(selections)).toString() + " "
                                                + caught.getMessage());
                                        CoreGUI.getErrorHandler().handleError(msg, caught);
                                        latch.countDown();
                                        refreshTableInfo();
                                    }
                                };
                                int storageNodeId = storageNodeRecord.getAttributeAsInt("id");
                                StorageNode node = new StorageNode(storageNodeId);
                                if (operationType == StorageNodeOperation.DEPLOY) {
                                    GWTServiceLookup.getStorageService().deployStorageNode(node, callback);
                                } else {
                                    GWTServiceLookup.getStorageService().undeployStorageNode(node, callback);
                                }
                            } else {
                                // invoke the operation on the storage service resource
                                GWTServiceLookup.getStorageService().invokeOperationOnStorageService(resourceId,
                                    operationName, new AsyncCallback<Void>() {
                                        public void onSuccess(Void result) {
                                            latch.countDown();
                                        }

                                        public void onFailure(Throwable caught) {
                                            String msg = failure.getMessage(operationName, selectedAddresses + " "
                                                + caught.getMessage());
                                            CoreGUI.getErrorHandler().handleError(msg, caught);
                                            latch.countDown();
                                            refreshTableInfo();
                                        }
                                    });
                            }
                        }
                    }
                } else {
                    refreshTableInfo();
                }
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

    private boolean isDeployable(boolean parentsOpinion, ListGridRecord[] selection) {
        if (!parentsOpinion || !isEnabled(parentsOpinion, selection)) {
            return false;
        }
        for (ListGridRecord storageNodeRecord : selection) {
            if ("NORMAL".equals(storageNodeRecord.getAttributeAsString(FIELD_STATUS.propertyName()))
                || "JOINING".equals(storageNodeRecord.getAttributeAsString(FIELD_STATUS.propertyName()))
                || "LEAVING".equals(storageNodeRecord.getAttributeAsString(FIELD_STATUS.propertyName()))) {
                return false;
            }
        }
        List<ListGridRecord> selectionList = Arrays.asList(selection);
        ListGridRecord[] allRecords = getListGrid().getRecords();
        for (ListGridRecord storageNodeRecord : allRecords) {
            if (!selectionList.contains(storageNodeRecord)) {
                if (StorageNode.Status.JOINING.toString().equals(
                    storageNodeRecord.getAttributeAsString(FIELD_STATUS.propertyName()))
                    || StorageNode.Status.LEAVING.toString().equals(
                        storageNodeRecord.getAttributeAsString(FIELD_STATUS.propertyName()))) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isUndeployable(boolean parentsOpinion, ListGridRecord[] selection) {
        if (!parentsOpinion || !isEnabled(parentsOpinion, selection)) {
            return false;
        }
        for (ListGridRecord storageNodeRecord : selection) {
            if ("JOINING".equals(storageNodeRecord.getAttributeAsString(FIELD_STATUS.propertyName()))
                || "LEAVING".equals(storageNodeRecord.getAttributeAsString(FIELD_STATUS.propertyName()))) {
                return false;
            }
        }
        List<ListGridRecord> selectionList = Arrays.asList(selection);
        ListGridRecord[] allRecords = getListGrid().getRecords();
        int nodesInNormalCouner = 0;
        for (ListGridRecord storageNodeRecord : allRecords) {
            if (!selectionList.contains(storageNodeRecord)) {
                if (StorageNode.Status.JOINING.toString().equals(
                    storageNodeRecord.getAttributeAsString(FIELD_STATUS.propertyName()))
                    || StorageNode.Status.LEAVING.toString().equals(
                        storageNodeRecord.getAttributeAsString(FIELD_STATUS.propertyName()))) {
                    return false;
                }
            }
            if (StorageNode.Status.NORMAL.toString().equals(
                storageNodeRecord.getAttributeAsString(FIELD_STATUS.propertyName()))
                && AvailabilityType.UP
                    .equals(storageNodeRecord.getAttributeAsObject(FIELD_AVAILABILITY.propertyName()))) {
                nodesInNormalCouner++;
            }
        }
        return nodesInNormalCouner > 1;
    }

    @Override
    protected String getBasePath() {
        return VIEW_PATH;
    }
}
