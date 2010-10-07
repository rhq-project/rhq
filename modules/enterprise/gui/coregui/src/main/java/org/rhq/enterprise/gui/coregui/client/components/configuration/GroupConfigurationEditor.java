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
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.FormItemClickHandler;
import com.smartgwt.client.widgets.form.fields.events.FormItemIconClickEvent;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A SmartGWT widget for editing a group of RHQ {@link Configuration}s that conform to the same
 * {@link ConfigurationDefinition}.
 *
 * @author Ian Springer
 */
public class GroupConfigurationEditor extends ConfigurationEditor {
    private List<GroupMemberConfiguration> memberConfigurations;

    public GroupConfigurationEditor(String locatorId, ConfigurationDefinition configurationDefinition,
                                    List<GroupMemberConfiguration> memberConfigurations) {
        super(locatorId, configurationDefinition, buildAggregateConfiguration(memberConfigurations, configurationDefinition));
        this.memberConfigurations = memberConfigurations;
    }

    private static Configuration buildAggregateConfiguration(
        List<GroupMemberConfiguration> memberConfigurations,
        ConfigurationDefinition configurationDefinition) {
        List<Configuration> configurations = new ArrayList<Configuration>(memberConfigurations.size());
        for (GroupMemberConfiguration memberConfiguration : memberConfigurations) {
            Configuration configuration = memberConfiguration.getConfiguration();
            configurations.add(configuration);
        }
        return AggregateConfigurationBuilder.buildAggregateConfiguration(configurations, configurationDefinition);
    }

    @Override
    protected FormItem buildSimpleField(final PropertyDefinitionSimple propertyDefinitionSimple,
                                        final PropertySimple propertySimple) {
        final FormItem item;
        if (propertySimple.getOverride()) {
            item = super.buildSimpleField(propertyDefinitionSimple, propertySimple);
        } else {
            item = new StaticTextItem();
            item.setTitle("<i>Member Values Differ</i>");
            item.setIcons();
        }

        // Add the icon that user can click to edit the member values.
        FormItemIcon icon = new FormItemIcon();
        icon.setSrc("[SKIN]/actions/edit.png");
        icon.setName("Edit Member Values");
        icon.addFormItemClickHandler(new FormItemClickHandler() {
            public void onFormItemClick(FormItemIconClickEvent event) {
                // TODO: Pass the actual index, rather than null, if the prop is inside a list.
                displayMemberValuesEditor(extendLocatorId("MemberValuesEditor"), propertyDefinitionSimple, item, 
                    propertySimple, null);
            }
        });
        item.setIcons(icon);

        return item;
    }

    private void displayMemberValuesEditor(String locatorId, PropertyDefinitionSimple propertyDefinitionSimple,
                                           final FormItem aggregateValueItem,
                                           final PropertySimple aggregatePropertySimple, Integer index) {
        LocatableVLayout layout = new LocatableVLayout(locatorId);
        layout.setHeight100();

        // TODO: Add 'Set All Values To' and 'Unset All' controls in a separate DynamicForm.

        final DynamicForm form = new DynamicForm();
        form.setHeight100();
        form.setColWidths("55%", "5%", "40%");
        layout.addMember(form);

        // Add header row.
        List<FormItem> items = new ArrayList<FormItem>();
        StaticTextItem memberHeader = new StaticTextItem("memberHeader", "Member");
        items.add(memberHeader);
        StaticTextItem unsetHeader = new StaticTextItem("unsetHeader", "Unset");
        items.add(unsetHeader);
        StaticTextItem valueHeader = new StaticTextItem("valueHeader", "Value");
        valueHeader.setEndRow(true);
        items.add(valueHeader);

        // Add data rows.
        final Map<String, PropertySimple> memberProperties = new HashMap<String, PropertySimple>(this.memberConfigurations.size());
        final List<FormItem> valueItems = new ArrayList<FormItem>(this.memberConfigurations.size());
        for (GroupMemberConfiguration memberConfiguration : this.memberConfigurations) {
            // TODO: Format the disambiguated member name.
            String memberName = memberConfiguration.getLabel();
            StaticTextItem memberItem = new StaticTextItem("member", memberName);
            items.add(memberItem);
            Configuration configuration = memberConfiguration.getConfiguration();
            PropertySimple memberPropertySimple = getPropertySimple(configuration, propertyDefinitionSimple, index);
            memberProperties.put(memberName, memberPropertySimple);
            FormItem valueItem = buildSimpleField(propertyDefinitionSimple, memberPropertySimple);
            valueItem.setAttribute("rhq:property", memberPropertySimple);
            valueItems.add(valueItem);
            FormItem unsetItem = buildUnsetItem(propertyDefinitionSimple, memberPropertySimple, valueItem);
            items.add(unsetItem);
            items.add(valueItem);
            valueItem.setEndRow(true);
        }
        form.setItems(items.toArray(new FormItem[items.size()]));

        final Window popup = new Window();
        popup.setTitle("Member Values for Property '" + propertyDefinitionSimple.getName());
        popup.setWidth(800);
        popup.setHeight(600);
        popup.setIsModal(true);
        popup.setShowModalMask(true);
        popup.setShowCloseButton(false);
        popup.centerInPage();

        final IButton okButton = new IButton("OK");
        okButton.disable();
        okButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                boolean valuesHomogeneous = true;
                String firstValue = (String)valueItems.get(0).getValue();
                for (FormItem valueItem : valueItems) {
                    String value = (String)valueItem.getValue();
                    if ((value != null && !value.equals(firstValue)) || firstValue != null) {
                        valuesHomogeneous = false;
                    }
                    PropertySimple memberPropertySimple =
                        (PropertySimple)valueItem.getAttributeAsObject("rhq:property");
                    memberPropertySimple.setStringValue(value);
                    memberPropertySimple.setErrorMessage(null);
                }
                if (valuesHomogeneous) {
                    // Values are all the same, so update the value of the aggregate property and set its override flag to true.
                    aggregateValueItem.setValue(firstValue);
                    // TODO: Will this cause a value changed event to fire for the aggregate form item?
                    aggregatePropertySimple.setStringValue(firstValue);
                    aggregatePropertySimple.setErrorMessage(null);
                    aggregatePropertySimple.setOverride(true);
                }
                popup.destroy();
            }
        });

        // Only enable the OK button if all properties are valid.
        form.addItemChangedHandler(new ItemChangedHandler() {
            public void onItemChanged(ItemChangedEvent itemChangedEvent) {
                okButton.setDisabled(!form.validate());
            }
        });

        final IButton cancelButton = new IButton("Cancel");
        cancelButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                popup.destroy();
            }
        });

        // TODO: Anchor the button bar at the bottom of the modal window, so it's always visible.
        HLayout buttons = new HLayout();
        buttons.setAlign(Alignment.CENTER);
        buttons.setMembersMargin(10);
        buttons.setMembers(okButton, cancelButton);
        layout.addMember(buttons);

        popup.addItem(layout);

        popup.show();
    }

    private PropertySimple getPropertySimple(Configuration configuration,
                                             PropertyDefinitionSimple propertyDefinitionSimple, Integer index) {
        LinkedList<PropertyDefinition> propertyDefinitionHierarchy = new LinkedList<PropertyDefinition>();
        PropertyDefinition currentPropertyDefinition = propertyDefinitionSimple;
        do {
            propertyDefinitionHierarchy.addFirst(currentPropertyDefinition);
            if (currentPropertyDefinition.getParentPropertyMapDefinition() != null) {
                currentPropertyDefinition = currentPropertyDefinition.getParentPropertyMapDefinition();
            } else if (currentPropertyDefinition.getParentPropertyListDefinition() != null) {
                currentPropertyDefinition = currentPropertyDefinition.getParentPropertyListDefinition();
            } else if (currentPropertyDefinition.getConfigurationDefinition() == null) {
                throw new IllegalStateException(currentPropertyDefinition + " has no parent.");
            }
        }
        while (currentPropertyDefinition.getConfigurationDefinition() == null);

        Property property = configuration.get(propertyDefinitionHierarchy.get(0).getName());
        for (int i = 1, propertyDefinitionHierarchySize = propertyDefinitionHierarchy.size();
             i < propertyDefinitionHierarchySize; i++) {
            PropertyDefinition propertyDefinition = propertyDefinitionHierarchy.get(i);
            if (property instanceof PropertyMap) {
                PropertyMap propertyMap = (PropertyMap)property;
                property = propertyMap.get(propertyDefinition.getName());
            } else if (property instanceof PropertyList) {
                PropertyList propertyList = (PropertyList)property;
                property = propertyList.getList().get(index);
            }
        }

        return (PropertySimple)property;
    }
}
