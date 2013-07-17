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
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_CQL_PORT;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_CTIME;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_JMX_PORT;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_MTIME;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_OPERATION_MODE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNode.OperationMode;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.StorageNodeCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.InventoryView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ResourceConfigurationEditView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.history.ResourceOperationHistoryListView;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedToolStrip;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * Shows details of a storage node.
 * 
 * @author Jirka Kremser
 */
public class StorageNodeDetailView extends EnhancedVLayout implements BookmarkableView {

    private final int storageNodeId;

    private static final int SECTION_COUNT = 3;
    private final SectionStack sectionStack;
    private SectionStackSection detailsSection;
    private SectionStackSection loadSection;
    private SectionStackSection historySection;
    private int expandedSection = -1;

    private volatile int initSectionCount = 0;

    public StorageNodeDetailView(int storageNodeId) {
        super();
        this.storageNodeId = storageNodeId;
        setHeight100();
        setWidth100();
        setOverflow(Overflow.AUTO);

        sectionStack = new SectionStack();
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        sectionStack.setWidth100();
        sectionStack.setHeight100();
        sectionStack.setMargin(5);
        sectionStack.setOverflow(Overflow.VISIBLE);
    }
    
    public StorageNodeDetailView(int storageNodeId, int expandedSection) {
        this(storageNodeId);
        this.expandedSection = expandedSection;
    }

    @Override
    protected void onInit() {
        super.onInit();
        StorageNodeCriteria criteria = new StorageNodeCriteria();
        criteria.addFilterId(storageNodeId);
        criteria.fetchResource(true);
        GWTServiceLookup.getStorageService().findStorageNodesByCriteria(criteria,
            new AsyncCallback<PageList<StorageNode>>() {
                public void onSuccess(final PageList<StorageNode> storageNodes) {
                    if (storageNodes == null || storageNodes.isEmpty() || storageNodes.size() != 1) {
                        CoreGUI.getErrorHandler().handleError(
                            MSG.view_adminTopology_message_fetchServerFail(String.valueOf(storageNodeId)));
                        initSectionCount = SECTION_COUNT;
                    }
                    final StorageNode node = storageNodes.get(0);
                    Resource res = node.getResource();
                    if (res != null) {
                        fetchResourceComposite(node.getResource().getId());
                    } else {
                        // skip this if the resource id is not there
                        initSectionCount++;
                    }
                    prepareDetailsSection(sectionStack, node);
                    prepareLoadSection(sectionStack, node);
                }

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_adminTopology_message_fetchServerFail(String.valueOf(storageNodeId)) + " "
                            + caught.getMessage(), caught);
                    initSectionCount = SECTION_COUNT;
                }
            });
    }
    
    private void fetchResourceComposite(final int resourceId) {
        ResourceCriteria resourceCriteria = new ResourceCriteria();
        resourceCriteria.addFilterId(resourceId);
        GWTServiceLookup.getResourceService().findResourceCompositesByCriteria(resourceCriteria,
            new AsyncCallback<PageList<ResourceComposite>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Message message = new Message(MSG.view_inventory_resource_loadFailed(String.valueOf(resourceId)),
                        Message.Severity.Warning);
                    CoreGUI.goToView(InventoryView.VIEW_ID.getName(), message);
                    initSectionCount = SECTION_COUNT;
                }

                @Override
                public void onSuccess(PageList<ResourceComposite> result) {
                    if (result.isEmpty()) {
                        onFailure(new Exception("Resource with id [" + resourceId + "] does not exist."));
                        initSectionCount = SECTION_COUNT;
                    } else {
                        final ResourceComposite resourceComposite = result.get(0);
//                        prepareOperationHistory(resourceComposite);
                        prepareResourceConfigEditor(resourceComposite);
                        
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

        // wait until we have all of the sections before we show them. We don't use InitializableView because,
        // it seems they are not supported (in the applicable renderView()) at this level.
        new Timer() {
            final long startTime = System.currentTimeMillis();

            public void run() {
                if (isInitialized()) {
                    if (null != detailsSection) {
                        sectionStack.addSection(detailsSection);
                    }
                    if (null != loadSection) {
                        sectionStack.addSection(loadSection);
                    }
                    if (null != historySection) {
                        sectionStack.addSection(historySection);
                    }
//                    if (expandedSection != -1) {
//                        for (int i = 1; i < SECTION_COUNT; i++) {
//                            sectionStack.collapseSection(i);
//                        }
//                        sectionStack.expandSection(expandedSection);
//                    }
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

    private void prepareDetailsSection(SectionStack stack, final StorageNode storageNode) {
        final DynamicForm form = new DynamicForm();
        form.setMargin(10);
        form.setWidth100();
        form.setWrapItemTitles(false);
        form.setNumCols(2);

        final StaticTextItem nameItem = new StaticTextItem(FIELD_ADDRESS.propertyName(), FIELD_ADDRESS.title());
        nameItem.setValue("<b>" + storageNode.getAddress() + "</b>");

//        final TextItem jmxPortItem = new TextItem(FIELD_JMX_PORT.propertyName(), FIELD_JMX_PORT.title());
        final StaticTextItem jmxPortItem = new StaticTextItem(FIELD_JMX_PORT.propertyName(), FIELD_JMX_PORT.title());
        jmxPortItem.setValue(storageNode.getJmxPort());

        final StaticTextItem jmxConnectionUrlItem = new StaticTextItem("jmxConnectionUrl",
            MSG.view_adminTopology_storageNode_jmxConnectionUrl());
        jmxConnectionUrlItem.setValue(storageNode.getJMXConnectionURL());

        final StaticTextItem cqlPortItem = new StaticTextItem(FIELD_CQL_PORT.propertyName(), FIELD_CQL_PORT.title());
        cqlPortItem.setValue(storageNode.getCqlPort());

        final StaticTextItem operationModeItem = new StaticTextItem(FIELD_OPERATION_MODE.propertyName(), MSG.view_adminTopology_serverDetail_operationMode());
        operationModeItem.setValue(storageNode.getOperationMode());

        // make clickable link to associated resource
        StaticTextItem resourceItem = new StaticTextItem("associatedResource", "Associated Resource");
        String storageNodeItemText = "";
        Resource storageNodeResource = storageNode.getResource();
        if (storageNodeResource != null && storageNodeResource.getName() != null) {
            String detailsUrl = LinkManager.getResourceLink(storageNodeResource.getId());
            String formattedValue = StringUtility.escapeHtml(storageNodeResource.getName());
            storageNodeItemText = LinkManager.getHref(detailsUrl, formattedValue);
        } else {
            storageNodeItemText = MSG.common_label_none();
        }
        resourceItem.setValue(storageNodeItemText);

        StaticTextItem installationDateItem = new StaticTextItem(FIELD_CTIME.propertyName(), FIELD_CTIME.title());
        installationDateItem.setValue(TimestampCellFormatter.format(Long.valueOf(storageNode.getCtime()),
            TimestampCellFormatter.DATE_TIME_FORMAT_LONG));

        StaticTextItem lastUpdateItem = new StaticTextItem(FIELD_MTIME.propertyName(), FIELD_MTIME.title());
        lastUpdateItem.setValue(TimestampCellFormatter.format(Long.valueOf(storageNode.getMtime()),
            TimestampCellFormatter.DATE_TIME_FORMAT_LONG));

        IButton saveButton = new IButton();
        saveButton.setOverflow(Overflow.VISIBLE);
        saveButton.setTitle(MSG.common_button_save());
        saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                if (form.validate()) {
//                    storageNode.setOperationMode(OperationMode.valueOf(operationModeItem.getValueAsString()));
                    storageNode.setOperationMode(OperationMode.valueOf((String) operationModeItem.getValue()));
                    SC.say(storageNode.toString());
                    // TODO: logic
                }
            }
        });
        List<FormItem> formItems = new ArrayList<FormItem>(8);
        formItems.addAll(Arrays.asList(nameItem, jmxPortItem, cqlPortItem, jmxConnectionUrlItem));
        if (!CoreGUI.isDebugMode()) formItems.add(operationModeItem); // debug mode fails if this item is added
        formItems.addAll(Arrays.asList(resourceItem, installationDateItem, lastUpdateItem));
        form.setItems(formItems.toArray(new FormItem[]{}));

        EnhancedToolStrip footer = new EnhancedToolStrip();
        footer.setPadding(5);
        footer.setWidth100();
        footer.setMembersMargin(15);
        footer.addMember(saveButton);

        SectionStackSection section = new SectionStackSection(MSG.common_title_details());
        section.setExpanded(expandedSection != -1 ? expandedSection == 0 : true);
        
        section.setItems(form);
        detailsSection = section;
        initSectionCount++;
    }

    private void prepareLoadSection(SectionStack stack, final StorageNode storageNode) {
        StorageNodeLoadComponent loadDataComponent = new StorageNodeLoadComponent(storageNode.getId());
        SectionStackSection section = new SectionStackSection("Load");
        section.setItems(loadDataComponent);
        section.setExpanded(expandedSection != -1 ? expandedSection == 1 : true);

        loadSection = section;
        initSectionCount++;
    }
    
    private void prepareOperationHistory(ResourceComposite resourceComposite) {
        ResourceOperationHistoryListView historyView = new ResourceOperationHistoryListView(resourceComposite);
        SectionStackSection section = new SectionStackSection("Operations");
        section.setItems(historyView);
        section.setExpanded(false);

        historySection = section;
        initSectionCount++;
    }
    
    private void prepareResourceConfigEditor(ResourceComposite resourceComposite) {
        ResourceConfigurationEditView editorView = new ResourceConfigurationEditView(resourceComposite);
        SectionStackSection section = new SectionStackSection("Configuration");
        section.setItems(editorView);
        section.setExpanded(expandedSection != -1 && expandedSection == 2);

        historySection = section;
        initSectionCount++;
    }
    
    

    @Override
    public void renderView(ViewPath viewPath) {
        if (viewPath.toString().endsWith("/Config")) {
//            for (int i = 1; i < SECTION_COUNT; i++) {
//                sectionStack.collapseSection(i);
//            }
            expandedSection = 2;
//            sectionStack.expandSection(expandedSection);
//            detailsSection.setExpanded(false);
//            loadSection.setExpanded(false);
//            historySection.setExpanded(true);
        }
        Log.debug("StorageNodeDetailView: " + viewPath);
    }
}
