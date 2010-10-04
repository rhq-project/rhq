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
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.WidgetCanvas;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;

import java.util.List;

/**
 * Note: This was a failed attempt at an editor composed with ListGrid components instead of DynamicForm components.
 * I had problems with having different editors active for different rows in the table at the same time. Smart says
 * they're working on enhancing this area... but the DynamicForm might be a better option anyway.
 * @author Greg Hinkle
 */
public class ListGridConfigurationEditor extends VLayout {

    private ConfigurationGWTServiceAsync configurationService = GWTServiceLookup.getConfigurationService();

    private ConfigurationDefinition definition;
    private Configuration configuration;

    public ListGridConfigurationEditor() {


        setOverflow(Overflow.AUTO);


        configurationService.getResourceConfiguration(10005, new AsyncCallback<Configuration>() {
            public void onFailure(Throwable caught) {
                // TODO: Implement this method.
            }

            public void onSuccess(Configuration result) {
                configuration = result;
                com.allen_sauer.gwt.log.client.Log.info("Got config");
                reload();
            }
        });

        configurationService.getResourceConfigurationDefinition(10060, new AsyncCallback<ConfigurationDefinition>() {
            public void onFailure(Throwable caught) {
                // TODO: Implement this method.
            }

            public void onSuccess(ConfigurationDefinition result) {
                definition = result;
                com.allen_sauer.gwt.log.client.Log.info("Got def");
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
        sectionStack.setOverflow(Overflow.VISIBLE);

        for (PropertyGroupDefinition definition : definitions) {
            com.allen_sauer.gwt.log.client.Log.info("building: " + definition.getDisplayName());
            sectionStack.addSection(buildGroupSection(definition));
        }

        addMember(sectionStack);

        this.redraw();
    }


    public SectionStackSection buildGroupSection(PropertyGroupDefinition group) {
        SectionStackSection section = new SectionStackSection(group.getDisplayName() + "<div style='text-align: right; font-size: -2'>" + group.getDescription() + "</div>");
        section.setExpanded(true);


        ListGrid grid = new ListGrid();
        grid.setAlternateRecordStyles(true);
        grid.setSelectionType(SelectionStyle.NONE);
        grid.setEditByCell(false);

        grid.setShowHover(false);
        grid.setShowAllRecords(true);
        grid.setShowAllRecords(true);
        grid.setWrapCells(true);
        grid.setOverflow(Overflow.VISIBLE);
        grid.setBodyOverflow(Overflow.VISIBLE);
        grid.setCanSort(false);

        grid.setFixedRecordHeights(false);
        grid.setAutoFitMaxHeight(50);
        grid.setAutoFitData(Autofit.VERTICAL);

        grid.setAlwaysShowEditors(true);


        ListGridField nameField = new ListGridField("displayName", "Name", 180);
        nameField.setCanEdit(false);
        //nameField

        ListGridField unsetField = new ListGridField("unset", "unset", 25);
        unsetField.setType(ListGridFieldType.BOOLEAN);
        unsetField.setCanEdit(true);

        ListGridField valueField = new ListGridField("value", "Value", 160);
        valueField.setCanEdit(true);
//            valueField.setEditorType(new TextItem());
        valueField.setEditorType(new ConfigEditor());


        final ConfigEditor ce = new ConfigEditor();
        valueField.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {


                return ce.tc.getInnerHTML();
//                "<input type='radio' name='foo' checked='checked'/> alpha<br/>" +
//                 "<input type='radio' name='foo' /> beta<br/>" +
//                 "<input type='radio' name='foo' /> delta<br/>";

            }
        });
        ListGridField descriptionField = new ListGridField("description", "Description");
        descriptionField.setCanEdit(false);
        grid.setFields(nameField, unsetField, valueField, descriptionField);


        for (PropertyDefinition property : definition.getPropertiesInGroup(group.getName())) {
            grid.addData(new PropertyRecord(property));
        }

        section.addItem(grid);
        return section;
    }


    public class ConfigurationListGrid extends ListGrid {

        public ConfigurationListGrid() {
            super(); // TODO: Implement this method.


        }


        // Crap, they didn't implement the override for getEditorType yet

    }


    public class PropertyRecord extends ListGridRecord {

        PropertyDefinition propertyDefinition;

        public PropertyRecord(PropertyDefinition definition) {
            this.propertyDefinition = definition;
            setAttribute("name", definition.getName());
            setAttribute("displayName", definition.getDisplayName());

            setAttribute("required", definition.isRequired());
            setAttribute("readOnly", definition.isReadOnly());

            PropertySimple property = configuration.getSimple(definition.getName());

            setAttribute("unset", property.getStringValue() == null);
            setAttribute("value", property.getStringValue());

            setAttribute("description", definition.getDescription());

        }
    }

    public class ConfigEditor extends CanvasItem {

        int x = 1;
        TestCanvas tc = new TestCanvas();

        public ConfigEditor() {
            super();

            setCanvas(new WidgetCanvas(new Label("testing")));
        }

        @Override
        public Canvas getCanvas() {

            tc.val = "" + x++;

            return tc;
        }


    }

    public class TestCanvas extends Canvas {

        String val;


        @Override
        public String getInnerHTML() {
            return val;
        }
    }

}