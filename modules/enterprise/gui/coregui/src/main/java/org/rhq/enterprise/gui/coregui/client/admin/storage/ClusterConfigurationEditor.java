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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.validator.IsIntegerValidator;
import com.smartgwt.client.widgets.form.validator.Validator;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.cloud.StorageClusterSettings;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedToolStrip;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * The component for editing the cluster wide configuration
 *
 * @author Jirka Kremser
 */
public class ClusterConfigurationEditor extends EnhancedVLayout implements RefreshableView {

    private EnhancedDynamicForm form;
    private EnhancedIButton saveButton;
    private boolean oddRow;
    private StorageClusterSettings settings;
    
    private static String FIELD_CQL_PORT = "cql_port";
    private static String FIELD_GOSSIP_PORT = "gossip_port";

    public ClusterConfigurationEditor() {
        super();
    }
    
    private void fetchClusterSettings() {
        GWTServiceLookup.getStorageService().retrieveClusterSettings(
            new AsyncCallback<StorageClusterSettings>() {
                @Override
                public void onFailure(Throwable caught) {
                    Message message = new Message(MSG.view_configurationHistoryDetails_error_loadFailure(),
                        Message.Severity.Warning);
                }

                @Override
                public void onSuccess(StorageClusterSettings settings) {
                    ClusterConfigurationEditor.this.settings = settings;
                    prepareForm();
                }
            });
    }

    private void save() {
        updateSettings();
        GWTServiceLookup.getStorageService().updateClusterSettings(settings, new AsyncCallback<Void>() {
            public void onSuccess(Void result) {
                Message msg = new Message("Storage node settings were successfully updated.", Message.Severity.Info);
                CoreGUI.getMessageCenter().notify(msg);
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Unable to update the storage node settings.", caught);
            }
        });
    }
    
    private List<FormItem> buildOneFormRowWithValidator(String name, String title, String value, String description,
        Validator validator) {
        return buildOneFormRow(name, title, value, description, false, validator);
    }
    
    private List<FormItem> buildOneFormRow(String name, String title, String value, String description,
        boolean unitsDropdown, Validator validator) {
        List<FormItem> fields = new ArrayList<FormItem>();
        StaticTextItem nameItem = new StaticTextItem();
        nameItem.setStartRow(true);
        nameItem.setValue("<b>" + title + "</b>");
        nameItem.setShowTitle(false);
        nameItem.setCellStyle(oddRow ? "OddRow" : "EvenRow");
        fields.add(nameItem);

        FormItem valueItem = null;
            valueItem = new TextItem();
            valueItem.setName(name);
            valueItem.setValue(value);
            valueItem.setWidth(220);
            if (validator != null) {
                valueItem.setValidators(validator);
            }
        valueItem.setValidateOnChange(true);
        valueItem.setAlign(Alignment.CENTER);
        valueItem.setShowTitle(false);
        valueItem.setRequired(true);
        valueItem.setCellStyle(oddRow ? "OddRow" : "EvenRow");
        fields.add(valueItem);

        StaticTextItem descriptionItem = new StaticTextItem();
        descriptionItem.setValue(description);
        descriptionItem.setShowTitle(false);
        descriptionItem.setEndRow(true);
        descriptionItem.setCellStyle(oddRow ? "OddRow" : "EvenRow");
        fields.add(descriptionItem);

        oddRow = !oddRow;
        return fields;
    }

    private List<FormItem> buildHeaderItems() {
        List<FormItem> fields = new ArrayList<FormItem>();
        fields.add(createHeaderTextItem(MSG.view_configEdit_property()));
        fields.add(createHeaderTextItem(MSG.common_title_value()));
        fields.add(createHeaderTextItem(MSG.common_title_description()));
        return fields;
    }

    private StaticTextItem createHeaderTextItem(String value) {
        StaticTextItem unsetHeader = new StaticTextItem();
        unsetHeader.setValue(value);
        unsetHeader.setShowTitle(false);
        unsetHeader.setCellStyle("configurationEditorHeaderCell");
        return unsetHeader;
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        refresh();
    }
    
    private void prepareForm() {
        form = new EnhancedDynamicForm();
        form.setHiliteRequiredFields(true);
        form.setNumCols(3);
        form.setCellPadding(5);
        form.setColWidths(190, 220, "*");
        form.setIsGroup(true);
        form.setGroupTitle("Cluster Wide Settings");
        form.setBorder("1px solid #AAA");
        oddRow = true;

        List<FormItem> items = buildHeaderItems();
//          IntegerRangeValidator positiveInteger = new IntegerRangeValidator();
//        positiveInteger.setMin(1);
//        positiveInteger.setMax(Integer.MAX_VALUE);
        IsIntegerValidator validator = new IsIntegerValidator();
        items.addAll(buildOneFormRowWithValidator(FIELD_CQL_PORT, "CQL Port", String.valueOf(settings.getCqlPort()),
            "The port on which the Storage Nodes listens for CQL client connections.", validator));
        
//        IntegerRangeValidator portValidator = new IntegerRangeValidator();
//        portValidator.setMin(1);
//        portValidator.setMax(65535); // (1 << 16) - 1
        validator = new IsIntegerValidator();
        items.addAll(buildOneFormRowWithValidator(FIELD_GOSSIP_PORT, "Gossip Port", String.valueOf(settings.getGossipPort()),
            "The port used for internode communication. This is a shared, cluster-wide setting.", validator));
        form.setFields(items.toArray(new FormItem[items.size()]));
        form.setWidth100();
        form.setOverflow(Overflow.VISIBLE);
        setWidth100();
        
        LayoutSpacer spacer = new LayoutSpacer();
        spacer.setWidth100();
        
        ToolStrip toolStrip = buildToolStrip();
        setMembers(form, spacer, toolStrip);
        form.validate();
        markForRedraw();
    }

    @Override
    public void refresh() {
        fetchClusterSettings();
    }
    
    private EnhancedToolStrip buildToolStrip() {
        saveButton = new EnhancedIButton(MSG.common_button_save());
        saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (form.validate()) {
                    SC.ask(
                        "Changing the cluster wide configuration will eventually affect all the storage nodes. Do you want to continue?",
                        new BooleanCallback() {
                            @Override
                            public void execute(Boolean value) {
                                if (value) {
                                    save();
                                }
                            }
                        });
                }
            }
        });
        EnhancedToolStrip toolStrip = new EnhancedToolStrip();
        toolStrip.setWidth100();
        toolStrip.setMembersMargin(5);
        toolStrip.setLayoutMargin(5);
        toolStrip.addMember(saveButton);

        return toolStrip;
    }
    
    private StorageClusterSettings updateSettings() {
        settings.setCqlPort(Integer.parseInt(form.getValueAsString(FIELD_CQL_PORT)));
        settings.setGossipPort(Integer.parseInt(form.getValueAsString(FIELD_GOSSIP_PORT)));
        return settings;
    }
}
