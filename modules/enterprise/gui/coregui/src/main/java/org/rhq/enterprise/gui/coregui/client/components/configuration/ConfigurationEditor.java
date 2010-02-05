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
package org.rhq.enterprise.gui.coregui.client.components.configuration;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.enterprise.gui.coregui.client.inventory.configuration.ConfigurationGwtService;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.ItemChangedEvent;
import com.smartgwt.client.widgets.form.events.ItemChangedHandler;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FileItem;
import com.smartgwt.client.widgets.form.fields.FloatItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.IntegerItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuButton;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author Greg Hinkle
 */
public class ConfigurationEditor extends VLayout {

    private ConfigurationDefinition definition;
    private Configuration configuration;

    public ConfigurationEditor() {


        setOverflow(Overflow.AUTO);


        ConfigurationGwtService.App.getInstance().getResourceConfiguration(10005, new AsyncCallback<Configuration>() {
            public void onFailure(Throwable caught) {
                // TODO: Implement this method.
            }

            public void onSuccess(Configuration result) {
                configuration = result;
                System.out.println("-------Got config");
                reload();
            }
        });

        ConfigurationGwtService.App.getInstance().getResourceConfigurationDefinition(10060, new AsyncCallback<ConfigurationDefinition>() {
            public void onFailure(Throwable caught) {
                // TODO: Implement this method.
            }

            public void onSuccess(ConfigurationDefinition result) {
                definition = result;
                System.out.println("-------Got def");
                reload();
            }

            ;
        });
    }


    public void reload() {
        if (definition == null || configuration == null) {
            return;
        }


        List<PropertyGroupDefinition> definitions = definition.getGroupDefinitions();


        final SectionStack sectionStack = new SectionStack();
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        sectionStack.setWidth100();
        sectionStack.setScrollSectionIntoView(true);
        sectionStack.setOverflow(Overflow.VISIBLE);

        for (PropertyGroupDefinition definition : definitions) {
//            System.out.println("building: " + definition.getDisplayName());
            sectionStack.addSection(buildGroupSection(definition));
        }


        Menu menu = new Menu();
        for (PropertyGroupDefinition definition : definitions) {
            MenuItem item = new MenuItem(definition.getDisplayName());
            item.addClickHandler(new ClickHandler() {
                public void onClick(MenuItemClickEvent event) {
                    int x = event.getMenu().getItemNum(event.getItem());
                    sectionStack.expandSection(x);
//                    SectionStackSection section = sectionStack.getSection(x);
//                    sectionStack.scrollTo(0,sectionStack.);
                    sectionStack.showSection(x);
                }
            });
            menu.addItem(item);
        }

        addMember(new MenuButton("Section", menu));

        addMember(sectionStack);

        this.redraw();
    }


    public SectionStackSection buildGroupSection(PropertyGroupDefinition group) {
        SectionStackSection section = new SectionStackSection(group.getDisplayName() +
                (group.getDescription() != null
                ? ("<div style='padding-left: 20px; font-weight: normal; font-size: smaller;'>" + group.getDescription() + "</div>")
                  : ""));
        section.setExpanded(!group.isDefaultHidden());


        DynamicForm form = new DynamicForm();

        form.addItemChangedHandler(new ItemChangedHandler() {
            public void onItemChanged(ItemChangedEvent itemChangedEvent) {

            }
        });

        form.setNumCols(4);
        form.setCellPadding(5);
        form.setColWidths(190, 28, 210);


        ArrayList<FormItem> fields = new ArrayList<FormItem>();


        boolean odd = true;
        for (PropertyDefinition property : definition.getPropertiesInGroup(group.getName())) {
            addItems(fields, property, odd);
            odd = !odd;
        }

        form.setFields(fields.toArray(new FormItem[fields.size()]));

        section.addItem(form);
        return section;
    }


    public void addItems(ArrayList<FormItem> fields, PropertyDefinition propertyDefinition, boolean oddRow) {
        Property property = configuration.get(propertyDefinition.getName());

        StaticTextItem nameItem = new StaticTextItem();
        nameItem.setValue("<b>" + propertyDefinition.getDisplayName() + "</b>");
        nameItem.setShowTitle(false);
        nameItem.setCellStyle(oddRow ? "OddRow" : "EvenRow");

        fields.add(nameItem);

        FormItem valueItem = null;
        if (propertyDefinition instanceof PropertyDefinitionSimple) {
            PropertyDefinitionSimple defSimple = (PropertyDefinitionSimple) propertyDefinition;
            PropertySimple propertySimple = (PropertySimple) property;


            boolean isUnset = property == null || propertySimple.getStringValue() == null;


            CheckboxItem unsetItem = new CheckboxItem();
            unsetItem.setValue(isUnset);
            unsetItem.setShowLabel(false);
            unsetItem.setShowTitle(false);
            unsetItem.setLabelAsTitle(false);
            unsetItem.setColSpan(1);
            unsetItem.setCellStyle(oddRow ? "OddRow" : "EvenRow");
            fields.add(unsetItem);


            List<PropertyDefinitionEnumeration> enumeratedValues = defSimple.getEnumeratedValues();
            if (enumeratedValues != null && enumeratedValues.size() > 0) {

                LinkedHashMap<String, String> valueOptions = new LinkedHashMap<String, String>();
                for (PropertyDefinitionEnumeration option : defSimple.getEnumeratedValues()) {
                    valueOptions.put(option.getName(), option.getValue());
                }

                if (valueOptions.size() > 5) {
                    valueItem = new SelectItem();
                } else {
                    valueItem = new RadioGroupItem();
                }
                valueItem.setValueMap(valueOptions);
                if (property != null) {
                    valueItem.setValue(propertySimple.getStringValue());
                }
            } else {
                switch (defSimple.getType()) {

                    case STRING:
                        valueItem = new TextItem();
                        valueItem.setValue(property == null ? "" : propertySimple.getStringValue());
                        break;
                    case LONG_STRING:
                        break;
                    case PASSWORD:
                        valueItem = new PasswordItem();
                        valueItem.setValue(property == null ? "" : propertySimple.getStringValue());
                        break;
                    case BOOLEAN:
                        valueItem = new RadioGroupItem();
                        ((RadioGroupItem) valueItem).setValueMap("Yes", "No");
                        valueItem.setValue(property == null || propertySimple.getStringValue() == null ? false : propertySimple.getBooleanValue());
                        break;
                    case INTEGER:
                    case LONG:
                        valueItem = new IntegerItem();
                        if (property != null && propertySimple.getStringValue() != null)
                            valueItem.setValue(propertySimple.getLongValue());
                        break;
                    case FLOAT:
                    case DOUBLE:
                        valueItem = new FloatItem();
                        valueItem.setValue(property == null || propertySimple.getStringValue() == null ? 0 : propertySimple.getDoubleValue());
                        break;
                    case FILE:
                        valueItem = new FileItem();
                        break;
                    case DIRECTORY:
                        break;
                }
            }


            /* if (defSimple.getConstraints() != null) {
                Set<Constraint> constraints = defSimple.getConstraints();
                for (Constraint c : constraints) {
                    if (c instanceof )
                }
            }*/


            valueItem.setShowTitle(false);
            valueItem.setDisabled(isUnset);
            valueItem.setWidth(220);
            valueItem.setCellStyle(oddRow ? "OddRow" : "EvenRow");


            final FormItem finalValueItem = valueItem;
            unsetItem.addChangeHandler(new ChangeHandler() {
                public void onChange(ChangeEvent changeEvent) {
                    Boolean isUnset = (Boolean) changeEvent.getValue();
                    if (isUnset) {
                        finalValueItem.setDisabled(true);
                        finalValueItem.setValue((String) null);
                    } else {
                        finalValueItem.setDisabled(false);
                        finalValueItem.focusInItem();
                    }
                }
            });

            fields.add(valueItem);

        }

        StaticTextItem descriptionItem = new StaticTextItem();
        descriptionItem.setValue(propertyDefinition.getDescription());
        descriptionItem.setShowTitle(false);
        descriptionItem.setEndRow(true);
        descriptionItem.setCellStyle(oddRow ? "OddRow" : "EvenRow");
        fields.add(descriptionItem);
    }

}

