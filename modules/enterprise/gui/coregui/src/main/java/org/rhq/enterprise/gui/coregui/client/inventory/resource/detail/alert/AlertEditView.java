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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.FormItemIfFunction;
import com.smartgwt.client.widgets.form.ValuesManager;
import com.smartgwt.client.widgets.form.fields.BlurbItem;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.HeaderItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.components.form.RadioGroupWithComponentsItem;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;

/**
 * @author Greg Hinkle
 */
public class AlertEditView extends VLayout {

    private Resource resource;
    private ResourceType resourceType;

    public AlertEditView(Resource resource) {
        this.resource = resource;
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

        ResourceTypeRepository.Cache.getInstance().getResourceTypes(resource.getResourceType().getId(),
                EnumSet.of(ResourceTypeRepository.MetadataType.measurements, ResourceTypeRepository.MetadataType.operations),
                new ResourceTypeRepository.TypeLoadedCallback() {
                    public void onTypesLoaded(ResourceType type) {
                        resourceType = type;
                        addMember(buildEditForm());

                        addMember(buildConditionSection(0));
                    }
                });
    }


    public DynamicForm buildEditForm() {

        ValuesManager vm = new ValuesManager();
        DynamicForm form = new DynamicForm();
        form.setNumCols(4);
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

        LinkedHashMap<String, String> iconMap = new LinkedHashMap<String, String>();
        iconMap.put(AlertPriority.LOW.name(), AlertPriority.LOW.name());
        iconMap.put(AlertPriority.MEDIUM.name(), AlertPriority.MEDIUM.name());
        iconMap.put(AlertPriority.HIGH.name(), AlertPriority.HIGH.name());
        priority.setValueIcons(iconMap);
        priority.setImageURLPrefix("subsystems/alert/Alert_");
        priority.setImageURLSuffix("_16.png");
        priority.setValue("LOW");


        RadioGroupItem active = new RadioGroupItem("alertActive", "Active");

        active.setValueMap("Yes", "No");
        active.setValue("Yes");


        HeaderItem conditionsHeader = new HeaderItem();
        conditionsHeader.setValue("Alert Conditions");


/*
        LinkedHashMap valuesMap = new LinkedHashMap();
        valuesMap.put("Metric", buildMetricSectionCavans(vm));
        valuesMap.put("InventoryProperty", buildInventoryPropertySectionCavans(vm));
        valuesMap.put("Event", "Events");
        valuesMap.put("Configuration", "Configuration Change");
        RadioGroupWithComponentsItem conditionGroup = new RadioGroupWithComponentsItem("radioTest", "Test", valuesMap, form);
*/

        form.setItems(alertPropertiesHeader, alertName, priority, description, active, conditionsHeader);


        return form;

    }


    /*
    AVAILABILITY("Resource Availability"), //
    THRESHOLD("Measurement Threshold"), //
    BASELINE("Measurement Baseline"), //
    CHANGE("Measurement Value Change"), //
    TRAIT("Measurement Trait"), //
    CONTROL("Control Action"), //
    ALERT("Alert Fired"), //
    RESOURCE_CONFIG("Resource Configuration Property Value Change"), //
    EVENT("Log Event");
    */


    private DynamicForm buildConditionSection(final int ci) {

        final DynamicForm form = new DynamicForm();

        ValuesManager vm = new ValuesManager();
        form.setValuesManager(vm);

        form.setNumCols(3);
        form.setColWidths("30", "120", "*");
        ArrayList<FormItem> items = new ArrayList<FormItem>();


        RadioGroupItem conditionType = new RadioGroupItem("conditionType" + ci, "Condition Type");
        conditionType.setImageURLPrefix("subsystems/");
        conditionType.setImageURLSuffix("_16.png");
        items.add(conditionType);

        LinkedHashMap<String, String> valueMap = new LinkedHashMap<String, String>();
        valueMap.put(AlertConditionCategory.AVAILABILITY.name(), AlertConditionCategory.AVAILABILITY.getDisplayName());
        valueMap.put("metric", "Metric");
        valueMap.put(AlertConditionCategory.TRAIT.name(), AlertConditionCategory.TRAIT.getDisplayName());
        valueMap.put(AlertConditionCategory.CONTROL.name(), AlertConditionCategory.CONTROL.getDisplayName());
        valueMap.put(AlertConditionCategory.EVENT.name(), AlertConditionCategory.EVENT.getDisplayName());
        conditionType.setValueMap(valueMap);
        conditionType.setRedrawOnChange(true);


        LinkedHashMap<String, String> iconMap = new LinkedHashMap<String, String>();
        iconMap.put(AlertConditionCategory.AVAILABILITY.name(), "availability/availability_red");
        iconMap.put("metric", "monitor/Monitor_failed");
        iconMap.put(AlertConditionCategory.TRAIT.name(), "inventory/Inventory");
        iconMap.put(AlertConditionCategory.CONTROL.name(), "control/Operation");
        iconMap.put(AlertConditionCategory.EVENT.name(), "event/Events_error");

        conditionType.setValueIcons(iconMap);
        conditionType.setValue(AlertConditionCategory.AVAILABILITY.name());
//        conditionType.setValueIconSize(24);http://localhost:7080/coregui/images/availability/availability_red
        conditionType.setShowIcons(true);

/*
        for (final String key : valueMap.keySet()) {
            BlurbItem icon = new BlurbItem();
            icon.setColSpan(1);
            icon.setEndRow(false);
            icon.setValue(Canvas.imgHTML("subsystems/" + iconMap.get(key) + "_24.png"));
            icon.setShowTitle(false);
            icon.setWidth(30);
            icon.setHeight(30);
            items.add(icon);

            final RadioGroupItem radio = new RadioGroupItem("conditionType" + ci);
            radio.setStartRow(false);
            radio.setShowTitle(false);
            radio.setValueMap(valueMap.get(key));
            radio.setRedrawOnChange(true);
            items.add(radio);

            icon.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    radio.setValue(key);
                }
            });

        }*/

        CanvasItem availCanvas = buildAvailabilitySectionCanvas(vm);
        availCanvas.setShowIfCondition(new FormItemIfFunction() {
            public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                return AlertConditionCategory.AVAILABILITY.name().equals(form.getValue("conditionType" + ci));
            }
        });
        items.add(availCanvas);

        CanvasItem metricCanvas = buildMetricSectionCavans(vm);
        metricCanvas.setShowIfCondition(new FormItemIfFunction() {
            public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                return "metric".equals(form.getValue("conditionType" + ci));
            }
        });
        items.add(metricCanvas);


        CanvasItem propertyCanvas = buildInventoryPropertySectionCavans(vm);
        propertyCanvas.setShowIfCondition(new FormItemIfFunction() {
            public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                return AlertConditionCategory.TRAIT.name().equals(form.getValue("conditionType" + ci));
            }
        });
        items.add(propertyCanvas);


        CanvasItem operationCanvas = buildOperationsSectionCanvas(vm);
        operationCanvas.setShowIfCondition(new FormItemIfFunction() {
            public boolean execute(FormItem formItem, Object o, DynamicForm dynamicForm) {
                return AlertConditionCategory.CONTROL.name().equals(form.getValue("conditionType" + ci));
            }
        });
        items.add(operationCanvas);


        form.setItems(items.toArray(new FormItem[items.size()]));
        return form;
    }

    private CanvasItem buildAvailabilitySectionCanvas(ValuesManager vm) {
        DynamicForm form = new DynamicForm();
        form.setTitleSuffix("");
        form.setColWidths("10%");
        form.setValuesManager(vm);


        SelectItem metricSelect = new SelectItem("availChange", "Avaialability Change");
        metricSelect.setRequired(true);
        metricSelect.setEmptyDisplayValue("Select...");
        metricSelect.setValueMap("Goes UP", "Goes DOWN");


        form.setItems(metricSelect);


        CanvasItem canvasItem = new CanvasItem("metricConditionCanvas");
        canvasItem.setShowTitle(false);
        canvasItem.setCanvas(form);

        return canvasItem;

    }

    private CanvasItem buildOperationsSectionCanvas(ValuesManager vm) {
        final DynamicForm form = new DynamicForm();
        form.setTitleSuffix("");
        form.setColWidths("10%");
        form.setValuesManager(vm);


        final SelectItem operationSelect = new SelectItem("operation", "Operation");
        operationSelect.setRequired(true);
        operationSelect.setEmptyDisplayValue("Select...");
        operationSelect.setShowHint(true);


        LinkedHashMap<String, String> operations = new LinkedHashMap<String, String>();
        for (OperationDefinition def : resourceType.getOperationDefinitions()) {
            operations.put(def.getName(), def.getDisplayName());
        }
        operationSelect.setValueMap(operations);
        operationSelect.setRedrawOnChange(true);

        operationSelect.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent changedEvent) {
                for (OperationDefinition def : resourceType.getOperationDefinitions()) {

                    if (def.getName().equals(operationSelect.getValue())) {
                        operationSelect.setHint(def.getDescription());
                    }
                }
            }
        });


        form.setItems(operationSelect);

        CanvasItem canvasItem = new CanvasItem("operationConditionCanvas");
        canvasItem.setShowTitle(false);
        canvasItem.setCanvas(form);

        return canvasItem;

    }

    private CanvasItem buildMetricSectionCavans(ValuesManager vm) {

        DynamicForm form = new DynamicForm();
        form.setTitleSuffix("");
        form.setColWidths("10%", "90%");
        form.setValuesManager(vm);


        final SelectItem metricSelect = new SelectItem("metric", "Metric");
        metricSelect.setRequired(true);
        metricSelect.setEmptyDisplayValue("Select...");
        metricSelect.setWidth(200);

        LinkedHashMap<String, String> metrics = new LinkedHashMap<String, String>();
        for (MeasurementDefinition def : resourceType.getMetricDefinitions()) {
            if (def.getDataType() == DataType.MEASUREMENT) {
                metrics.put(def.getName(), def.getDisplayName());
            }
        }
        metricSelect.setValueMap(metrics);


        metricSelect.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent changedEvent) {
                for (MeasurementDefinition def : resourceType.getMetricDefinitions()) {

                    if (def.getName().equals(metricSelect.getValue())) {
                        metricSelect.setHint(def.getDescription());
                    }
                }
            }
        });





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


        valueMap.put("valueChanges", new Label("Value Changes"));


        RadioGroupWithComponentsItem metricConditionType = new RadioGroupWithComponentsItem("metricConditionType", null, valueMap, form);
        metricConditionType.setShowTitle(false);

        form.setItems(metricSelect, new SpacerItem(), metricConditionType);

        CanvasItem canvasItem = new CanvasItem("metricConditionCanvas");
        canvasItem.setShowTitle(false);
        canvasItem.setCanvas(form);

        return canvasItem;
    }


    private CanvasItem buildInventoryPropertySectionCavans(ValuesManager vm) {
        DynamicForm form = new DynamicForm();
        form.setValuesManager(vm);


        SelectItem inventoryProperty = new SelectItem("inventoryPropert", "Inventory Property");
        inventoryProperty.setValueMap("OS Version", "Architecture", "Vendor", "RAM", "CPU Speed");
        inventoryProperty.setHint("value changes");

        form.setItems(inventoryProperty);


        CanvasItem canvasItem = new CanvasItem("inventoryPropertyCanvas");
        canvasItem.setShowTitle(false);
        canvasItem.setCanvas(form);

        return canvasItem;
    }
}
