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
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_CQL_PORT;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_CTIME;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_JMX_PORT;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_MTIME;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_OPERATION_MODE;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNode.OperationMode;
import org.rhq.core.domain.criteria.StorageNodeCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedToolStrip;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * Shows details of a storage node.
 * 
 * @author Jirka Kremser
 */
public class StorageNodeDetailView extends EnhancedVLayout implements BookmarkableView {

    private final int storageNodeId;

    private static final int SECTION_COUNT = 1;
    private final SectionStack sectionStack;
    private SectionStackSection detailsSection = null;
    private SectionStackSection agentSection = null;

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

    @Override
    protected void onInit() {
        super.onInit();
        StorageNodeCriteria criteria = new StorageNodeCriteria();
        criteria.addFilterId(storageNodeId);
        criteria.fetchResource(true);
        GWTServiceLookup.getTopologyService().findStorageNodesByCriteria(criteria, new AsyncCallback<PageList<StorageNode>>() {
            public void onSuccess(final PageList<StorageNode> storageNodes) {
                if (storageNodes == null || storageNodes.isEmpty() || storageNodes.size() != 1) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_adminTopology_message_fetchServerFail(String.valueOf(storageNodeId)));
                    initSectionCount = SECTION_COUNT;
                    return;
                }
                prepareDetailsSection(sectionStack, storageNodes.get(0));
//                prepareAgentSection(sectionStack, storageNodes.get(0));
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(
                    MSG.view_adminTopology_message_fetchServerFail(String.valueOf(storageNodeId)) + " "
                        + caught.getMessage(), caught);
                initSectionCount = SECTION_COUNT;
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
                    if (null != agentSection) {
                        sectionStack.addSection(agentSection);
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

//    private void prepareAgentSection(SectionStack stack, Server server) {
//        SectionStackSection section = new SectionStackSection(MSG.view_adminTopology_serverDetail_connectedAgents());
//        section.setExpanded(true);
//        AgentTableView agentsTable = new AgentTableView(storageNodeId, false);
//        section.setItems(agentsTable);
//
//        agentSection = section;
//        initSectionCount++;
//        return;
//    }

    private void prepareDetailsSection(SectionStack stack, final StorageNode storageNode) {
        final DynamicForm form = new DynamicForm();
        form.setMargin(10);
        form.setWidth100();
        form.setWrapItemTitles(false);
        form.setNumCols(2);

        final StaticTextItem nameItem = new StaticTextItem(FIELD_ADDRESS.propertyName(), FIELD_ADDRESS.title());
        nameItem.setValue("<b>" + storageNode.getAddress() + "</b>");

//        final TextItem addressItem = new TextItem(FIELD_ADDRESS.propertyName(), FIELD_ADDRESS.title());
//        addressItem.setRequired(true);
//        addressItem.setValue(server.getAddress());

//        IntegerRangeValidator portValidator = new IntegerRangeValidator();
//        portValidator.setMin(0);
//        portValidator.setMax(65535);
        final TextItem jmxPortItem = new TextItem(FIELD_JMX_PORT.propertyName(), FIELD_JMX_PORT.title());
//        portItem.setRequired(true);
//        portItem.setValidators(portValidator);
        jmxPortItem.setValue(storageNode.getJmxPort());
        
        final StaticTextItem jmxConnectionUrlItem = new StaticTextItem("jmxConnectionUrl", MSG.view_adminTopology_storageNode_jmxUrl());
        jmxConnectionUrlItem.setValue(storageNode.getJMXConnectionURL());

        final TextItem cqlPortItem = new TextItem(FIELD_CQL_PORT.propertyName(), FIELD_CQL_PORT.title());
//        cqlPortItem.setRequired(true);
//        cqlPortItem.setValidators(portValidator);
        cqlPortItem.setValue(storageNode.getCqlPort());

        final SelectItem operationModeItem = new SelectItem(FIELD_OPERATION_MODE.propertyName(),
            MSG.view_adminTopology_serverDetail_operationMode());
        operationModeItem.setValueMap("NORMAL", "MAINTENANCE");
        operationModeItem.setValue(storageNode.getOperationMode());

        // make clickable link to associated resource
        StaticTextItem resourceItem = new StaticTextItem("associatedResource",
            "Associated Resource");
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

        StaticTextItem lastUpdatetem = new StaticTextItem(FIELD_MTIME.propertyName(), FIELD_MTIME.title());
        lastUpdatetem.setValue(TimestampCellFormatter.format(Long.valueOf(storageNode.getMtime()),
            TimestampCellFormatter.DATE_TIME_FORMAT_LONG));

        IButton saveButton = new IButton();
        saveButton.setOverflow(Overflow.VISIBLE);
        saveButton.setTitle(MSG.common_button_save());
        saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                if (form.validate()) {
                    storageNode.setOperationMode(OperationMode.valueOf(operationModeItem.getValueAsString()));
                    SC.say(storageNode.toString());
//                    GWTServiceLookup.getTopologyService().updateServer(storageNode, new AsyncCallback<Void>() {
//                        public void onSuccess(Void result) {
//                            Message msg = new Message(MSG.view_adminTopology_message_serverUpdated(server.getName()),
//                                Message.Severity.Info);
//                            CoreGUI.getMessageCenter().notify(msg);
//
//                        }
//
//                        public void onFailure(Throwable caught) {
//                            CoreGUI.getErrorHandler().handleError(
//                                MSG.view_adminTopology_message_serverUpdateFail(server.getName()) + " "
//                                    + caught.getMessage(), caught);
//                        }
//                    });
                }
            }
        });

        form.setItems(nameItem, jmxPortItem, cqlPortItem, jmxConnectionUrlItem, operationModeItem, resourceItem,
            installationDateItem, lastUpdatetem);

        EnhancedToolStrip footer = new EnhancedToolStrip();
        footer.setPadding(5);
        footer.setWidth100();
        footer.setMembersMargin(15);
        footer.addMember(saveButton);

        SectionStackSection section = new SectionStackSection(MSG.common_title_details());
        section.setExpanded(true);
        section.setItems(form, footer);

        detailsSection = section;
        initSectionCount++;
    }

    @Override
    public void renderView(ViewPath viewPath) {
        Log.debug("StorageNodeDetainView: " + viewPath);
    }

}
