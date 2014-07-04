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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import org.rhq.core.domain.cloud.StorageNodeConfigurationComposite;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.RefreshableView;
import org.rhq.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.coregui.client.components.form.ValueWithUnitsItem;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedToolStrip;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;

/**
 * The component for editing the storage node configuration
 *
 * @author Jirka Kremser
 */
public class StorageNodeConfigurationEditor extends EnhancedVLayout implements RefreshableView {

    private EnhancedDynamicForm form;
    private EnhancedIButton saveButton;
    private boolean oddRow;
    private final StorageNodeConfigurationComposite configuration;

    private static String FIELD_HEAP_MAX = "heap_max";
    private static String FIELD_HEAP_NEW = "heap_new";
    private static String FIELD_THREAD_STACK_SIZE = "thread_stack_size";
    private static String FIELD_JMX_PORT = "jmx_port";

    public StorageNodeConfigurationEditor(final StorageNodeConfigurationComposite configuration) {
        super();
        this.configuration = configuration;
    }

    private void save(final StorageNodeConfigurationComposite configuration) {
        GWTServiceLookup.getStorageService().updateConfiguration(configuration, new AsyncCallback<Void>() {
            public void onSuccess(Void result) {
                Message msg = new Message(MSG.view_adminTopology_storageNodes_settings_message_updateSuccess(),
                    Message.Severity.Info);
                CoreGUI.getMessageCenter().notify(msg);
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_adminTopology_storageNodes_clusterSettings_message_updateFail(), caught);
            }
        });
    }

    private List<FormItem> buildOneFormRowWithCombobox(String name, String title, String value, String description) {
        return buildOneFormRow(name, title, value, description, true, null);
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
        if (unitsDropdown) {
            valueItem = buildJVMMemoryItem(name, value);
        } else {
            valueItem = new TextItem();
            valueItem.setName(name);
            valueItem.setValue(value);
            valueItem.setWidth(220);
            if (validator != null) {
                valueItem.setValidators(validator);
            }
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

    private FormItem buildJVMMemoryItem(String name, String value) {
        Set<MeasurementUnits> supportedUnits = new LinkedHashSet<MeasurementUnits>();
        supportedUnits.add(MeasurementUnits.MEGABYTES);
        supportedUnits.add(MeasurementUnits.GIGABYTES);

        ValueWithUnitsItem valueItem = new ValueWithUnitsItem(name, null, supportedUnits);
        if (value != null && !value.isEmpty()) {
            boolean megs = value.trim().substring(value.trim().length() - 1).equalsIgnoreCase("m");
            MeasurementUnits units = megs ? MeasurementUnits.MEGABYTES : MeasurementUnits.GIGABYTES;
            try {
                int intVal = Integer.parseInt(value.substring(0, value.toLowerCase().indexOf(megs ? "m" : "g")));
                valueItem.setValue(intVal, units);
            } catch (StringIndexOutOfBoundsException e) {
                // do nothing
            }
        }
        return valueItem;
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

    @Override
    public void refresh() {
        form = new EnhancedDynamicForm();
        form.setHiliteRequiredFields(true);
        form.setNumCols(3);
        form.setCellPadding(5);
        form.setColWidths(190, 220, "*");
        form.setIsGroup(true);
        form.setGroupTitle(MSG.view_adminTopology_storageNodes_settings_specific());
        oddRow = true;

        List<FormItem> items = buildHeaderItems();
        items
            .addAll(buildOneFormRowWithCombobox(
                FIELD_HEAP_MAX,
                MSG.view_adminTopology_storageNodes_settings_heapSizeName(),
                configuration.getHeapSize(),
                MSG.view_adminTopology_storageNodes_settings_heapSizeDescription()));
        items
            .addAll(buildOneFormRowWithCombobox(
                FIELD_HEAP_NEW,
                MSG.view_adminTopology_storageNodes_settings_heapNewSizeName(),
                configuration.getHeapNewSize(),
                MSG.view_adminTopology_storageNodes_settings_heapNewSizeDescription()));

        IsIntegerValidator validator = new IsIntegerValidator();
        items.addAll(buildOneFormRowWithValidator(FIELD_THREAD_STACK_SIZE,
            MSG.view_adminTopology_storageNodes_settings_threadStackSizeName(), configuration.getThreadStackSize(),
            MSG.view_adminTopology_storageNodes_settings_threadStackSizeDescription(), validator));
        validator = new IsIntegerValidator();
        items.addAll(buildOneFormRowWithValidator(FIELD_JMX_PORT,
            MSG.view_adminTopology_storageNodes_settings_jmxPortName(), String.valueOf(configuration.getJmxPort()),
            MSG.view_adminTopology_storageNodes_settings_jmxPortDescription(), validator));
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

    private EnhancedToolStrip buildToolStrip() {
        saveButton = new EnhancedIButton(MSG.common_button_save());
        saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (form.validate()) {
                    if (!checkNewHeapLowerThanMaxHeap()) {
                        Map<String, String> errors = new HashMap<String, String>(2);
                        errors.put(FIELD_HEAP_MAX, MSG.view_adminTopology_storageNodes_settings_validator1());
                        errors.put(FIELD_HEAP_NEW, MSG.view_adminTopology_storageNodes_settings_validator2());
                        form.setErrors(errors, true);
                        return;
                    }
                    final StorageNodeConfigurationComposite configuration = getConfiguration();
                    if (StorageNodeConfigurationEditor.this.configuration.equals(configuration)) {
                        SC.say("Info", MSG.view_adminTopology_storageNodes_settings_noChanges());
                    } else {
                        SC.ask(
                            MSG.view_adminTopology_storageNodes_settings_confirmation(),
                            new BooleanCallback() {
                                @Override
                                public void execute(Boolean value) {
                                    if (value) {
                                        save(configuration);
                                    }
                                }
                            });
                    }
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

    private StorageNodeConfigurationComposite getConfiguration() {
        StorageNodeConfigurationComposite configuration = new StorageNodeConfigurationComposite(
            this.configuration.getStorageNode());
        configuration.setHeapSize(getJVMMemoryString(form.getField(FIELD_HEAP_MAX).getValue().toString()));
        configuration.setHeapNewSize(getJVMMemoryString(form.getField(FIELD_HEAP_NEW).getValue().toString()));
        configuration.setThreadStackSize(form.getValueAsString(FIELD_THREAD_STACK_SIZE));
        configuration.setJmxPort(Integer.parseInt(form.getValueAsString(FIELD_JMX_PORT)));
        return configuration;
    }

    private boolean checkNewHeapLowerThanMaxHeap() {
        // let's be paranoid
        Object maxHeapObject = form.getField(FIELD_HEAP_MAX).getValue();
        Object newHeapObject = form.getField(FIELD_HEAP_NEW).getValue();

        String maxHeapString = maxHeapObject != null ? maxHeapObject.toString().trim() : "";
        String newHeapString = newHeapObject != null ? newHeapObject.toString().trim() : "";

        if (maxHeapString.isEmpty() || newHeapString.isEmpty()) {
            return false;
        }

        int maxHeap = Integer.parseInt(maxHeapString.substring(0, maxHeapString.length() - 2));
        int newHeap = Integer.parseInt(newHeapString.substring(0, newHeapString.length() - 2));

        boolean isMaxHeapInMegs = maxHeapString.toLowerCase().indexOf("m") != -1;
        boolean isNewHeapInMegs = newHeapString.toLowerCase().indexOf("m") != -1;

        maxHeap = isMaxHeapInMegs ? maxHeap : maxHeap * 1024;
        newHeap = isNewHeapInMegs ? newHeap : newHeap * 1024;

        return newHeap < maxHeap;
    }

    private String getJVMMemoryString(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("input string is null or empty");
        }
        return raw.trim().substring(0, raw.trim().length() - 1);
    }
}
