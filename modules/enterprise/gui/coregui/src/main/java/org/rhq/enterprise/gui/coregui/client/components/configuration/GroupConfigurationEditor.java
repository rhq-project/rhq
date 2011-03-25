/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.gui.coregui.client.components.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.ItemChangedEvent;
import com.smartgwt.client.widgets.form.events.ItemChangedHandler;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.FormItemClickHandler;
import com.smartgwt.client.widgets.form.fields.events.FormItemIconClickEvent;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A SmartGWT widget for editing a group of RHQ {@link Configuration}s that conform to the same
 * {@link ConfigurationDefinition}.
 *
 * @author Ian Springer
 */
public class GroupConfigurationEditor extends ConfigurationEditor {
    private List<GroupMemberConfiguration> memberConfigurations;
    private Map<String, FormItem> valueItemNameToStaticItemMap = new HashMap<String, FormItem>();
    private Map<String, FormItem> valueItemNameToUnsetItemMap = new HashMap<String, FormItem>();

    public GroupConfigurationEditor(String locatorId, ConfigurationDefinition configurationDefinition,
        List<GroupMemberConfiguration> memberConfigurations) {
        super(locatorId, configurationDefinition, buildAggregateConfiguration(memberConfigurations,
            configurationDefinition));
        this.memberConfigurations = memberConfigurations;
    }

    private static Configuration buildAggregateConfiguration(List<GroupMemberConfiguration> memberConfigurations,
        ConfigurationDefinition configurationDefinition) {
        List<Configuration> configurations = new ArrayList<Configuration>(memberConfigurations.size());
        for (GroupMemberConfiguration memberConfiguration : memberConfigurations) {
            Configuration configuration = memberConfiguration.getConfiguration();
            configurations.add(configuration);
        }
        return AggregateConfigurationBuilder.buildAggregateConfiguration(configurations, configurationDefinition);
    }

    @Override
    protected List<FormItem> buildFieldsForPropertySimple(PropertyDefinition propertyDefinition,
        PropertyDefinitionSimple propertyDefinitionSimple, PropertySimple propertySimple) {
        List<FormItem> fields = super.buildFieldsForPropertySimple(propertyDefinition, propertyDefinitionSimple,
            propertySimple);

        if (isAggregateProperty(propertySimple)) {
            // Create the "MEMBER VALUES DIFFER" label that is displayed when member values are heterogeneous.
            final StaticTextItem staticItem = new StaticTextItem();
            staticItem.setValue(MSG.view_groupConfigEdit_valsDiff());
            staticItem.setTextBoxStyle("InlineNote");
            staticItem.setShowTitle(false);
            staticItem.setTooltip(MSG.view_groupConfigEdit_tooltip_1());
            Boolean isHomogeneous = isHomogeneous(propertySimple);
            staticItem.setVisible(!isHomogeneous);

            FormItem valueItem = fields.get(2);
            FormItemIcon icon = buildEditMemberValuesIcon(propertyDefinitionSimple, propertySimple, valueItem);
            staticItem.setIcons(icon);

            this.valueItemNameToStaticItemMap.put(valueItem.getName(), staticItem);
            fields.add(3, staticItem);
        }

        return fields;
    }

    @Override
    protected List<FormItem> buildFieldsForPropertyList(String locatorId, PropertyDefinition propertyDefinition,
        boolean oddRow, PropertyDefinitionList propertyDefinitionList, PropertyDefinition memberDefinition,
        PropertyList propertyList) {
        List<FormItem> fields = new ArrayList<FormItem>();

        StaticTextItem nameItem = buildNameItem(propertyDefinition);
        fields.add(nameItem);

        StaticTextItem staticTextItem = new StaticTextItem();
        staticTextItem.setShowTitle(false);
        staticTextItem.setValue(MSG.view_groupConfigEdit_noListProps());
        staticTextItem.setColSpan(3);
        staticTextItem.setEndRow(true);
        fields.add(staticTextItem);

        return fields;
    }

    @Override
    protected FormItem buildSimpleField(final PropertyDefinitionSimple propertyDefinitionSimple,
        final PropertySimple propertySimple) {
        final FormItem item = super.buildSimpleField(propertyDefinitionSimple, propertySimple);

        boolean isAggregate = isAggregateProperty(propertySimple);
        if (isAggregate) {
            // Add the icon that user can click to edit the member values.
            FormItemIcon icon = buildEditMemberValuesIcon(propertyDefinitionSimple, propertySimple, item);

            item.setIcons(icon);

            item.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent changedEvent) {
                    Object value = changedEvent.getValue();
                    updateMemberProperties(propertyDefinitionSimple, propertySimple, value);
                }
            });

            Boolean isHomogeneous = isHomogeneous(propertySimple);
            item.setVisible(isHomogeneous);
        }

        return item;
    }

    private FormItemIcon buildEditMemberValuesIcon(final PropertyDefinitionSimple propertyDefinitionSimple,
        final PropertySimple propertySimple, final FormItem dynamicItem) {
        FormItemIcon icon = new FormItemIcon();

        icon.setSrc(ImageManager.getEditIcon());
        icon.setName("Edit Member Values");
        icon.setNeverDisable(true);
        icon.addFormItemClickHandler(new FormItemClickHandler() {
            public void onFormItemClick(FormItemIconClickEvent event) {
                // TODO: Pass the actual index, rather than null, if the prop is inside a list.
                displayMemberValuesEditor(extendLocatorId("MemberValuesEditor"), propertyDefinitionSimple,
                    propertySimple, null, dynamicItem);
            }
        });

        // TODO: Figure out a way to add a tooltip to the icon.

        return icon;
    }

    private void updateMemberProperties(PropertyDefinitionSimple propertyDefinitionSimple,
        PropertySimple propertySimple, Object value) {
        for (GroupMemberConfiguration memberConfiguration : this.memberConfigurations) {
            Configuration configuration = memberConfiguration.getConfiguration();
            PropertySimple memberPropertySimple = (PropertySimple) getProperty(configuration, propertySimple, null);
            memberPropertySimple.setValue(value);
        }
    }

    @Override
    protected FormItem buildUnsetItem(final PropertyDefinitionSimple propertyDefinitionSimple,
        final PropertySimple propertySimple, final FormItem valueItem) {
        final FormItem unsetItem = super.buildUnsetItem(propertyDefinitionSimple, propertySimple, valueItem);
        if (unsetItem instanceof CheckboxItem && !isHomogeneous(propertySimple) && isAggregateProperty(propertySimple)) {
            // non-homogeneous aggregate property (i.e. members have mixed values)
            unsetItem.setValue(false);
            unsetItem.setDisabled(true);
        }
        this.valueItemNameToUnsetItemMap.put(valueItem.getName(), unsetItem);
        return unsetItem;
    }

    private void displayMemberValuesEditor(String locatorId, final PropertyDefinitionSimple propertyDefinitionSimple,
        final PropertySimple aggregatePropertySimple, Integer index, final FormItem aggregateValueItem) {
        final Window popup = new Window();
        popup.setTitle(MSG.view_groupConfigEdit_valsDiffForProp(propertyDefinitionSimple.getName()));
        popup.setWidth(800);
        popup.setHeight(600);
        popup.setIsModal(true);
        popup.setShowModalMask(true);
        popup.setShowCloseButton(false);
        popup.centerInPage();

        LocatableVLayout layout = new LocatableVLayout(locatorId);
        layout.setHeight100();
        layout.setMargin(11);

        final DynamicForm setAllForm = new DynamicForm();
        setAllForm.setNumCols(4);
        setAllForm.setColWidths(175, 25, 200, 100);
        setAllForm.setCellPadding(5);

        List<FormItem> setAllItems = new ArrayList<FormItem>();

        // Header Row
        SpacerItem spacerItem = new SpacerItem();
        setAllItems.add(spacerItem);

        StaticTextItem unsetAllHeader = new StaticTextItem();
        unsetAllHeader.setShowTitle(false);
        if (!propertyDefinitionSimple.isRequired()) {
            unsetAllHeader.setDefaultValue("<h4>" + MSG.view_groupConfigEdit_unset() + "</h4>");
        }
        setAllItems.add(unsetAllHeader);

        StaticTextItem valueAllHeader = new StaticTextItem();
        valueAllHeader.setShowTitle(false);
        valueAllHeader.setDefaultValue("<h4>" + MSG.common_title_value() + "</h4>");

        setAllItems.add(valueAllHeader);

        spacerItem = new SpacerItem();
        //spacerItem.setEndRow(true);
        setAllItems.add(spacerItem);

        // Input Row
        StaticTextItem setAllValuesToItem = new StaticTextItem();
        setAllValuesToItem.setShowTitle(false);
        setAllValuesToItem.setDefaultValue("<b>" + MSG.view_groupConfigEdit_setAll() + "</b>");
        setAllValuesToItem.setAlign(Alignment.RIGHT);
        setAllItems.add(setAllValuesToItem);

        PropertySimple masterPropertySimple = new PropertySimple(aggregatePropertySimple.getName(), null);
        final FormItem masterValueItem = super.buildSimpleField(propertyDefinitionSimple, masterPropertySimple);
        masterValueItem.setDisabled(false);

        FormItem masterUnsetItem = buildUnsetItem(propertyDefinitionSimple, masterPropertySimple, masterValueItem);
        if (masterUnsetItem instanceof CheckboxItem) {
            masterUnsetItem.setValue(false);
        }

        setAllItems.add(masterUnsetItem);
        setAllItems.add(masterValueItem);

        //ButtonItem applyButtonItem = new ButtonItem();
        //applyButtonItem.setTitle("Apply");
        //applyButtonItem.setEndRow(true);
        //setAllItems.add(applyButtonItem);

        // NOTE (ips, 10/27/10): Using a ButtonItem didn't work out, because SmartGWT would always display it in a new
        //                       table row, rather than in the next column in the current row (most likely a bug). So
        //                       we use an IButton wrapped in a CanvasItem instead.
        CanvasItem canvasItem = new CanvasItem();
        canvasItem.setShowTitle(false);
        HLayout buttonCanvas = new HLayout();
        buttonCanvas.setWidth(90);
        buttonCanvas.setHeight100();
        buttonCanvas.setAlign(Alignment.LEFT);
        final IButton applyButton = new LocatableIButton(layout.extendLocatorId("Apply"), MSG.common_button_apply());
        applyButton.setDisabled(true);
        buttonCanvas.addMember(applyButton);
        canvasItem.setCanvas(buttonCanvas);
        canvasItem.setEndRow(true);
        setAllItems.add(canvasItem);

        masterValueItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent changedEvent) {
                applyButton.enable();
                applyButton.focus();
            }
        });

        masterUnsetItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent changedEvent) {
                applyButton.enable();
                applyButton.focus();
            }
        });

        setAllForm.setFields(setAllItems.toArray(new FormItem[setAllItems.size()]));
        layout.addMember(setAllForm);

        VLayout spacerLayout = new VLayout();
        spacerLayout.setHeight(20);
        layout.addMember(spacerLayout);

        final DynamicForm membersForm = new DynamicForm();
        membersForm.setHeight100();
        membersForm.setNumCols(3);
        membersForm.setColWidths("52%", "8%", "40%");
        membersForm.setHiliteRequiredFields(true);
        layout.addMember(membersForm);

        // Add header row.
        List<FormItem> items = new ArrayList<FormItem>();
        StaticTextItem memberHeader = new StaticTextItem();
        memberHeader.setShowTitle(false);
        memberHeader.setDefaultValue("<h4>" + MSG.view_groupConfigEdit_member() + "</h4>");
        items.add(memberHeader);
        StaticTextItem unsetHeader = new StaticTextItem();
        unsetHeader.setShowTitle(false);
        if (!propertyDefinitionSimple.isRequired()) {
            unsetHeader.setDefaultValue("<h4>" + MSG.view_groupConfigEdit_unset() + "</h4>");
        }
        items.add(unsetHeader);
        StaticTextItem valueHeader = new StaticTextItem();
        valueHeader.setShowTitle(false);
        valueHeader.setDefaultValue("<h4>" + MSG.common_title_value() + "</h4>");
        valueHeader.setEndRow(true);
        items.add(valueHeader);

        // Add data rows.
        final List<FormItem> valueItems = new ArrayList<FormItem>(this.memberConfigurations.size());
        final Map<String, PropertySimple> valueItemNameToPropertySimpleMap = new HashMap<String, PropertySimple>();
        for (GroupMemberConfiguration memberConfiguration : this.memberConfigurations) {
            String memberName = memberConfiguration.getLabel();
            StaticTextItem memberItem = new StaticTextItem();
            memberItem.setShowTitle(false);
            memberItem.setDefaultValue(memberName);
            items.add(memberItem);
            Configuration configuration = memberConfiguration.getConfiguration();
            PropertySimple memberPropertySimple = (PropertySimple) getProperty(configuration, aggregatePropertySimple,
                index);
            FormItem valueItem = buildSimpleField(propertyDefinitionSimple, memberPropertySimple);
            valueItems.add(valueItem);
            valueItemNameToPropertySimpleMap.put(valueItem.getName(), memberPropertySimple);
            FormItem unsetItem = buildUnsetItem(propertyDefinitionSimple, memberPropertySimple, valueItem);
            items.add(unsetItem);
            items.add(valueItem);
            valueItem.setEndRow(true);
        }
        membersForm.setItems(items.toArray(new FormItem[items.size()]));

        for (FormItem valueItem : valueItems) {
            String defaultValue = valueItem.getAttribute("defaultValue");
            setValue(valueItem, defaultValue);
            valueItem.setDisabled(false);
        }

        final IButton okButton = new LocatableIButton(layout.extendLocatorId("OK"), MSG.common_button_ok());
        okButton.disable();
        okButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                boolean valuesHomogeneous = true;
                boolean isValid = true;

                Object firstValue = valueItems.get(0).getValue();
                for (FormItem valueItem : valueItems) {
                    Object value = valueItem.getValue();
                    if ((value != null && !value.equals(firstValue)) || (value == null && firstValue != null)) {
                        valuesHomogeneous = false;
                    }
                    isValid = isValid && valueItem.validate();
                    PropertySimple memberPropertySimple = valueItemNameToPropertySimpleMap.get(valueItem.getName());
                    memberPropertySimple.setValue(value);
                    memberPropertySimple.setErrorMessage(null);
                }

                FormItem aggregateStaticItem = valueItemNameToStaticItemMap.get(aggregateValueItem.getName());

                FormItem aggregateUnsetItem = valueItemNameToUnsetItemMap.get(aggregateValueItem.getName());
                aggregateUnsetItem.setDisabled(!valuesHomogeneous);

                if (valuesHomogeneous) {
                    // Update the value of the aggregate property and set its override flag to true.
                    aggregatePropertySimple.setValue(firstValue);
                    aggregatePropertySimple.setOverride(true);

                    aggregateUnsetItem.setValue(firstValue == null);

                    // Set the aggregate value item's value to the homogeneous value, unhide it, and enable it.
                    setValue(aggregateValueItem, firstValue);
                    aggregateValueItem.show();
                    aggregateValueItem.setDisabled(false);

                    aggregateStaticItem.hide();
                } else {
                    aggregatePropertySimple.setValue(null);
                    aggregatePropertySimple.setOverride(false);

                    aggregateValueItem.hide();

                    aggregateStaticItem.show();
                }

                firePropertyChangedEvent(aggregatePropertySimple, propertyDefinitionSimple, isValid);

                popup.destroy();
            }
        });

        // Only enable the OK button if all properties are valid.
        membersForm.addItemChangedHandler(new ItemChangedHandler() {
            public void onItemChanged(ItemChangedEvent itemChangedEvent) {
                okButton.setDisabled(!membersForm.validate());
            }
        });

        applyButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                Object value = masterValueItem.getValue();
                for (FormItem valueItem : valueItems) {
                    setValue(valueItem, value);

                    FormItem unsetItem = valueItemNameToUnsetItemMap.get(valueItem.getName());
                    if (unsetItem instanceof CheckboxItem) {
                        unsetItem.setValue((value == null));
                        valueItem.setDisabled((value == null));
                    }

                    okButton.setDisabled(!membersForm.validate());
                }
            }
        });

        final IButton cancelButton = new LocatableIButton(layout.extendLocatorId("Cancel"), MSG.common_button_cancel());
        cancelButton.focus();
        cancelButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                popup.destroy();
            }
        });

        // TODO: Anchor the button bar at the bottom of the modal window, so it's always visible.
        HLayout buttons = new HLayout();
        buttons.setAlign(Alignment.CENTER);
        buttons.setMembersMargin(7);
        buttons.setMembers(okButton, cancelButton);
        layout.addMember(buttons);

        popup.addItem(layout);
        popup.show();
    }

    @Override
    protected boolean fireEventOnPropertyValueChange(PropertyDefinitionSimple propertyDefinitionSimple,
                                                     PropertySimple propertySimple) {
        return isAggregateProperty(propertySimple)
            && super.fireEventOnPropertyValueChange(propertyDefinitionSimple, propertySimple);
    }

    @Override
    protected void updatePropertySimpleValue(Object value, PropertySimple propertySimple,
        PropertyDefinitionSimple propertyDefinitionSimple) {
        // Update the aggregate property
        super.updatePropertySimpleValue(value, propertySimple, propertyDefinitionSimple);
        propertySimple.setOverride(true);

        // Update all the member properties.
        for (GroupMemberConfiguration memberConfiguration : this.memberConfigurations) {
            Configuration configuration = memberConfiguration.getConfiguration();
            PropertySimple memberPropertySimple = (PropertySimple) getProperty(configuration, propertySimple, null);
            memberPropertySimple.setErrorMessage(null);
            memberPropertySimple.setValue(value);
        }
    }

    @Override
    protected void removePropertyFromDynamicMap(PropertySimple propertySimple) {
        // Remove the aggregate property.
        super.removePropertyFromDynamicMap(propertySimple);

        // Remove the member properties.
        for (GroupMemberConfiguration memberConfiguration : this.memberConfigurations) {
            Configuration configuration = memberConfiguration.getConfiguration();
            PropertySimple memberPropertySimple = (PropertySimple) getProperty(configuration, propertySimple, null);
            PropertyMap parentMap = memberPropertySimple.getParentMap();
            parentMap.getMap().remove(memberPropertySimple.getName());
        }
    }

    @Override
    protected void addPropertyToDynamicMap(PropertySimple propertySimple, PropertyMap propertyMap) {
        // Add the aggregate property.
        super.addPropertyToDynamicMap(propertySimple, propertyMap);

        // Add the member properties.
        for (GroupMemberConfiguration memberConfiguration : this.memberConfigurations) {
            Configuration configuration = memberConfiguration.getConfiguration();
            // The below call will create the member property and add it to the appropriate parent property in the member config.
            PropertySimple memberPropertySimple = (PropertySimple) getProperty(configuration, propertySimple, null);
        }
    }

    private Property getProperty(Configuration configuration, Property referenceProperty, Integer index) {
        LinkedList<Property> propertyHierarchy = new LinkedList<Property>();
        Property currentProperty = referenceProperty;
        propertyHierarchy.add(currentProperty);
        do {
            if (currentProperty.getParentMap() != null) {
                currentProperty = currentProperty.getParentMap();
            } else if (currentProperty.getParentList() != null) {
                currentProperty = currentProperty.getParentList();
            } else if (currentProperty.getConfiguration() == null) {
                throw new IllegalStateException(currentProperty + " has no parent.");
            }
            propertyHierarchy.addFirst(currentProperty);
        } while (currentProperty.getConfiguration() == null);

        Property property = configuration.get(propertyHierarchy.get(0).getName());
        for (int i = 1, propertyHierarchySize = propertyHierarchy.size(); i < propertyHierarchySize; i++) {
            String childPropertyName = propertyHierarchy.get(i).getName();
            if (property instanceof PropertyMap) {
                PropertyMap propertyMap = (PropertyMap) property;
                property = propertyMap.get(childPropertyName);
                if (property == null) {
                    property = new PropertySimple(childPropertyName, null);
                    propertyMap.put(property);
                }
            } else if (property instanceof PropertyList) {
                PropertyList propertyList = (PropertyList) property;
                if (index < propertyList.getList().size()) {
                    property = propertyList.getList().get(index);
                } else {
                    property = new PropertySimple(childPropertyName, null);
                    propertyList.add(property);
                }
            }
        }

        return property;
    }

    private boolean isAggregateProperty(PropertySimple propertySimple) {
        return (getConfiguration(propertySimple) == getConfiguration());
    }

    private static Boolean isHomogeneous(PropertySimple propertySimple) {
        Boolean override = propertySimple.getOverride();
        return (override != null && override);
    }

    private static Configuration getConfiguration(Property property) {
        Property topLevelProperty = getTopLevelProperty(property);
        return topLevelProperty.getConfiguration();
    }
}
