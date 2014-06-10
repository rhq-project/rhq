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
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_CQL_PORT;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_CTIME;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_MTIME;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_OPERATION_MODE;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_STATUS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNode.OperationMode;
import org.rhq.core.domain.cloud.StorageNodeConfigurationComposite;
import org.rhq.core.domain.criteria.StorageNodeCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.collection.ArrayUtils;
import org.rhq.coregui.client.BookmarkableView;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.ViewPath;
import org.rhq.coregui.client.admin.storage.StorageNodeConfigurationEditor.SaveCallback;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.InventoryView;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.MeasurementUtility;
import org.rhq.coregui.client.util.StringUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedHLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;

/**
 * Shows details of a storage node.
 * 
 * @author Jirka Kremser
 */
public class StorageNodeDetailView extends EnhancedVLayout implements BookmarkableView {

    private final int storageNodeId;

    private static final int SECTION_COUNT = 3;
    private final SectionStack sectionStack;
    private EnhancedVLayout detailsLayout;
    private EnhancedHLayout detailsAndLoadLayout;
    private EnhancedVLayout loadLayout;
    private SectionStackSection configurationSection;
    private SectionStackSection detailsAndLoadSection;
    private StaticTextItem alertsItem;
    private HTMLFlow header;
    private boolean alerts = false;
    private boolean isAddressEditable = true;
    private StaticTextItem jmxPortItem;
    private String originalAddress;
    private FormItem nameItem;

    private volatile int initSectionCount = 0;
    private int unackAlerts = -1;

    public StorageNodeDetailView(int storageNodeId, HTMLFlow header) {
        super();
        this.storageNodeId = storageNodeId;
        this.header = header;
        setHeight100();
        setWidth100();
        setOverflow(Overflow.AUTO);

        sectionStack = new SectionStack();
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        sectionStack.setWidth100();
        sectionStack.setHeight100();
        sectionStack.setCanResizeSections(false);
    }

    @Override
    protected void onInit() {
        super.onInit();
        if (alerts) {
            return;
        }
        StorageNodeCriteria criteria = new StorageNodeCriteria();
        criteria.addFilterId(storageNodeId);
        criteria.fetchResource(true);
        GWTServiceLookup.getStorageService().findStorageNodesByCriteria(criteria,
            new AsyncCallback<PageList<StorageNode>>() {
                public void onSuccess(final PageList<StorageNode> storageNodes) {
                    if (storageNodes == null || storageNodes.isEmpty() || storageNodes.size() != 1) {
                        onFailure(new Exception("No storage nodes have been found."));
                    }
                    final StorageNode node = storageNodes.get(0);
                    header.setContents("<div style='text-align: center; font-weight: bold; font-size: medium;'> Storage Node ("
                        + node.getAddress() + ")</div>");
                        
                    prepareDetailsSection(node);
                    fetchStorageNodeConfigurationComposite(node);
                    fetchSparkLineDataForLoadComponent(node);
                    fetchUnackAlerts(storageNodeId, node.getResource() != null);
                }

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_adminTopology_message_fetchServerFail(String.valueOf(storageNodeId)) + " "
                            + caught.getMessage(), caught);
                    initSectionCount = SECTION_COUNT;
                }
            });
    }
    
    
    private void fetchStorageNodeConfigurationComposite(final StorageNode node) {
        if (node.getResource() == null) { // no associated resource yet
            LayoutSpacer spacer = new LayoutSpacer();
            spacer.setHeight(15);
            HTMLFlow info = new HTMLFlow("<h2>There is no configuration available for this node. Is the agent running on the "
                + node.getAddress() + "?</h2>");            
            SectionStackSection section = new SectionStackSection("Configuration");
            section.setItems(spacer, info);
            section.setExpanded(true);
            section.setCanCollapse(false);

            configurationSection = section;
            initSectionCount++;
        } else {
            GWTServiceLookup.getStorageService().retrieveConfiguration(node,
                new AsyncCallback<StorageNodeConfigurationComposite>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Message message = new Message(MSG.view_configurationHistoryDetails_error_loadFailure(),
                            Message.Severity.Warning);
                        initSectionCount = SECTION_COUNT;
                        CoreGUI.getMessageCenter().notify(message);
                    }

                    @Override
                    public void onSuccess(StorageNodeConfigurationComposite result) {
                        jmxPortItem.setValue(result.getJmxPort());
                        prepareResourceConfigEditor(result);
                    }
                });
        }
    }
    
    private void fetchSparkLineDataForLoadComponent(final StorageNode storageNode) {
        if (storageNode.getResource() == null) {
            HTMLFlow info = new HTMLFlow("<i>No load data available.</i>");
            info.setExtraSpace(5);
            loadLayout = new EnhancedVLayout();
            loadLayout.setWidth100();
            LayoutSpacer spacer = new LayoutSpacer();
            spacer.setHeight(10);
            HTMLFlow loadLabel = new HTMLFlow("Status");
            loadLabel.addStyleName("formTitle");
            loadLabel.setHoverWidth(300);
            loadLayout.setMembers(spacer, loadLabel, info);

            if (detailsAndLoadLayout == null) {
                detailsAndLoadLayout = new EnhancedHLayout();
            }
            initSectionCount++;
        } else {
            GWTServiceLookup.getStorageService().findStorageNodeLoadDataForLast(storageNode, 8,
                MeasurementUtility.UNIT_HOURS, 60,
                new AsyncCallback<Map<String, List<MeasurementDataNumericHighLowComposite>>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Message message = new Message("Unable to fetch storage node load data.",
                            Message.Severity.Warning);
                        initSectionCount = SECTION_COUNT;
                        CoreGUI.getMessageCenter().notify(message);
                    }

                    @Override
                    public void onSuccess(Map<String, List<MeasurementDataNumericHighLowComposite>> result) {
                        prepareLoadSection(sectionStack, storageNode, result);
                    }

                });
        }
    }
    
    private void fetchUnackAlerts(final int storageNodeId, final boolean isResourceIdSet) {
        GWTServiceLookup.getStorageService().findNotAcknowledgedStorageNodeAlertsCounts(Arrays.asList(storageNodeId),
            new AsyncCallback<List<Integer>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Message message = new Message(MSG.view_inventory_resource_loadFailed(String.valueOf(storageNodeId)),
                        Message.Severity.Warning);
                    CoreGUI.getMessageCenter().notify(message);
                    initSectionCount = SECTION_COUNT;
                }

                @Override
                public void onSuccess(List<Integer> result) {
                    if (result.isEmpty()) {
                        onFailure(new Exception("Resource with id [" + storageNodeId + "] does not exist."));
                    } else {
                        unackAlerts = result.get(0);
                        if (alertsItem != null) {
                            alertsItem.setValue(isResourceIdSet ? StorageNodeAdminView.getAlertsString("Unacknowledged Alerts", storageNodeId, unackAlerts) : "New Alerts (0)");
                        }
                    }
                }
            });
    }

    public boolean isInitialized() {
        return initSectionCount >= SECTION_COUNT;
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        if (alerts) {
            return;
        }

        // wait until we have all of the sections before we show them. We don't use InitializableView because,
        // it seems they are not supported (in the applicable renderView()) at this level.
        new Timer() {
            final long startTime = System.currentTimeMillis();

            public void run() {
                if (isInitialized()) {
                    if (null != detailsAndLoadLayout) {
                        LayoutSpacer spacer = new LayoutSpacer();
                        spacer.setWidth(30);
                        detailsAndLoadLayout.setMembers(detailsLayout, spacer, loadLayout);
                        detailsAndLoadLayout.setHeight(220);
                        detailsAndLoadSection = new SectionStackSection("Storage Node Information");
                        detailsAndLoadSection.setExpanded(true);
                        detailsAndLoadSection.setItems(detailsAndLoadLayout);
                        sectionStack.addSection(detailsAndLoadSection);
                    }
                    if (null != configurationSection) {
                        sectionStack.addSection(configurationSection);
                    }
                    addMember(sectionStack);
                    markForRedraw();

                } else {
                    // don't wait forever, give up after 20s and show what we have
                    long elapsedMillis = System.currentTimeMillis() - startTime;
                    if (elapsedMillis > 20000) {
                        initSectionCount = SECTION_COUNT;
                    }
                    schedule(100); // Reschedule the timer.
                }
            }
        }.run(); // fire the timer immediately
    }

    private void prepareDetailsSection(final StorageNode storageNode) {
        final DynamicForm form = new DynamicForm();
        form.setMargin(10);
        form.setWidth100();
        form.setWrapItemTitles(false);
        form.setNumCols(2);

        ButtonItem save = null;
        isAddressEditable = storageNode.getResource() == null
            && storageNode.getOperationMode() == OperationMode.INSTALLED;
        if (isAddressEditable) {
            nameItem = new TextItem(FIELD_ADDRESS.propertyName(), FIELD_ADDRESS.title());
            nameItem.setValue(storageNode.getAddress());
            originalAddress = storageNode.getAddress();
            save = new ButtonItem("save");
            save.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    if (!addressWasChanged()) {
                        SC.say("Info", MSG.view_adminTopology_storageNodes_settings_noChanges());
                    } else {
                        SC.ask(MSG.common_msg_areYouSure(), new BooleanCallback() {
                            @Override
                            public void execute(Boolean value) {
                                if (value) {
                                    updateAddress();
                                }
                            }
                        });
                    }
                }
            });
        } else {
            nameItem = new StaticTextItem(FIELD_ADDRESS.propertyName(), FIELD_ADDRESS.title());
            nameItem.setValue("<b>" + storageNode.getAddress() + "</b>");
        }

        final StaticTextItem cqlPortItem = new StaticTextItem(FIELD_CQL_PORT.propertyName(), FIELD_CQL_PORT.title());
        cqlPortItem.setValue(storageNode.getCqlPort());

        jmxPortItem = new StaticTextItem("jmxPort", "JMX Port");

        final StaticTextItem operationModeItem = new StaticTextItem(FIELD_OPERATION_MODE.propertyName(),
            FIELD_OPERATION_MODE.title());

        operationModeItem.setValue(storageNode.getOperationMode());
        final StaticTextItem clusterStatusItem = new StaticTextItem(FIELD_STATUS.propertyName(), FIELD_STATUS.title());
        clusterStatusItem.setValue(storageNode.getStatus());

        final StaticTextItem availabilityItem = new StaticTextItem(FIELD_AVAILABILITY.propertyName(),
            FIELD_AVAILABILITY.title());

        // make clickable link to associated resource
        StaticTextItem resourceItem = new StaticTextItem("associatedResource", "Associated Resource");
        String storageNodeItemText = "";
        String availabilityItemText = imgHTML(ImageManager.getAvailabilityIconFromAvailType(AvailabilityType.UNKNOWN));
        Resource storageNodeResource = storageNode.getResource();
        if (storageNodeResource != null && storageNodeResource.getName() != null) {
            String detailsUrl = LinkManager.getResourceLink(storageNodeResource.getId());
            String formattedValue = StringUtility.escapeHtml(storageNodeResource.getName());
            storageNodeItemText = LinkManager.getHref(detailsUrl, formattedValue);

            // set the availability
            ResourceAvailability availability = storageNodeResource.getCurrentAvailability();
            if (storageNodeResource.getCurrentAvailability() != null
                && storageNodeResource.getCurrentAvailability().getAvailabilityType() != null) {
                availabilityItemText = imgHTML(ImageManager.getAvailabilityIconFromAvailType(availability
                    .getAvailabilityType()));
            }
        } else {
            storageNodeItemText = MSG.common_label_none();
        }
        resourceItem.setValue(storageNodeItemText);
        availabilityItem.setValue(availabilityItemText);

        StaticTextItem installationDateItem = new StaticTextItem(FIELD_CTIME.propertyName(), FIELD_CTIME.title());
        installationDateItem.setValue(TimestampCellFormatter.format(Long.valueOf(storageNode.getCtime()),
            TimestampCellFormatter.DATE_TIME_FORMAT_LONG));

        StaticTextItem lastUpdateItem = new StaticTextItem(FIELD_MTIME.propertyName(), FIELD_MTIME.title());
        lastUpdateItem.setValue(TimestampCellFormatter.format(Long.valueOf(storageNode.getMtime()),
            TimestampCellFormatter.DATE_TIME_FORMAT_LONG));

        alertsItem = new StaticTextItem(FIELD_ALERTS.propertyName(), FIELD_ALERTS.title());
        alertsItem
            .setPrompt("The number in brackets represents the number of unacknowledged alerts for this storage node.");
        if (unackAlerts != -1) {
            alertsItem.setValue(StorageNodeAdminView.getAlertsString("Unacknowledged Alerts", storageNodeId, unackAlerts));
        }

        StaticTextItem messageItem = new StaticTextItem("message", "Note");
        StringBuffer message = new StringBuffer();
        boolean isOk = true;
        OperationMode operationMode = storageNode.getOperationMode();
        boolean joining = operationMode == OperationMode.ANNOUNCE || operationMode == OperationMode.BOOTSTRAP
            || operationMode == OperationMode.ADD_MAINTENANCE;
        boolean leaving = operationMode == OperationMode.DECOMMISSION || operationMode == OperationMode.UNANNOUNCE
            || operationMode == OperationMode.REMOVE_MAINTENANCE || operationMode == OperationMode.UNINSTALL;
        if (storageNode.getResource() == null) {
            message.append("Storage node has no associated resource.<br />");
            isOk = false;
        }
        if (storageNode.getErrorMessage() != null) {
            String noteTextPrefix = (joining ? "Deployment error: " : (leaving ? "Undeployment error: " : ""));
            message.append(noteTextPrefix);
            message.append(storageNode.getErrorMessage()).append("<br />");
            isOk = false;
        } else if (storageNode.getFailedOperation() != null) {
            message.append("Last operation has failed.<br />");
        }
        if (isOk) {
            message.append("Everything is ok");
        }
        messageItem.setValue(message);

        StaticTextItem lastOperation = null;
        boolean isOperationFailed = storageNode.getFailedOperation() != null
            && storageNode.getFailedOperation().getResource() != null;
        if (isOperationFailed) {
            ResourceOperationHistory operationHistory = storageNode.getFailedOperation();
            String value = LinkManager.getSubsystemResourceOperationHistoryLink(operationHistory.getResource().getId(),
                operationHistory.getId());
            lastOperation = new StaticTextItem("lastOp", "Operation");
            String operationTextPrefix = (joining ? "Failed deployment operation: "
                : (leaving ? "Failed undeployment operation: " : ""));
            lastOperation.setValue(operationTextPrefix + LinkManager.getHref(value, operationHistory.getOperationDefinition()
                .getDisplayName()));
        }

        List<FormItem> formItems = new ArrayList<FormItem>(6);
        formItems.addAll(Arrays.asList(nameItem, resourceItem, availabilityItem, cqlPortItem, jmxPortItem/*, jmxConnectionUrlItem*/));
        if (!CoreGUI.isDebugMode())
            formItems.add(operationModeItem); // debug mode fails if this item is added
        formItems.addAll(Arrays.asList(clusterStatusItem, installationDateItem, lastUpdateItem, alertsItem, messageItem));
        if (isOperationFailed)
            formItems.add(lastOperation);
        if (isAddressEditable)
            formItems.add(save);
        form.setItems(formItems.toArray(new FormItem[] {}));

        detailsLayout = new EnhancedVLayout();
        detailsLayout.setWidth(450);
        detailsLayout.addMember(form);
        if (detailsAndLoadLayout == null) {
            detailsAndLoadLayout = new EnhancedHLayout(0);
        }
        initSectionCount++;
    }

    private void prepareLoadSection(SectionStack stack, final StorageNode storageNode,
        final Map<String, List<MeasurementDataNumericHighLowComposite>> sparkLineData) {
        StorageNodeLoadComponent loadDataComponent = new StorageNodeLoadComponent(storageNode.getId(), sparkLineData);
        loadDataComponent.setExtraSpace(5);
        loadLayout = new EnhancedVLayout();
        loadLayout.setWidth100();
        LayoutSpacer spacer = new LayoutSpacer();
        spacer.setHeight(10);
        HTMLFlow loadLabel = new HTMLFlow("Status");
        loadLabel.addStyleName("formTitle");
        loadLabel.setTooltip("Contains selected metrics collected for last 8 hours.");
        loadLabel.setHoverWidth(300);
        loadLayout.setMembers(spacer, loadLabel, loadDataComponent);

        if (detailsAndLoadLayout == null) {
            detailsAndLoadLayout = new EnhancedHLayout();
        }
        initSectionCount++;
    }
    
    private void prepareResourceConfigEditor(final StorageNodeConfigurationComposite configuration) {
        LayoutSpacer spacer = new LayoutSpacer();
        spacer.setHeight(15);
        StorageNodeConfigurationEditor editorView = new StorageNodeConfigurationEditor(configuration,
            !isAddressEditable ? null : new SaveCallback() {
                public void onSave() {
                    updateAddress();
                }
                public boolean wasChanged() {
                    return addressWasChanged();
                }
            });
        SectionStackSection section = new SectionStackSection(
            MSG.view_adminTopology_storageNodes_detail_configuration());
        section.setItems(spacer, editorView);
        section.setExpanded(true);
        section.setCanCollapse(false);

        configurationSection = section;
        initSectionCount++;
    }
    
    private boolean addressWasChanged() {
        return !originalAddress.equals(nameItem.getValue()) && !((String) nameItem.getValue()).isEmpty();
    }
    
    private void updateAddress() {
        GWTServiceLookup.getStorageService().updateAddress(storageNodeId, (String) nameItem.getValue(),
            new AsyncCallback<Void>() {
                public void onSuccess(Void result) {
                    Message msg = new Message(MSG.view_adminTopology_storageNodes_settings_message_updateSuccess(),
                        Message.Severity.Info);
                    CoreGUI.getMessageCenter().notify(msg);
                    originalAddress = (String) nameItem.getValue();
                }

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_adminTopology_storageNodes_clusterSettings_message_updateFail(), caught);
                }
            });
    }

    private void showAlertsForSingleStorageNode() {
        GWTServiceLookup.getStorageService().findResourcesWithAlertDefinitions(new StorageNode(storageNodeId),
            new AsyncCallback<Integer[]>() {
                @Override
                public void onFailure(Throwable caught) {
                    alerts = false;
                    CoreGUI.getErrorHandler().handleError(
                        "Unable to fetch alerts for storage node with id " + storageNodeId + ". Caused by: "
                            + caught.getMessage(), caught);
                }

                @Override
                public void onSuccess(Integer[] result) {
                    if (result == null || result.length == 0) {
                        onFailure(new Exception(
                            "There were no resources under the storage node that could contain an alert."));
                    } else {
                        removeMember(sectionStack);
                        sectionStack.destroy();
                        int[] resIds = ArrayUtils.unwrapArray(result);
                        Canvas alertsView = new StorageNodeAlertHistoryView("storageNode_" + storageNodeId + "_Alerts",
                            resIds, header, storageNodeId);
                        addMember(alertsView);
                    }
                }
            });
    }

    @Override
    public void renderView(ViewPath viewPath) {
        if (viewPath.toString().endsWith("/Alerts")) {
            alerts = true;
            showAlertsForSingleStorageNode();
        } else {
            alerts = false;
        }
        Log.debug("StorageNodeDetailView: " + viewPath);
    }
}
