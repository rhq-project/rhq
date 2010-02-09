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
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationFormat;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.core.domain.configuration.definition.constraint.Constraint;
import org.rhq.core.domain.configuration.definition.constraint.IntegerRangeConstraint;
import org.rhq.core.domain.configuration.definition.constraint.RegexConstraint;
import org.rhq.enterprise.gui.coregui.client.inventory.configuration.ConfigurationGwtService;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.AnimationEffect;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.TreeModelType;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.AnimationCallback;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.ItemChangedEvent;
import com.smartgwt.client.widgets.form.events.ItemChangedHandler;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FileItem;
import com.smartgwt.client.widgets.form.fields.FloatItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.IntegerItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.validator.IntegerRangeValidator;
import com.smartgwt.client.widgets.form.validator.RegExpValidator;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuButton;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import com.smartgwt.client.widgets.toolbar.ToolStripSeparator;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * @author Greg Hinkle
 */
public class ConfigurationEditor extends VLayout {


    private TabSet tabSet;
    private ToolStrip toolStrip;

    private ConfigurationDefinition definition;
    private Configuration configuration;
    private Configuration originalConfiguration;

    private boolean changed = false;


    private int resourceId;
    private int resourceTypeId;
    private ConfigType configType;
    private IButton saveButton;

    public static enum ConfigType {
        plugin, resource
    }


    public ConfigurationEditor(int resourceId, int resourceTypeId) {
        this(resourceId, resourceTypeId, ConfigType.resource);
    }

    public ConfigurationEditor(int resourceId, int resourceTypeId, ConfigType configType) {
        this.resourceId = resourceId;
        this.resourceTypeId = resourceTypeId;
        this.configType = configType;
        setOverflow(Overflow.AUTO);
    }


    @Override
    protected void onInit() {
        super.onInit();

//        addMember(new Label("<b>Loading...</b>"));
//        this.redraw();


        if (configType == ConfigType.resource) {
            ConfigurationGwtService.App.getInstance().getResourceConfiguration(resourceId, new AsyncCallback<Configuration>() {
                public void onFailure(Throwable caught) {
                }

                public void onSuccess(Configuration result) {
                    configuration = result;
                    reload();
                }
            });

            ConfigurationGwtService.App.getInstance().getResourceConfigurationDefinition(resourceTypeId, new AsyncCallback<ConfigurationDefinition>() {
                public void onFailure(Throwable caught) {
                }

                public void onSuccess(ConfigurationDefinition result) {
                    definition = result;
                    reload();
                }
            });
        } else if (configType == ConfigType.plugin) {
            ConfigurationGwtService.App.getInstance().getPluginConfiguration(resourceId, new AsyncCallback<Configuration>() {
                public void onFailure(Throwable caught) {
                }

                public void onSuccess(Configuration result) {
                    configuration = result;
                    reload();
                }
            });

            ConfigurationGwtService.App.getInstance().getPluginConfigurationDefinition(resourceTypeId, new AsyncCallback<ConfigurationDefinition>() {
                public void onFailure(Throwable caught) {
                }

                public void onSuccess(ConfigurationDefinition result) {
                    definition = result;
                    reload();
                }
            });
        }
    }


    public void reload() {
        if (definition == null || configuration == null) {
            // Wait for both to load
            return;
        }

        originalConfiguration = configuration.deepCopy();


        tabSet = new TabSet();

        if (definition.getConfigurationFormat() == ConfigurationFormat.RAW || definition.getConfigurationFormat() == ConfigurationFormat.STRUCTURED_AND_RAW) {
            System.out.println("Loading files view");
            Tab tab = new Tab("Files");
            tab.setPane(buildRawPane());
            tabSet.addTab(tab);
        }

        if (definition.getConfigurationFormat() == ConfigurationFormat.STRUCTURED || definition.getConfigurationFormat() == ConfigurationFormat.STRUCTURED_AND_RAW) {
            System.out.println("loading properties view");
            Tab tab = new Tab("Properties");
            tab.setPane(buildStructuredPane());
            tabSet.addTab(tab);
        }

        addMember(tabSet);

        this.redraw();
    }

    protected HLayout buildRawPane() {

        HLayout layout = new HLayout();
        final HashMap<String, RawConfiguration> filesMap = new HashMap<String, RawConfiguration>();

        TreeGrid fileTree = new TreeGrid();
        fileTree.setShowResizeBar(true);

        Tree files = new Tree();
        files.setModelType(TreeModelType.CHILDREN);
        TreeNode root = new TreeNode("root");
        TreeNode[] children = new TreeNode[configuration.getRawConfigurations().size()];
        int i = 0;
        for (RawConfiguration rawConfiguration : configuration.getRawConfigurations()) {
            children[i++] = new TreeNode(rawConfiguration.getPath());
            filesMap.put(rawConfiguration.getPath(), rawConfiguration);
        }
        root.setChildren(children);
        files.setRoot(root);
        fileTree.setData(files);
        fileTree.setWidth(250);


        DynamicForm form = new DynamicForm();
        final TextAreaItem rawEditor = new TextAreaItem();
//        rawEditor.setValue("This is a test");
        rawEditor.setShowTitle(false);
        form.setItems(rawEditor);
        form.setHeight100();
        form.setWidth100();

        fileTree.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                String path = selectionEvent.getRecord().getAttribute("name");
                System.out.println("Getting Path: " + path);
                rawEditor.setValue(filesMap.get(path).getContents());
                rawEditor.redraw();
                System.out.println("Data: " + filesMap.get(path).getContents());
            }
        });

        if (children.length > 0) {
            fileTree.selectRecord(children[0]);
            rawEditor.setValue(filesMap.get(children[0].getName()).getContents());
        }

        layout.setMembers(fileTree, form);
        return layout;
    }


    protected VLayout buildStructuredPane() {

        VLayout layout = new VLayout();
        List<PropertyGroupDefinition> definitions = definition.getGroupDefinitions();


        final SectionStack sectionStack = new SectionStack();
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        sectionStack.setWidth100();
        sectionStack.setHeight100();
        sectionStack.setScrollSectionIntoView(true);
        sectionStack.setOverflow(Overflow.AUTO);


        if (definition.getNonGroupedProperties().size() > 0) {
            sectionStack.addSection(buildGroupSection(null));
        }

        for (PropertyGroupDefinition definition : definitions) {
//            System.out.println("building: " + definition.getDisplayName());
            sectionStack.addSection(buildGroupSection(definition));
        }


        Menu menu = new Menu();
        for (SectionStackSection section : sectionStack.getSections()) {
            MenuItem item = new MenuItem(section.getTitle());
            item.addClickHandler(new ClickHandler() {
                public void onClick(MenuItemClickEvent event) {
                    int x = event.getMenu().getItemNum(event.getItem());
                    sectionStack.expandSection(x);
                    sectionStack.showSection(x);
                }
            });
            menu.addItem(item);
        }

        menu.addItem(new MenuItemSeparator());

        MenuItem hideAllItem = new MenuItem("Hide All");
        hideAllItem.addClickHandler(new ClickHandler() {
            public void onClick(MenuItemClickEvent event) {
                for (int i = 0; i < sectionStack.getSections().length; i++) {
                    sectionStack.collapseSection(i);
                }
            }
        });
        menu.addItem(hideAllItem);


        toolStrip = new ToolStrip();
        toolStrip.setBackgroundImage(null);

        toolStrip.setWidth100();

        toolStrip.addMember(new LayoutSpacer());
        saveButton = new IButton("Save");
        saveButton.setAlign(Alignment.CENTER);
        saveButton.setDisabled(true);
        toolStrip.addMember(saveButton);
        toolStrip.addMember(new IButton("Reset"));
        toolStrip.addMember(new LayoutSpacer());
        toolStrip.addMember(new MenuButton("Jump to Section", menu));

        layout.addMember(toolStrip);

        layout.addMember(sectionStack);

        return layout;
    }


    public SectionStackSection buildGroupSection(PropertyGroupDefinition group) {

        SectionStackSection section;
        if (group == null) {
            section = new SectionStackSection("General Properties");

        } else {
            section = new SectionStackSection(group.getDisplayName() +
                    (group.getDescription() != null
                            ? ("<div style='padding-left: 20px; font-weight: normal; font-size: smaller;'>" + group.getDescription() + "</div>")
                            : ""));
            section.setExpanded(!group.isDefaultHidden());
        }

        DynamicForm form = new DynamicForm();
        form.setValidateOnChange(true);

        form.addItemChangedHandler(new ItemChangedHandler() {
            public void onItemChanged(ItemChangedEvent itemChangedEvent) {
                if (!changed) {
                    changed = true;
                    toolStrip.setBackgroundColor("yellow");
                    saveButton.animateHide(AnimationEffect.FADE, new AnimationCallback() {
                        public void execute(boolean b) {
                            saveButton.setDisabled(false);                            
                            saveButton.animateShow(AnimationEffect.FADE, new AnimationCallback() {
                                public void execute(boolean b) {

                                }
                            },1000);
                        }
                    }, 1000);
                }
            }
        });

        form.setNumCols(4);
        form.setCellPadding(5);
        form.setColWidths(190, 28, 210);


        ArrayList<FormItem> fields = new ArrayList<FormItem>();


        boolean odd = true;

        ArrayList<PropertyDefinition> definitions = new ArrayList<PropertyDefinition>(
                ((group == null) ? definition.getNonGroupedProperties() : definition.getPropertiesInGroup(group.getName()))
        );
        Collections.sort(definitions, new PropertyDefinitionComparator());


        for (PropertyDefinition property : definitions) {
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
            valueItem = buildSimpleField(fields, (PropertyDefinitionSimple) propertyDefinition, oddRow, property);
            fields.add(valueItem);


            StaticTextItem descriptionItem = new StaticTextItem();
            descriptionItem.setValue(propertyDefinition.getDescription());
            descriptionItem.setShowTitle(false);
            descriptionItem.setEndRow(true);
            descriptionItem.setCellStyle(oddRow ? "OddRow" : "EvenRow");
            fields.add(descriptionItem);
        } else if (propertyDefinition instanceof PropertyDefinitionList) {
            if (((PropertyDefinitionList) propertyDefinition).getMemberDefinition() instanceof PropertyDefinitionMap) {
                // List of Maps is a specially supported case with summary fields as columns in a table
                buildListOfMapsField(fields, (PropertyDefinitionMap) ((PropertyDefinitionList) propertyDefinition).getMemberDefinition(), oddRow, (PropertyList) property);
            }


        }
    }

    private void buildListOfMapsField(ArrayList<FormItem> fields, PropertyDefinitionMap propertyDefinition, boolean oddRow, PropertyList property) {

        final ListGrid summaryTable = new ListGrid();
        summaryTable.setAlternateRecordStyles(true);
        summaryTable.setShowAllRecords(true);
        summaryTable.setBodyOverflow(Overflow.VISIBLE);
        summaryTable.setOverflow(Overflow.VISIBLE);

        ArrayList<ListGridField> fieldsList = new ArrayList<ListGridField>();
        ArrayList<PropertyDefinition> definitions = new ArrayList<PropertyDefinition>(propertyDefinition.getPropertyDefinitions().values());
        Collections.sort(definitions, new PropertyDefinitionComparator());

        for (PropertyDefinition subDef : definitions) {
            if (subDef.isSummary()) {
                ListGridField field = new ListGridField(subDef.getName(), subDef.getDisplayName());
                fieldsList.add(field);
            }
        }


        if (fieldsList.size() == 0) {
            // An extra "feature of the config system". If no fields are labeled summary, all are considered summary.
            for (PropertyDefinition subDef : definitions) {
                ListGridField field = new ListGridField(subDef.getName(), subDef.getDisplayName());
                fieldsList.add(field);
            }
        }

        ListGridField editField = new ListGridField("edit", 20);
        editField.setType(ListGridFieldType.ICON);
//        editField.setIcon(Window.getImgURL("[SKIN]/actions/edit.png"));
        editField.setCellIcon(Window.getImgURL("[SKIN]/actions/edit.png"));
        editField.setCanEdit(false);
        editField.setCanGroupBy(false);
        editField.setCanSort(false);
        editField.setCanHide(false);
        editField.addRecordClickHandler(new RecordClickHandler() {
            public void onRecordClick(RecordClickEvent recordClickEvent) {
                System.out.println("You want to edit: " + recordClickEvent.getRecord());
            }
        });
        fieldsList.add(editField);


        ListGridField removeField = new ListGridField("remove", 20);
        removeField.setType(ListGridFieldType.ICON);
//        removeField.setIcon(Window.getImgURL("[SKIN]/actions/remove.png")); //"/images/tbb_delete.gif");
        removeField.setCellIcon(Window.getImgURL("[SKIN]/actions/remove.png")); //"/images/tbb_delete.gif");
        removeField.setCanEdit(false);
        removeField.setCanFilter(true);
        removeField.setFilterEditorType(new SpacerItem());
        removeField.setCanGroupBy(false);
        removeField.setCanSort(false);
        removeField.setCanHide(false);

        removeField.addRecordClickHandler(new RecordClickHandler() {
            public void onRecordClick(final RecordClickEvent recordClickEvent) {
                System.out.println("You want to delete: " + recordClickEvent.getRecordNum());
                SC.confirm("Are you sure you want to delete this row?", new BooleanCallback() {
                    public void execute(Boolean aBoolean) {
                        if (aBoolean) {
                            summaryTable.removeData(recordClickEvent.getRecord());
                        }
                    }
                });
            }
        });

        editField.setEditorType(new ButtonItem("delete", "Delete"));
        fieldsList.add(removeField);


        summaryTable.setFields(fieldsList.toArray(new ListGridField[fieldsList.size()]));


        ListGridRecord[] rows = new ListGridRecord[property.getList().size()];
        int i = 0;
        for (Property row : property.getList()) {
            PropertyMap rowMap = (PropertyMap) row;

            ListGridRecord record = new ListGridRecord();
            for (PropertyDefinition subDef : definitions) {
                PropertyDefinitionSimple subDefSimple = (PropertyDefinitionSimple) subDef;
                record.setAttribute(subDefSimple.getName(), ((PropertySimple) rowMap.get(subDefSimple.getName())).getStringValue());
            }
            rows[i++] = record;
        }
        summaryTable.setData(rows);


        CanvasItem item = new CanvasItem();
        item.setCanvas(summaryTable);
//        item.setHeight(500);
        item.setColSpan(3);
        item.setEndRow(true);
        item.setShowTitle(false);
        fields.add(item);

    }


    private FormItem buildSimpleField(ArrayList<FormItem> fields, PropertyDefinitionSimple propertyDefinition, boolean oddRow, Property property) {
        final PropertySimple propertySimple = (PropertySimple) property;

        FormItem valueItem = null;


        boolean isUnset = property == null || propertySimple.getStringValue() == null;


        final CheckboxItem unsetItem = new CheckboxItem();
        unsetItem.setValue(isUnset);
        unsetItem.setShowLabel(false);
        unsetItem.setShowTitle(false);
        unsetItem.setLabelAsTitle(false);
        unsetItem.setColSpan(1);
        unsetItem.setCellStyle(oddRow ? "OddRow" : "EvenRow");
        fields.add(unsetItem);


        List<PropertyDefinitionEnumeration> enumeratedValues = propertyDefinition.getEnumeratedValues();
        if (enumeratedValues != null && enumeratedValues.size() > 0) {

            LinkedHashMap<String, String> valueOptions = new LinkedHashMap<String, String>();
            for (PropertyDefinitionEnumeration option : propertyDefinition.getEnumeratedValues()) {
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
            switch (propertyDefinition.getType()) {

                case STRING:
                    valueItem = new TextItem();
                    valueItem.setValue(property == null ? "" : propertySimple.getStringValue());
                    break;
                case LONG_STRING:
                    valueItem = new TextAreaItem();
                    valueItem.setValue(property == null ? "" : propertySimple.getStringValue());
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
                    valueItem = new TextItem();
                    break;
            }
        }


        if (propertyDefinition.getConstraints() != null) {
            Set<Constraint> constraints = propertyDefinition.getConstraints();
            for (Constraint c : constraints) {
                if (c instanceof IntegerRangeConstraint) {
                    IntegerRangeConstraint integerConstraint = ((IntegerRangeConstraint) c);
                    IntegerRangeValidator validator = new IntegerRangeValidator();

                    if (integerConstraint.getMinimum() != null)
                        validator.setMin(integerConstraint.getMinimum().intValue());
                    if (integerConstraint.getMaximum() != null)
                        validator.setMax(integerConstraint.getMaximum().intValue());
                    valueItem.setValidators(validator);
                } else if (c instanceof RegexConstraint) {

                    valueItem.setValidators(new RegExpValidator("^" + ((RegexConstraint) c).getDetails() + "$"));
                }
            }
        }

/*
        Click handlers seem to be turned off for disabled fields... need an alternative
        valueItem.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                System.out.println("Click in value field");
                clickEvent.getItem().setDisabled(false);
                unsetItem.setValue(false);

            }
        });
*/


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
                propertySimple.setValue(changeEvent.getValue());
            }
        });

        return valueItem;
    }


    private static class PropertyDefinitionComparator implements Comparator<PropertyDefinition> {
        public int compare(PropertyDefinition o1, PropertyDefinition o2) {
            return new Integer(o1.getOrder()).compareTo(o2.getOrder());
        }
    }


}

