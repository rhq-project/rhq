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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.LayoutSpacer;

import org.rhq.core.domain.cloud.StorageNodeConfigurationComposite;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeEvent;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeListener;
import org.rhq.enterprise.gui.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.enterprise.gui.coregui.client.components.form.ValueWithUnitsItem;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedToolStrip;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * The component for editing the storage node configuration
 *
 * @author Jirka Kremser
 */
public class StorageNodeConfigurationEditor extends EnhancedVLayout implements PropertyValueChangeListener,
    RefreshableView {

    private EnhancedDynamicForm form;
    private EnhancedToolStrip toolStrip;
    private boolean oddRow;
    private final StorageNodeConfigurationComposite configuration;

    public StorageNodeConfigurationEditor(final StorageNodeConfigurationComposite configuration) {
        super();
        this.configuration = configuration;
        
    }

    private void save() {

    }

    private List<FormItem> buildOneFormRow(String name, String title, String value, String description,
        boolean unitsDropdown) {
        List<FormItem> fields = new ArrayList<FormItem>();
        StaticTextItem nameItem = new StaticTextItem();
        nameItem.setStartRow(true);
        nameItem.setValue("<b>" + title + "</b>");
        nameItem.setShowTitle(false);
        nameItem.setCellStyle(oddRow ? "OddRow" : "EvenRow");
        fields.add(nameItem);

        FormItem valueItem = null;
        if (unitsDropdown) {
            valueItem = buildJMXMemoryItem(name, value);
        } else {
            valueItem = new TextItem();
            valueItem.setName(name);
            valueItem.setValue(value);
            valueItem.setWidth(220);
        }
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

    private FormItem buildJMXMemoryItem(String name, String value) {
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
                //nothing
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
        form.setGroupTitle("Storage Node Specific Settings");
        form.setBorder("1px solid #AAA");
        oddRow = true;

        List<FormItem> items = buildHeaderItems();
        items
            .addAll(buildOneFormRow(
                "foo2",
                "Max Heap Size",
                configuration.getHeapSize(),
                "The maximum heap size. This value will be used with the -Xmx JVM option. The value should be an integer with a suffix of M or G to indicate megabytes or gigabytes.",
                true));
        items
            .addAll(buildOneFormRow(
                "foo",
                "Heap New Size",
                configuration.getHeapNewSize(),
                "The size of the new generation portion of the heap. This value will be used with the -Xmn JVM option. The value should be an integer with a suffix of M or G to indicate megabytes or gigabytes.",
                true));

        items.addAll(buildOneFormRow("foo3", "Thread Stack Size", configuration.getThreadStackSize(),
            "asdfsdfffa df sdbla", false));
        items.addAll(buildOneFormRow("foo4", "JMX Port", String.valueOf(configuration.getJmxPort()),
            "sdfla ffa blsdfa", false));
        form.setFields(items.toArray(new FormItem[items.size()]));
        form.validate();

        EnhancedIButton saveButton = new EnhancedIButton(MSG.common_button_save());
        saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                save();
            }
        });
        toolStrip = new EnhancedToolStrip();
        toolStrip.setWidth100();
        toolStrip.setMembersMargin(5);
        toolStrip.setLayoutMargin(5);
        toolStrip.addMember(saveButton);
        form.setWidth100();
        form.setOverflow(Overflow.VISIBLE);
        setWidth100();
        LayoutSpacer spacer = new LayoutSpacer();
        spacer.setWidth100();
        setMembers(form, spacer, toolStrip);
        markForRedraw();
    }

    @Override
    public void propertyValueChanged(PropertyValueChangeEvent event) {
        
    }
}
