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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.coregui.client.components.configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridEditEvent;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.events.FormItemClickHandler;
import com.smartgwt.client.widgets.form.fields.events.FormItemIconClickEvent;
import com.smartgwt.client.widgets.form.validator.Validator;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RowEditorExitEvent;
import com.smartgwt.client.widgets.grid.events.RowEditorExitHandler;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.PopupWindow;
import org.rhq.coregui.client.components.form.SortedSelectItem;
import org.rhq.coregui.client.util.StringUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedToolStrip;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * A SmartGWT widget for editing a group of RHQ {@link Configuration}s that conform to the same
 * {@link ConfigurationDefinition}.
 *
 * @author Ian Springer
 */
public class GroupConfigurationEditor extends ConfigurationEditor {

    private static final String RHQ_STATIC_ITEM_NAME_ATTRIBUTE = "rhq:staticItemName";
    private static final String RHQ_UNSET_ITEM_NAME_ATTRIBUTE = "rhq:unsetItemName";

    private static final String MEMBER_VALUES_EDITOR_FIELD_PREFIX = "memberValuesEditor-";
    private static final String FIELD_ID = MEMBER_VALUES_EDITOR_FIELD_PREFIX + "idField";
    private static final String FIELD_MEMBER = MEMBER_VALUES_EDITOR_FIELD_PREFIX + "memberField";
    private static final String FIELD_UNSET = MEMBER_VALUES_EDITOR_FIELD_PREFIX + "unsetField";
    private static final String FIELD_VALUE = MEMBER_VALUES_EDITOR_FIELD_PREFIX + "valueField";
    private static final String ATTR_CONFIG_OBJ = "configObject";

    private List<GroupMemberConfiguration> memberConfigurations;

    public GroupConfigurationEditor(ConfigurationDefinition configurationDefinition,
        List<GroupMemberConfiguration> memberConfigurations) {
        super(configurationDefinition, buildAggregateConfiguration(memberConfigurations, configurationDefinition));
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
            final StaticTextItem staticItem = new StaticTextItem("static-" + propertySimple.getName());
            staticItem.setValue(MSG.view_groupConfigEdit_valsDiff());
            staticItem.setTextBoxStyle("InlineNote");
            staticItem.setShowTitle(false);
            staticItem.setTooltip(MSG.view_groupConfigEdit_tooltip_1());
            Boolean isHomogeneous = isHomogeneous(propertySimple);
            staticItem.setVisible(!isHomogeneous);

            FormItem valueItem = fields.get(2);
            FormItemIcon icon = buildEditMemberValuesIcon(propertyDefinitionSimple, propertySimple, valueItem);
            staticItem.setIcons(icon);

            valueItem.setAttribute(RHQ_STATIC_ITEM_NAME_ATTRIBUTE, staticItem.getName());
            fields.add(3, staticItem);
        }

        return fields;
    }

    @Override
    protected List<FormItem> buildFieldsForPropertyList(PropertyDefinition propertyDefinition, boolean oddRow,
        PropertyDefinitionList propertyDefinitionList, PropertyDefinition memberDefinition, PropertyList propertyList) {
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
                    updateMemberProperties(propertySimple, value);
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
                displayMemberValuesEditor(propertyDefinitionSimple, propertySimple, null, dynamicItem);
            }
        });

        // TODO: Figure out a way to add a tooltip to the icon.

        return icon;
    }

    private void updateMemberProperties(PropertySimple propertySimple, Object value) {
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
        valueItem.setAttribute(RHQ_UNSET_ITEM_NAME_ATTRIBUTE, unsetItem.getName());
        return unsetItem;
    }

    private void displayMemberValuesEditor(final PropertyDefinitionSimple propertyDefinitionSimple,
        final PropertySimple aggregatePropertySimple, Integer index, final FormItem aggregateValueItem) {

        final PopupWindow popup = new PopupWindow(null);
        popup.setTitle(MSG.view_groupConfigEdit_valsDiffForProp(propertyDefinitionSimple.getName()));
        popup.setWidth(800);
        popup.setHeight(600);

        EnhancedVLayout layout = buildMemberEditor(propertyDefinitionSimple, aggregatePropertySimple, index,
            aggregateValueItem, popup);

        popup.addItem(layout);
        popup.show();
    }

    private EnhancedVLayout buildMemberEditor(final PropertyDefinitionSimple propertyDefinitionSimple,
        final PropertySimple aggregatePropertySimple, final Integer index, final FormItem aggregateValueItem,
        final PopupWindow popup) {

        final boolean propertyIsReadOnly = isReadOnly(propertyDefinitionSimple, aggregatePropertySimple);

        EnhancedVLayout layout = new EnhancedVLayout();
        layout.setHeight100();
        layout.setWidth100();
        layout.setMargin(11);

        // create the header strip - will contain "set all values to" form
        EnhancedToolStrip headerStrip = null;
        if (!propertyIsReadOnly) {
            headerStrip = new EnhancedToolStrip();
            headerStrip.setWidth100();
            headerStrip.setPadding(5);
            headerStrip.setMembersMargin(10);
            layout.addMember(headerStrip);
        }

        // create the list grid that contains all the member resource names and values
        final ListGridRecord[] data = getMemberValuesGridData(this.memberConfigurations, propertyDefinitionSimple,
            aggregatePropertySimple, index);
        final ListGrid memberValuesGrid = new ListGrid();
        memberValuesGrid.setHeight100();
        memberValuesGrid.setWidth100();
        memberValuesGrid.setData(data);
        memberValuesGrid.setEditEvent(ListGridEditEvent.CLICK);
        memberValuesGrid.setEditByCell(false); // selecting any cell allows the user to edit the entire row

        ListGridField idField = new ListGridField(FIELD_ID, MSG.common_title_id());
        idField.setHidden(true);
        idField.setCanEdit(false);

        ListGridField memberField = new ListGridField(FIELD_MEMBER, MSG.view_groupConfigEdit_member());
        memberField.setCanEdit(false);

        final ListGridField valueField = new ListGridField(FIELD_VALUE, MSG.common_title_value());
        valueField.setRequired(propertyDefinitionSimple.isRequired());
        valueField.setCanEdit(true);
        final FormItem editorItem = buildEditorFormItem(valueField, propertyDefinitionSimple);
        valueField.setEditorType(editorItem);
        valueField.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                if (value == null) {
                    return "";
                }
                List<PropertyDefinitionEnumeration> enumeratedValues = propertyDefinitionSimple.getEnumeratedValues();
                if (enumeratedValues != null && !enumeratedValues.isEmpty()) {
                    for (PropertyDefinitionEnumeration option : enumeratedValues) {
                        if (option.getValue().equals(value.toString())) {
                            return option.getName();
                        }
                    }
                    return value.toString();
                } else {
                    switch (propertyDefinitionSimple.getType()) {
                    case BOOLEAN: {
                        return BOOLEAN_PROPERTY_ITEM_VALUE_MAP.get(value.toString());
                    }
                    case PASSWORD: {
                        return "******";
                    }
                    default:
                        return value.toString();
                    }
                }
            }
        });

        ListGridField unsetField = new ListGridField(FIELD_UNSET, MSG.view_groupConfigEdit_unset());
        unsetField.setType(ListGridFieldType.BOOLEAN);
        if (propertyDefinitionSimple.isRequired()) {
            unsetField.setHidden(true);
            unsetField.setCanEdit(false);
        } else {
            unsetField.setHidden(false);
            unsetField.setCanEdit(true);
            unsetField.setCanToggle(false); // otherwise, our handler won't get the form

            // add handler to handle when the user flips the unset bit
            unsetField.addChangedHandler(new com.smartgwt.client.widgets.grid.events.ChangedHandler() {
                public void onChanged(com.smartgwt.client.widgets.grid.events.ChangedEvent event) {
                    Boolean isUnset = (Boolean) event.getValue();
                    FormItem valueItem = event.getForm().getField(FIELD_VALUE);
                    if (isUnset) {
                        int rowNum = event.getRowNum();
                        memberValuesGrid.setEditValue(rowNum, FIELD_VALUE, (String) null);
                    } else {
                        valueItem.focusInItem();
                    }
                    valueItem.redraw();
                }
            });

            // add handler when user clears the value field - this should turn on the "unset" button
            valueField.addChangeHandler(new com.smartgwt.client.widgets.grid.events.ChangeHandler() {
                public void onChange(com.smartgwt.client.widgets.grid.events.ChangeEvent event) {
                    int rowNum = event.getRowNum();
                    if (event.getValue() == null || event.getValue().toString().length() == 0) {
                        memberValuesGrid.setEditValue(rowNum, FIELD_UNSET, true);
                    } else {
                        memberValuesGrid.setEditValue(rowNum, FIELD_UNSET, false);
                    }
                }
            });
        }

        idField.setWidth("75");
        memberField.setWidth("*");
        unsetField.setWidth("50");
        valueField.setWidth("45%");
        memberValuesGrid.setFields(idField, memberField, unsetField, valueField);
        layout.addMember(memberValuesGrid);

        // create the footer strip - will contain ok and cancel buttons
        EnhancedToolStrip footerStrip = new EnhancedToolStrip();
        footerStrip.setWidth100();
        footerStrip.setPadding(5);
        footerStrip.setMembersMargin(10);
        layout.addMember(footerStrip);

        // footer OK button
        final IButton okButton = new EnhancedIButton(MSG.common_button_ok(), ButtonColor.BLUE);
        if (!propertyIsReadOnly) {
            // if its read-only, allow the ok button to be enabled to just let the user close the window
            // otherwise, disable it now and we will enable it after we know things are ready to be saved
            okButton.setDisabled(true);
        }
        okButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (!propertyIsReadOnly) {
                    boolean valuesHomogeneous = true;
                    boolean isValid = true;

                    String firstValue = data[0].getAttribute(FIELD_VALUE);
                    int i = 0;
                    for (ListGridRecord valueItem : data) {
                        String value = valueItem.getAttribute(FIELD_VALUE);
                        if (valuesHomogeneous) {
                            if ((value != null && !value.equals(firstValue)) || (value == null && firstValue != null)) {
                                valuesHomogeneous = false;
                            }
                        }
                        isValid = isValid && memberValuesGrid.validateRow(i);
                        Configuration config = (Configuration) valueItem.getAttributeAsObject(ATTR_CONFIG_OBJ);

                        PropertySimple memberPropertySimple = (PropertySimple) getProperty(config,
                            aggregatePropertySimple, index);
                        memberPropertySimple.setValue(value);
                        memberPropertySimple.setErrorMessage(null);
                    }

                    String aggregateStaticItemName = aggregateValueItem.getAttribute(RHQ_STATIC_ITEM_NAME_ATTRIBUTE);
                    FormItem aggregateStaticItem = aggregateValueItem.getForm().getField(aggregateStaticItemName);

                    String aggregateUnsetItemName = aggregateValueItem.getAttribute(RHQ_UNSET_ITEM_NAME_ATTRIBUTE);
                    FormItem aggregateUnsetItem = aggregateValueItem.getForm().getField(aggregateUnsetItemName);
                    aggregateUnsetItem.setDisabled(!valuesHomogeneous);

                    if (valuesHomogeneous) {
                        // Update the value of the aggregate property and set its override flag to true.
                        aggregatePropertySimple.setValue(firstValue);
                        aggregatePropertySimple.setOverride(true);

                        boolean isUnset = (firstValue == null);
                        aggregateUnsetItem.setValue(isUnset);

                        // Set the aggregate value item's value to the homogeneous value, unhide it, and enable it
                        // unless it's unset.
                        setValue(aggregateValueItem, firstValue);
                        aggregateValueItem.show();
                        aggregateValueItem.setDisabled(isUnset);

                        aggregateStaticItem.hide();
                    } else {
                        aggregatePropertySimple.setValue(null);
                        aggregatePropertySimple.setOverride(false);

                        aggregateValueItem.hide();

                        aggregateStaticItem.show();
                    }

                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.view_groupConfigEdit_saveReminder(), Severity.Info));

                    // this has the potential to send another message to the message center
                    firePropertyChangedEvent(aggregatePropertySimple, propertyDefinitionSimple, isValid);
                }

                popup.destroy();
            }
        });
        footerStrip.addMember(okButton);

        // track errors in grid - enable/disable OK button accordingly
        // I tried many ways to get this to work right - this is what I came up with
        memberValuesGrid.addRowEditorExitHandler(new RowEditorExitHandler() {
            public void onRowEditorExit(RowEditorExitEvent event) {
                memberValuesGrid.validateRow(event.getRowNum());
                if (memberValuesGrid.hasErrors()) {
                    okButton.disable();
                } else {
                    okButton.enable();
                }
            }
        });

        // if the data can be changed, add some additional widgets; if not, make the value grid read only
        if (propertyIsReadOnly) {
            memberValuesGrid.setCanEdit(false);
        } else {
            // put a cancel button in the footer strip to allow the user to exit without saving changes
            final IButton cancelButton = new EnhancedIButton(MSG.common_button_cancel());
            cancelButton.focus();
            cancelButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    popup.destroy();
                }
            });
            footerStrip.addMember(cancelButton);

            // create a small form in the header strip to allow the user to apply a value to ALL rows
            final DynamicForm setAllForm = new DynamicForm();
            setAllForm.setNumCols(3);
            setAllForm.setCellPadding(5);
            setAllForm.setColWidths("20%", 50, "*"); // button, unset, value

            // the "set all" value field
            PropertySimple masterPropertySimple = new PropertySimple(propertyDefinitionSimple.getName(), null);
            final FormItem masterValueItem = super.buildSimpleField(propertyDefinitionSimple, masterPropertySimple);
            masterValueItem.setName(MEMBER_VALUES_EDITOR_FIELD_PREFIX + masterValueItem.getName());
            masterValueItem.setValidateOnChange(true);
            masterValueItem.setDisabled(false);
            masterValueItem.setAlign(Alignment.LEFT);
            masterValueItem.setStartRow(false);
            masterValueItem.setEndRow(true);

            // the "set all" unset field
            FormItem masterUnsetItem;
            if (!propertyDefinitionSimple.isRequired()) {
                final CheckboxItem unsetItem = new CheckboxItem(MEMBER_VALUES_EDITOR_FIELD_PREFIX + "unsetall");
                boolean unset = (masterPropertySimple == null || masterPropertySimple.getStringValue() == null);
                unsetItem.setValue(unset);
                unsetItem.setDisabled(isReadOnly(propertyDefinitionSimple, masterPropertySimple));
                unsetItem.setShowLabel(false);
                unsetItem.setLabelAsTitle(false);

                unsetItem.addChangedHandler(new ChangedHandler() {
                    public void onChanged(ChangedEvent changedEvent) {
                        Boolean isUnset = (Boolean) changedEvent.getValue();
                        masterValueItem.setDisabled(isUnset);
                        if (isUnset) {
                            setValue(masterValueItem, null);
                        }
                        masterValueItem.redraw();
                    }
                });

                masterValueItem.addChangeHandler(new ChangeHandler() {
                    public void onChange(ChangeEvent event) {
                        if (event.getValue() == null || event.getValue().toString().length() == 0) {
                            unsetItem.setValue(true);
                        } else {
                            unsetItem.setValue(false);
                        }
                    }
                });

                masterUnsetItem = unsetItem;
            } else {
                masterUnsetItem = new SpacerItem();
            }

            masterUnsetItem.setShowTitle(false);
            masterUnsetItem.setStartRow(false);
            masterUnsetItem.setEndRow(false);
            masterUnsetItem.setAlign(Alignment.CENTER);
            if (masterUnsetItem instanceof CheckboxItem) {
                masterUnsetItem.setValue(false);
            } else {
                masterUnsetItem.setVisible(false);
            }

            // the "set all values to" apply button
            final ButtonItem applyButton = new ButtonItem("apply", MSG.view_groupConfigEdit_setAll());
            applyButton.setDisabled(true);
            applyButton.setAlign(Alignment.RIGHT);
            applyButton.setStartRow(true);
            applyButton.setEndRow(false);

            applyButton.addClickHandler(new ClickHandler() {
                public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
                    if (masterValueItem.validate()) {
                        Object value = masterValueItem.getValue();

                        // Update the member property value items.
                        for (ListGridRecord field : data) {
                            String stringValue = (value != null) ? value.toString() : null;
                            field.setAttribute(FIELD_VALUE, stringValue);
                            field.setAttribute(FIELD_UNSET, value == null);
                        }

                        // since we know the master value is ok and has validated,
                        // we can clear all validation errors and re-enable the OK button
                        memberValuesGrid.discardAllEdits();
                        okButton.enable();

                        // redraw grid with the new values
                        memberValuesGrid.redraw();
                    }
                }
            });

            masterValueItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent changedEvent) {
                    applyButton.enable();
                }
            });

            masterUnsetItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent changedEvent) {
                    applyButton.enable();
                    if ((Boolean) changedEvent.getValue()) {
                        applyButton.focusInItem(); // unsetting value, nothing to enter so go to apply button
                    } else {
                        masterValueItem.focusInItem(); // if turning off "unset", go to value field to enter a value to set
                    }
                }
            });

            setAllForm.setFields(applyButton, masterUnsetItem, masterValueItem);
            headerStrip.addMember(setAllForm);
        }

        return layout;
    }

    @Override
    protected boolean shouldFireEventOnPropertyValueChange(FormItem formItem,
        PropertyDefinitionSimple propertyDefinitionSimple, PropertySimple propertySimple) {
        if (formItem.getName().startsWith(MEMBER_VALUES_EDITOR_FIELD_PREFIX)) {
            return false; // this is a form item in our member values editor dialog - don't fire event for it
        }
        boolean isAgg = isAggregateProperty(propertySimple);
        return isAgg && super.shouldFireEventOnPropertyValueChange(formItem, propertyDefinitionSimple, propertySimple);
    }

    @Override
    protected void updatePropertySimpleValue(FormItem formItem, Object value, PropertySimple propertySimple,
        PropertyDefinitionSimple propertyDefinitionSimple) {
        if ((propertySimple.getConfiguration() != null) || (propertySimple.getParentMap() != null)
            || (propertySimple.getParentList() != null)) {
            // Update the aggregate property. this could also be a member values editor field - but we do the same thing
            super.updatePropertySimpleValue(formItem, value, propertySimple, propertyDefinitionSimple);
            propertySimple.setOverride(true);

            // Update all the member properties if this is a true aggregate property
            if (!formItem.getName().startsWith(MEMBER_VALUES_EDITOR_FIELD_PREFIX)) {
                for (GroupMemberConfiguration memberConfiguration : this.memberConfigurations) {
                    Configuration configuration = memberConfiguration.getConfiguration();
                    PropertySimple memberPropertySimple = (PropertySimple) getProperty(configuration, propertySimple,
                        null);
                    memberPropertySimple.setErrorMessage(null);
                    memberPropertySimple.setValue(value);
                }
            }
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
        List<Property> referenceHierarchy = getParentFirstPropertiesHierarchy(referenceProperty);
        // Add referenceProperty to the configuration, creating parents if necessary
        Property property = configuration.get(referenceHierarchy.get(0).getName());
        if (property == null) {
            property = createPropertyByExample(referenceHierarchy.get(0));
            configuration.put(property);
        }
        for (int i = 1; i < referenceHierarchy.size(); i++) {
            String childPropertyName = referenceHierarchy.get(i).getName();
            if (property instanceof PropertyMap) {
                PropertyMap propertyMap = (PropertyMap) property;
                property = propertyMap.get(childPropertyName);
                if (property == null) {
                    property = createPropertyByExample(referenceHierarchy.get(i));
                    propertyMap.put(property);
                }
            } else if (property instanceof PropertyList) {
                PropertyList propertyList = (PropertyList) property;
                if (index < propertyList.getList().size()) {
                    property = propertyList.getList().get(index);
                } else {
                    property = createPropertyByExample(referenceHierarchy.get(i));
                    propertyList.add(property);
                }
            }
        }

        return property;
    }

    private List<Property> getParentFirstPropertiesHierarchy(Property bottomProperty) {
        LinkedList<Property> propertyHierarchy = new LinkedList<Property>();
        for (Property currentProperty = bottomProperty; currentProperty != null; ) {
            propertyHierarchy.addFirst(currentProperty);
            if (currentProperty.getParentMap() != null) {
                currentProperty = currentProperty.getParentMap();
            } else if (currentProperty.getParentList() != null) {
                currentProperty = currentProperty.getParentList();
            } else if (currentProperty.getConfiguration() == null) {
                throw new IllegalStateException(currentProperty + " has no parent.");
            } else {
                currentProperty = null;
            }
        }
        return new ArrayList<Property>(propertyHierarchy);
    }

    private <T extends Property> T createPropertyByExample(T example) {
        T property = null;
        if (example instanceof PropertyMap) {
            property = (T) new PropertyMap(example.getName());
        } else if (example instanceof PropertyList) {
            property = (T) new PropertyList(example.getName());
        } else {
            property = (T) new PropertySimple(example.getName(), null);
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

    private ListGridRecord[] getMemberValuesGridData(List<GroupMemberConfiguration> memberConfigs,
        PropertyDefinitionSimple propDef, PropertySimple aggregateProp, Integer propertyIndex) {

        ListGridRecord[] records;
        if (memberConfigs == null || memberConfigs.isEmpty()) {
            records = new ListGridRecord[0];
        } else {
            records = new ListGridRecord[memberConfigs.size()];
            int i = 0;
            for (GroupMemberConfiguration item : memberConfigs) {
                ListGridRecord record = new ListGridRecord();
                record.setAttribute(FIELD_ID, item.getId());
                record.setAttribute(FIELD_MEMBER, item.getLabel());

                Configuration configuration = item.getConfiguration();
                PropertySimple memberProp = (PropertySimple) getProperty(configuration, aggregateProp, propertyIndex);
                if (!propDef.isRequired()) {
                    boolean isUnset = (memberProp == null || memberProp.getStringValue() == null);
                    record.setAttribute(FIELD_UNSET, isUnset);
                }

                String value = memberProp.getStringValue();
                if (null == value && null != propDef.getDefaultValue()) {
                    value = propDef.getDefaultValue();
                }
                if (!propDef.isReadOnly()) {
                    // user-editable, so escape HTML, to prevent XSS attacks.
                    value = StringUtility.escapeHtml(value);
                }
                record.setAttribute(FIELD_VALUE, value);
                record.setAttribute(ATTR_CONFIG_OBJ, configuration);
                records[i++] = record;
            }
        }
        return records;
    }

    /**
     * Creates the editor type for the given value field. Validators will be added appropriately.
     *
     * @param valueField the field that is editable
     * @param propDef the definition of the property to be edited in the field
     * @return the editor item
     */
    protected FormItem buildEditorFormItem(ListGridField valueField, final PropertyDefinitionSimple propDef) {
        FormItem editorItem = null;

        // note: we don't have to worry about "read only" properties - if the members
        // value editor is in read-only mode or viewing read-only properties, the list grid
        // isn't editable anyway.

        List<PropertyDefinitionEnumeration> enumeratedValues = propDef.getEnumeratedValues();
        if (enumeratedValues != null && !enumeratedValues.isEmpty()) {
            LinkedHashMap<String, String> valueOptions = new LinkedHashMap<String, String>();
            for (PropertyDefinitionEnumeration option : enumeratedValues) {
                valueOptions.put(option.getValue(), option.getName());
            }

            if (propDef.getAllowCustomEnumeratedValue()) {
                editorItem = new ComboBoxItem();
                ((ComboBoxItem) editorItem).setAddUnknownValues(true);
            } else {
                if (valueOptions.size() > 5) {
                    editorItem = new SortedSelectItem();
                } else {
                    // TODO: we want RadioGroupItem, but smartgwt seems to have a bug and it won't render this when
                    //       our listgrid does not have the "unset" boolean field also present. If its just the value
                    //       field, the radio group editor won't show.
                    editorItem = new SortedSelectItem(); // new RadioGroupItem();
                }
            }
            editorItem.setValueMap(valueOptions);
        } else {
            switch (propDef.getType()) {
            case STRING:
            case FILE:
            case DIRECTORY:
                editorItem = new TextItem();
                break;
            case LONG_STRING:
                editorItem = new TextAreaItem();
                break;
            case PASSWORD:
                editorItem = new PasswordItem();
                break;
            case BOOLEAN:
                // TODO: we want RadioGroupItem, but smartgwt seems to have a bug and it won't render this when
                //       our listgrid does not have the "unset" boolean field also present. If its just the value
                //       field, the radio group editor won't show.
                // RadioGroupItem radioGroupItem = new RadioGroupItem();
                // radioGroupItem.setVertical(false);
                // editorItem = radioGroupItem;
                editorItem = new SelectItem();
                editorItem.setValueMap(BOOLEAN_PROPERTY_ITEM_VALUE_MAP);
                break;
            case INTEGER:
            case LONG:
                editorItem = new TextItem(); // could not get smartgwt listgrid editing to work with IntegerItem
                break;
            case FLOAT:
            case DOUBLE:
                editorItem = new TextItem(); // could not get smartgwt listgrid editing to work with FloatItem
                break;
            }
        }

        editorItem.setName(MEMBER_VALUES_EDITOR_FIELD_PREFIX + "editor");

        List<Validator> validators = buildValidators(propDef, new PropertySimple());
        valueField.setValidators(validators.toArray(new Validator[validators.size()]));

        return editorItem;
    }
}
