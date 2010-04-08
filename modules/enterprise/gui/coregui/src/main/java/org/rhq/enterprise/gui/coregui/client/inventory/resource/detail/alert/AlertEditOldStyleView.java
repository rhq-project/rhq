/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.alert;

import java.util.LinkedHashMap;

import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.ValuesManager;
import com.smartgwt.client.widgets.form.fields.HeaderItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.enterprise.gui.coregui.client.components.form.RadioGroupWithComponentsItem;

/**
 * @author Greg Hinkle
 */
public class AlertEditOldStyleView extends VLayout {

    public AlertEditOldStyleView() {
        setWidth100();
    }


    public void displayAsDialog() {
        Window window = new Window();
        window.setTitle("Alert Editor");
        window.setWidth(800);
        window.setHeight(800);
        window.setIsModal(true);
        window.setShowModalMask(true);
        window.setCanDragResize(true);
        window.centerInPage();
        window.addItem(this);
        window.show();
    }


    @Override
    protected void onDraw() {
        super.onDraw();

        addMember(buildEditForm());


    }


    public DynamicForm buildEditForm() {

        ValuesManager vm = new ValuesManager();
        DynamicForm form = new DynamicForm();
        form.setTitleSuffix("");
        form.setValuesManager(vm);

        HeaderItem alertPropertiesHeader = new HeaderItem();
        alertPropertiesHeader.setValue("Alert Properties");

        TextItem alertName = new TextItem("alertName", "Name");
        alertName.setRequired(true);

        TextAreaItem description = new TextAreaItem("alertDescription", "Description");

        SelectItem priority = new SelectItem("alertPriority", "Priority");
        LinkedHashMap<String, String> valueMap = new LinkedHashMap<String, String>();
        valueMap.put(AlertPriority.LOW.name(), "Low");
        valueMap.put(AlertPriority.MEDIUM.name(), "Medium");
        valueMap.put(AlertPriority.HIGH.name(), "High");
        priority.setValueMap(valueMap);
        priority.setImageURLPrefix("alerts/Alert_");
        priority.setImageURLSuffix("_16.png");
        priority.setValue("LOW");



        RadioGroupItem active = new RadioGroupItem("alertActive","Active");
        active.setValueMap("Yes","No");
        active.setValue("Yes");



        HeaderItem conditionsHeader = new HeaderItem();
        conditionsHeader.setValue("Alert Conditions");

        LinkedHashMap valuesMap = new LinkedHashMap();

        valuesMap.put("Metric", buildMetricSectionCavans(vm));

        valuesMap.put("InventoryProperty", buildInventoryPropertySectionCavans(vm));
        valuesMap.put("Event", "Events");
        valuesMap.put("Configuration", "Configuration Change");


        RadioGroupWithComponentsItem conditionGroup = new RadioGroupWithComponentsItem("radioTest", "Test", valuesMap, form);




        form.setItems(alertPropertiesHeader, alertName, description, priority, active, conditionsHeader, conditionGroup);


        return form;

    }

    private DynamicForm buildMetricSectionCavans(ValuesManager vm) {

        DynamicForm form = new DynamicForm();
        form.setTitleSuffix("");
        form.setColWidths("10%");
        form.setValuesManager(vm);


        SelectItem metricSelect = new SelectItem("metric", "Metric");
        metricSelect.setRequired(true);
        metricSelect.setEmptyDisplayValue("Select...");
        metricSelect.setValueMap("CPU Usage", "Free Memory", "Swap Used", "User CPU", "System CPU");


        LinkedHashMap valueMap = new LinkedHashMap();


        DynamicForm subForm1 = new DynamicForm();
        subForm1.setTitleSuffix("");
        subForm1.setValuesManager(vm);
        subForm1.setNumCols(6);
        SelectItem metricConditionComparison = new SelectItem("metricConditionKind", "is");
        metricConditionComparison.setValueMap("> (greater than)", "= (equals)", "< (less than)");

        TextItem metricConditionValue = new TextItem("metricConditionValue");
        metricConditionValue.setShowTitle(false);
        metricConditionValue.setHint("Absolute Value");

        subForm1.setItems(metricConditionComparison, metricConditionValue);

        valueMap.put("Value", subForm1);


        DynamicForm subForm2 = new DynamicForm();
        subForm2.setTitleSuffix("");
        subForm2.setValuesManager(vm);
        subForm2.setNumCols(8);
        SelectItem baselineConditionComparison = new SelectItem("baselineConditionKind", "is");
        baselineConditionComparison.setValueMap("> (greater than)", "= (equals)", "< (less than)");

        TextItem baselineConditionValue = new TextItem("baselineConditionValue");
        baselineConditionValue.setShowTitle(false);
        baselineConditionValue.setHint("%");

        SelectItem baselineRange = new SelectItem("baselineRange", "of");
        baselineRange.setValueMap("25 MB (min value)", "78 MB (avg value)", "322 MB (Max Value)");

        subForm2.setItems(baselineConditionComparison, baselineConditionValue, baselineRange);


        valueMap.put("Baseline", subForm2);


        valueMap.put("valueChanges", "Value Changes");


        RadioGroupWithComponentsItem metricConditionType = new RadioGroupWithComponentsItem("metricConditionType", null, valueMap, form);
        metricConditionType.setShowTitle(false);

        form.setItems(metricSelect, new SpacerItem(), metricConditionType);
        return form;
    }


    private DynamicForm buildInventoryPropertySectionCavans(ValuesManager vm) {
        DynamicForm form = new DynamicForm();
        form.setValuesManager(vm);


        SelectItem inventoryProperty = new SelectItem("inventoryPropert", "Inventory Property");
        inventoryProperty.setValueMap("OS Version", "Architecture", "Vendor", "RAM", "CPU Speed");
        inventoryProperty.setHint("value changes");

        form.setItems(inventoryProperty);


        return form;
    }
}