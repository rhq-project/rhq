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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.TreeModelType;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.ValuesManager;
import com.smartgwt.client.widgets.form.events.ItemChangedEvent;
import com.smartgwt.client.widgets.form.events.ItemChangedHandler;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
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
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.validator.FloatRangeValidator;
import com.smartgwt.client.widgets.form.validator.IntegerRangeValidator;
import com.smartgwt.client.widgets.form.validator.RegExpValidator;
import com.smartgwt.client.widgets.form.validator.Validator;
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
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.configuration.AbstractPropertyMap;
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
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.configuration.definition.constraint.Constraint;
import org.rhq.core.domain.configuration.definition.constraint.FloatRangeConstraint;
import org.rhq.core.domain.configuration.definition.constraint.IntegerRangeConstraint;
import org.rhq.core.domain.configuration.definition.constraint.RegexConstraint;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.components.table.PropertyGrid;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.CanvasUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIMenuButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableMenu;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableSectionStack;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTab;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTabSet;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableToolStrip;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTreeGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ConfigurationEditor extends LocatableVLayout {

    private ConfigurationGWTServiceAsync configurationService = GWTServiceLookup.getConfigurationService();

    private TabSet tabSet;
    private LocatableToolStrip toolStrip;

    private ConfigurationDefinition definition;
    private Configuration configuration;
    private Configuration originalConfiguration;

    private ValuesManager valuesManager = new ValuesManager();

    private boolean changed = false;

    private Label loadingLabel = new Label("<b>Loading...</b>");

    private int resourceId;
    private int resourceTypeId;
    private ConfigType configType;
    private IButton saveButton;

    private boolean readOnly = false;
    private Set<String> invalidPropertyNames = new HashSet<String>();
    private Set<ValidationStateChangeListener> validationStateChangeListeners =
        new HashSet<ValidationStateChangeListener>();

    public static enum ConfigType {
        plugin, resource
    }

    ; // Need this extra semicolon for the qdox parser

    public ConfigurationEditor(String locatorId) {
        super(locatorId);
    }

    public ConfigurationEditor(String locatorId, int resourceId, int resourceTypeId) {
        this(locatorId, resourceId, resourceTypeId, ConfigType.resource);
    }

    public ConfigurationEditor(String locatorId, int resourceId, int resourceTypeId, ConfigType configType) {
        super(locatorId);
        this.resourceId = resourceId;
        this.resourceTypeId = resourceTypeId;
        this.configType = configType;
        setOverflow(Overflow.AUTO);
    }

    public ConfigurationEditor(String locatorId, ConfigurationDefinition definition, Configuration configuration) {
        super(locatorId);
        this.configuration = configuration;
        this.definition = definition;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void showError(Throwable failure) {
        addMember(new Label(failure.getMessage()));
    }

    public void showError(String message) {
        addMember(new Label(message));
    }

    public boolean validate() {
        return this.valuesManager.validate();
    }

    public boolean isValid() {
        return this.valuesManager.hasErrors();
    }

    public void addValidationStateChangeListener(ValidationStateChangeListener validationStateChangeListener) {
        this.validationStateChangeListeners.add(validationStateChangeListener);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        addMember(loadingLabel);
        this.redraw();

        final long start = System.currentTimeMillis();

        if (definition == null || configuration == null) {

            if (configType == ConfigType.resource) {
                configurationService.getResourceConfiguration(resourceId, new AsyncCallback<Configuration>() {
                    public void onFailure(Throwable caught) {
                        showError(caught);
                    }

                    public void onSuccess(Configuration result) {
                        configuration = result;
                        System.out.println("Config retreived in: " + (System.currentTimeMillis() - start));
                        reload();
                    }
                });

                ResourceTypeRepository.Cache.getInstance().getResourceTypes(new Integer[] { resourceTypeId },
                    EnumSet.of(ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
                    new ResourceTypeRepository.TypesLoadedCallback() {
                        public void onTypesLoaded(Map<Integer, ResourceType> types) {
                            System.out.println("ConfigDef retreived in: " + (System.currentTimeMillis() - start));
                            definition = types.get(resourceTypeId).getResourceConfigurationDefinition();
                            if (definition == null) {
                                loadingLabel.hide();
                                showError("No configuration supported for this resource");
                            }
                            reload();
                        }
                    });

            } else if (configType == ConfigType.plugin) {
                configurationService.getPluginConfiguration(resourceId, new AsyncCallback<Configuration>() {
                    public void onFailure(Throwable caught) {
                        showError(caught);
                    }

                    public void onSuccess(Configuration result) {
                        configuration = result;
                        reload();
                    }
                });

                ResourceTypeRepository.Cache.getInstance().getResourceTypes(new Integer[] { resourceTypeId },
                    EnumSet.of(ResourceTypeRepository.MetadataType.pluginConfigurationDefinition),
                    new ResourceTypeRepository.TypesLoadedCallback() {
                        public void onTypesLoaded(Map<Integer, ResourceType> types) {
                            System.out.println("ConfigDef retreived in: " + (System.currentTimeMillis() - start));
                            definition = types.get(resourceTypeId).getPluginConfigurationDefinition();
                            if (definition == null) {
                                showError("No configuration supported for this resource");
                            }
                            reload();
                        }
                    });
            }
        } else {
            reload();
        }
    }

    public void reload() {
        if (definition == null || configuration == null) {
            // Wait for both to load
            return;
        }

        originalConfiguration = configuration.deepCopy();

        tabSet = new LocatableTabSet(getLocatorId());

        if (definition.getConfigurationFormat() == ConfigurationFormat.RAW
            || definition.getConfigurationFormat() == ConfigurationFormat.STRUCTURED_AND_RAW) {
            System.out.println("Loading files view...");
            Tab tab = new LocatableTab("Files", "Files");
            tab.setPane(buildRawPane());
            tabSet.addTab(tab);
        }

        if (definition.getConfigurationFormat() == ConfigurationFormat.STRUCTURED
            || definition.getConfigurationFormat() == ConfigurationFormat.STRUCTURED_AND_RAW) {
            System.out.println("Loading properties view...");
            Tab tab = new LocatableTab("Properties", "Properties");
            tab.setPane(buildStructuredPane());
            tabSet.addTab(tab);
        }

        for (Canvas c : getChildren()) {
            c.destroy();
        }

        addMember(tabSet);

        this.markForRedraw();
    }

    protected HLayout buildRawPane() {

        LocatableHLayout layout = new LocatableHLayout(extendLocatorId("Raw"));
        final HashMap<String, RawConfiguration> filesMap = new HashMap<String, RawConfiguration>();

        TreeGrid fileTree = new LocatableTreeGrid(layout.extendLocatorId("Files"));
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

        DynamicForm form = new LocatableDynamicForm(layout.extendLocatorId("Editor"));
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

        LocatableVLayout layout = new LocatableVLayout(extendLocatorId("Structured"));
        List<PropertyGroupDefinition> definitions = definition.getGroupDefinitions();

        final SectionStack sectionStack = new LocatableSectionStack(layout.extendLocatorId("Sections"));
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        sectionStack.setWidth100();
        sectionStack.setHeight100();
        sectionStack.setScrollSectionIntoView(true);
        sectionStack.setOverflow(Overflow.AUTO);

        if (definition.getNonGroupedProperties().size() > 0) {
            sectionStack.addSection(buildGroupSection(layout.extendLocatorId("NoGroup"), null));
        }

        for (PropertyGroupDefinition definition : definitions) {
            //            System.out.println("building: " + definition.getDisplayName());
            sectionStack.addSection(buildGroupSection(layout.extendLocatorId(definition.getName()), definition));
        }

        // TODO GH: Save button as saveListener() or remove the buttons from this form and have
        // the container provide them?

        toolStrip = new LocatableToolStrip(layout.extendLocatorId("Tools"));
        toolStrip.setBackgroundImage(null);

        toolStrip.setWidth100();

        toolStrip.addMember(new LayoutSpacer());
        saveButton = new LocatableIButton(toolStrip.extendLocatorId("Save"), "Save");
        saveButton.setAlign(Alignment.CENTER);
        saveButton.setDisabled(true);
        //        toolStrip.addMember(saveButton);

        IButton resetButton = new LocatableIButton(toolStrip.extendLocatorId("Reset"), "Reset");
        resetButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                reload();
            }
        });

        Menu menu = new LocatableMenu(toolStrip.extendLocatorId("JumpMenu"));
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

        //        toolStrip.addMember(resetButton);
        toolStrip.addMember(new LayoutSpacer());
        toolStrip.addMember(new LocatableIMenuButton(toolStrip.extendLocatorId("Jump"), "Jump to Section", menu));

        layout.addMember(toolStrip);

        layout.addMember(sectionStack);

        return layout;
    }

    public SectionStackSection buildGroupSection(String locatorId, PropertyGroupDefinition group) {

        SectionStackSection section;
        if (group == null) {
            section = new SectionStackSection("General Properties");

        } else {
            section = new SectionStackSection(
                "<div style=\"float:left; font-weight: bold;\">"
                    + group.getDisplayName()
                    + "</div>"
                    + (group.getDescription() != null ? ("<div style='padding-left: 10px; font-weight: normal; font-size: smaller; float: left;'>"
                        + " -" + group.getDescription() + "</div>")
                        : ""));
            section.setExpanded(!group.isDefaultHidden());
        }

        ArrayList<PropertyDefinition> definitions = new ArrayList<PropertyDefinition>(((group == null) ? definition
            .getNonGroupedProperties() : definition.getPropertiesInGroup(group.getName())));
        Collections.sort(definitions, new PropertyDefinitionComparator());

        DynamicForm form = buildPropertiesForm(locatorId + "_Props", definitions, configuration);

        section.addItem(form);
        return section;
    }

    private DynamicForm buildPropertiesForm(String locatorId, List<PropertyDefinition> definitions,
        AbstractPropertyMap propertyMap) {
        LocatableDynamicForm form = new LocatableDynamicForm(locatorId);
        form.setValuesManager(valuesManager);
        form.setValidateOnChange(true);

        form.addItemChangedHandler(new ItemChangedHandler() {
            public void onItemChanged(ItemChangedEvent itemChangedEvent) {
                if (!changed) {
                    changed = true;
                    CanvasUtility.blink(saveButton);
                    saveButton.setDisabled(false);
                }
            }
        });

        form.setNumCols(4);
        form.setCellPadding(5);
        form.setColWidths(190, 28, 210);

        ArrayList<FormItem> fields = new ArrayList<FormItem>();

        boolean odd = true;

        for (PropertyDefinition propertyDefinition : definitions) {
            Property property = propertyMap.get(propertyDefinition.getName());
            if (property == null) {
                if (propertyDefinition instanceof PropertyDefinitionSimple) {
                    property = new PropertySimple(propertyDefinition.getName(), null);
                    propertyMap.put(property);
                }
            }
            addItems(locatorId + "_" + propertyDefinition.getName(), fields, propertyDefinition, property, odd);
            odd = !odd;
        }

        form.setFields(fields.toArray(new FormItem[fields.size()]));

        return form;
    }

    public void addItems(String locatorId, ArrayList<FormItem> fields, PropertyDefinition propertyDefinition,
        Property property, boolean oddRow) {

        StaticTextItem nameItem = new StaticTextItem();
        nameItem.setStartRow(true);
        String title = "<b>"
            + (propertyDefinition.getDisplayName() != null ? propertyDefinition.getDisplayName() : propertyDefinition
                .getName()) + "</b>";
        nameItem.setValue(title);
        nameItem.setShowTitle(false);
        nameItem.setCellStyle(oddRow ? "OddRow" : "EvenRow");

        fields.add(nameItem);

        FormItem valueItem;
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
                buildListOfMapsField(locatorId, fields,
                    (PropertyDefinitionMap) ((PropertyDefinitionList) propertyDefinition).getMemberDefinition(),
                    oddRow, (PropertyList) property);
            }
        } else if (propertyDefinition instanceof PropertyDefinitionMap) {
            buildMapsField(fields, (PropertyDefinitionMap) propertyDefinition, (PropertyMap) property);
        }
    }

    private void buildMapsField(ArrayList<FormItem> fields, PropertyDefinitionMap propertyDefinitionMap,
        PropertyMap propertyMap) {
        // create the property grid
        PropertyGrid propertyGrid = new PropertyGrid();
        propertyGrid.getNameField().setName("Name");
        propertyGrid.getValuesField().setName("Value");

        // create the editors
        HashMap<String, FormItem> editorsMap = new HashMap<String, FormItem>();
        TextItem textEditor = new TextItem();
        editorsMap.put("simpleText", textEditor);

        // set the editors and attribute name where to find the record type
        propertyGrid.setEditorsMap("fieldType", editorsMap);

        if (propertyMap != null) {
            ListGridRecord[] records = new ListGridRecord[propertyMap.getMap().size()];
            int i = 0;
            for (Property property : propertyMap.getMap().values()) {
                ListGridRecord record = new ListGridRecord();
                record.setAttribute("Name", property.getName());
                record.setAttribute("Value", ((PropertySimple) property).getStringValue());
                record.setAttribute("fieldType", "simpleText");
                records[i++] = record;
            }
            propertyGrid.setData(records);
        }

        propertyGrid.draw();

        CanvasItem item = new CanvasItem();
        item.setCanvas(propertyGrid);
        //        item.setHeight(500);
        item.setColSpan(3);
        item.setEndRow(true);
        item.setShowTitle(false);
        fields.add(item);

    }

    private void buildListOfMapsField(final String locatorId, ArrayList<FormItem> fields,
        final PropertyDefinitionMap propertyDefinition, boolean oddRow, final PropertyList propertyList) {

        final ListGrid summaryTable = new ListGrid();
        //        summaryTable.setID("config_summaryTable_" + propertyDefinition.getName());
        summaryTable.setAlternateRecordStyles(true);
        summaryTable.setShowAllRecords(true);
        summaryTable.setBodyOverflow(Overflow.VISIBLE);
        summaryTable.setOverflow(Overflow.VISIBLE);
        summaryTable.setAutoFitData(Autofit.HORIZONTAL);

        ArrayList<ListGridField> fieldsList = new ArrayList<ListGridField>();
        ArrayList<PropertyDefinition> definitions = new ArrayList<PropertyDefinition>(propertyDefinition
            .getPropertyDefinitions().values());
        Collections.sort(definitions, new PropertyDefinitionComparator());

        for (PropertyDefinition subDef : definitions) {
            if (subDef.isSummary()) {
                ListGridField field = new ListGridField(subDef.getName(), subDef.getDisplayName());

                PropertyDefinitionSimple defSimple = (PropertyDefinitionSimple) subDef;
                if (defSimple.getType() == PropertySimpleType.INTEGER) {
                    field.setType(ListGridFieldType.INTEGER);
                } else if (defSimple.getType() == PropertySimpleType.FLOAT) {
                    field.setType(ListGridFieldType.FLOAT);
                }

                fieldsList.add(field);
            }
        }

        if (fieldsList.size() == 0) {
            // An extra "feature of the config system". If no fields are labeled summary, all are considered summary.
            for (PropertyDefinition subDef : definitions) {
                ListGridField field = new ListGridField(subDef.getName(), subDef.getDisplayName());
                fieldsList.add(field);
                PropertyDefinitionSimple defSimple = (PropertyDefinitionSimple) subDef;
                if (defSimple.getType() == PropertySimpleType.INTEGER) {
                    field.setType(ListGridFieldType.FLOAT);
                } else if (defSimple.getType() == PropertySimpleType.FLOAT) {
                    field.setType(ListGridFieldType.FLOAT);
                }

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
                displayMapEditor(locatorId + "_MapEdit", summaryTable, recordClickEvent.getRecord(),
                    propertyDefinition, propertyList, (PropertyMap) recordClickEvent.getRecord().getAttributeAsObject(
                        "_RHQ_PROPERTY"));
            }
        });
        fieldsList.add(editField);

        if (!readOnly) {
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
        }

        summaryTable.setFields(fieldsList.toArray(new ListGridField[fieldsList.size()]));

        ListGridRecord[] rows = buildSummaryRecords(propertyList, definitions);
        summaryTable.setData(rows);

        VLayout summaryTableHolder = new LocatableVLayout(locatorId);

        ToolStrip toolStrip = new ToolStrip();
        toolStrip.setWidth100();
        IButton addRowButton = new IButton();
        addRowButton.setIcon(Window.getImgURL("[SKIN]/actions/add.png"));
        addRowButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                displayMapEditor(locatorId + "_MapEdit", summaryTable, null, propertyDefinition, propertyList, null);
            }
        });

        toolStrip.addMember(addRowButton);

        summaryTableHolder.setMembers(summaryTable, toolStrip);

        CanvasItem item = new CanvasItem();
        item.setCanvas(summaryTableHolder);
        //        item.setHeight(500);
        item.setColSpan(3);
        item.setEndRow(true);
        item.setShowTitle(false);
        fields.add(item);
    }

    private ListGridRecord[] buildSummaryRecords(PropertyList propertyList, ArrayList<PropertyDefinition> definitions) {
        ListGridRecord[] rows = new ListGridRecord[propertyList == null ? 0 : propertyList.getList().size()];
        int i = 0;
        for (Property row : propertyList.getList()) {
            PropertyMap rowMap = (PropertyMap) row;

            ListGridRecord record = buildSummaryRecord(definitions, rowMap);
            rows[i++] = record;
        }
        return rows;
    }

    private ListGridRecord buildSummaryRecord(List<PropertyDefinition> definitions, PropertyMap rowMap) {
        ListGridRecord record = new ListGridRecord();
        for (PropertyDefinition subDef : definitions) {
            PropertyDefinitionSimple subDefSimple = (PropertyDefinitionSimple) subDef;
            PropertySimple propertySimple = ((PropertySimple) rowMap.get(subDefSimple.getName()));

            if (propertySimple.getStringValue() != null) {
                record.setAttribute(subDefSimple.getName(), propertySimple.getStringValue());
                /*
                switch (((PropertyDefinitionSimple) subDef).getType()) {
                    case BOOLEAN:
                        record.setAttribute(subDefSimple.getName(), propertySimple.getBooleanValue());
                        break;
                    case INTEGER:
                        record.setAttribute(subDefSimple.getName(), propertySimple.getLongValue());
                        break;
                    case LONG:
                        record.setAttribute(subDefSimple.getName(), propertySimple.getLongValue());
                        break;
                    case FLOAT:
                        record.setAttribute(subDefSimple.getName(), propertySimple.getDoubleValue());
                        break;
                    case DOUBLE:
                        record.setAttribute(subDefSimple.getName(), propertySimple.getDoubleValue());
                        break;
                    default:
                        record.setAttribute(subDefSimple.getName(), propertySimple.getStringValue());
                        break;
                }*/
            } else {

            }
        }
        record.setAttribute("_RHQ_PROPERTY", rowMap);
        return record;
    }

    private FormItem buildSimpleField(ArrayList<FormItem> fields, PropertyDefinitionSimple propertyDefinition,
        boolean oddRow, Property property) {
        final PropertySimple propertySimple = (PropertySimple) property;

        FormItem valueItem = null;

        boolean isUnset = (property == null || propertySimple.getStringValue() == null)
            && propertyDefinition.isRequired() == false;

        CheckboxItem unsetItem = null;
        if (!propertyDefinition.isRequired()) {
            unsetItem = new CheckboxItem();
            unsetItem.setValue(isUnset);
            unsetItem.setDisabled(readOnly);
            unsetItem.setShowLabel(false);
            unsetItem.setShowTitle(false);
            unsetItem.setLabelAsTitle(false);
            unsetItem.setColSpan(1);
            unsetItem.setCellStyle(oddRow ? "OddRow" : "EvenRow");
            fields.add(unsetItem);
        } else {
            SpacerItem spacer = new SpacerItem();
            spacer.setCellStyle(oddRow ? "OddRow" : "EvenRow");
            fields.add(spacer);
        }

        List<PropertyDefinitionEnumeration> enumeratedValues = propertyDefinition.getEnumeratedValues();
        if (enumeratedValues != null && !enumeratedValues.isEmpty()) {

            LinkedHashMap<String, String> valueOptions = new LinkedHashMap<String, String>();
            for (PropertyDefinitionEnumeration option : propertyDefinition.getEnumeratedValues()) {
                valueOptions.put(option.getValue(), option.getName());
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
            case FILE:
            case DIRECTORY:
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
                LinkedHashMap<String, String> valMap = new LinkedHashMap<String, String>();
                valMap.put("true", "Yes");
                valMap.put("false", "No");
                valueItem.setValueMap(valMap);
                valueItem.setValue(property == null || propertySimple.getStringValue() == null ? false : propertySimple
                    .getBooleanValue());
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
                valueItem.setValue(property == null || propertySimple.getStringValue() == null ? 0 : propertySimple
                    .getDoubleValue());
                break;
            }
        }

        valueItem.setRequired(propertyDefinition.isRequired());

        List<Validator> validators = buildValidators(propertyDefinition, valueItem);
        valueItem.setValidators(validators.toArray(new Validator[validators.size()]));

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
        valueItem.setDisabled(isUnset || readOnly);
        valueItem.setWidth(220);
        valueItem.setCellStyle(oddRow ? "OddRow" : "EvenRow");

        final FormItem finalValueItem = valueItem;

        finalValueItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent changedEvent) {
                boolean wasValidBefore = ConfigurationEditor.this.invalidPropertyNames.isEmpty();
                if (changedEvent.getItem().validate()) {
                    ConfigurationEditor.this.invalidPropertyNames.remove(propertySimple.getName());
                    propertySimple.setValue(changedEvent.getValue());                    
                } else {
                    ConfigurationEditor.this.invalidPropertyNames.add(propertySimple.getName());
                }
                boolean isValidNow = ConfigurationEditor.this.invalidPropertyNames.isEmpty();
                if (isValidNow != wasValidBefore) {
                    for (ValidationStateChangeListener validationStateChangeListener : ConfigurationEditor.this.validationStateChangeListeners) {
                        validationStateChangeListener.validateStateChanged(isValidNow);
                    }
                }
            }
        });

        if (unsetItem != null) {
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
                    propertySimple.setValue(finalValueItem.getValue());
                }
            });
        }

        return valueItem;
    }

    private List<Validator> buildValidators(PropertyDefinitionSimple propertyDefinition, FormItem valueItem) {
        List<Validator> validators = new ArrayList<Validator>();
        if (propertyDefinition.getConstraints() != null) {
            Set<Constraint> constraints = propertyDefinition.getConstraints();

            for (Constraint constraint : constraints) {
                if (constraint instanceof IntegerRangeConstraint) {
                    IntegerRangeConstraint integerConstraint = ((IntegerRangeConstraint) constraint);
                    IntegerRangeValidator validator = new IntegerRangeValidator();
                    if (integerConstraint.getMinimum() != null) {
                        validator.setMin(integerConstraint.getMinimum().intValue());
                    }
                    if (integerConstraint.getMaximum() != null) {
                        validator.setMax(integerConstraint.getMaximum().intValue());
                    }
                    validators.add(validator);
                } else if (constraint instanceof FloatRangeConstraint) {
                    FloatRangeConstraint floatConstraint = ((FloatRangeConstraint) constraint);
                    FloatRangeValidator validator = new FloatRangeValidator();
                    if (floatConstraint.getMinimum() != null) {
                        validator.setMin(floatConstraint.getMinimum().floatValue());
                    }
                    if (floatConstraint.getMaximum() != null) {
                        validator.setMax(floatConstraint.getMaximum().floatValue());
                    }
                    validators.add(validator);
                } else if (constraint instanceof RegexConstraint) {
                    RegExpValidator validator =
                        new RegExpValidator("^" + ((RegexConstraint) constraint).getDetails() + "$");
                    validators.add(validator);
                }
            }
        }
        return validators;
    }

    private void displayMapEditor(String locatorId, final ListGrid summaryTable, final Record existingRecord,
        PropertyDefinitionMap definition, final PropertyList list, PropertyMap map) {

        final List<PropertyDefinition> definitions = new ArrayList<PropertyDefinition>(definition
            .getPropertyDefinitions().values());

        Collections.sort(definitions, new PropertyDefinitionComparator());

        final boolean newRow = map == null;
        if (newRow)
            map = new PropertyMap(definition.getName());

        final PropertyMap finalMap = map;
        final PropertyMap copy = finalMap.deepCopy(true);

        LocatableVLayout layout = new LocatableVLayout(locatorId);
        layout.setHeight100();

        DynamicForm childForm = buildPropertiesForm(locatorId + "_Child", definitions, finalMap);
        childForm.setHeight100();
        layout.addMember(childForm);

        final Window popup = new Window();
        popup.setTitle("Edit Configuration Row");
        popup.setWidth(800);
        popup.setHeight(600);
        popup.setIsModal(true);
        popup.setShowModalMask(true);
        popup.setShowCloseButton(false);
        popup.centerInPage();

        final IButton saveButton = new IButton("Save");
        //        saveButton.setID("config_structured_button_save");
        saveButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(ClickEvent clickEvent) {

                if (newRow) {
                    list.add(finalMap);
                    ListGridRecord record = buildSummaryRecord(definitions, finalMap);

                    System.out.println("here");
                    try {
                        summaryTable.addData(record);
                        System.out.println("there");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    for (PropertyDefinition subDef : definitions) {
                        PropertyDefinitionSimple subDefSimple = (PropertyDefinitionSimple) subDef;
                        PropertySimple propertySimple = ((PropertySimple) finalMap.get(subDefSimple.getName()));
                        existingRecord.setAttribute(subDefSimple.getName(), propertySimple != null ? propertySimple
                            .getStringValue() : null);
                    }
                    summaryTable.updateData(existingRecord);
                }
                summaryTable.redraw();

                //                ListGridRecord[] rows = buildSummaryRecords(list, definitions);
                //                summaryTable.setData(rows);
                //                summaryTable.redraw();
                //                summaryTable.addData();
                popup.destroy();

            }
        });

        final IButton cancelButton = new IButton("Cancel");
        cancelButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (!newRow) {
                    list.getList().set(list.getList().indexOf(finalMap), copy);
                }
                popup.destroy();
            }
        });

        HLayout buttons = new HLayout();
        buttons.setAlign(Alignment.CENTER);
        buttons.setMembersMargin(10);
        buttons.setMembers(saveButton, cancelButton);
        layout.addMember(buttons);

        popup.addItem(layout);

        popup.show();

    }

    private static class PropertyDefinitionComparator implements Comparator<PropertyDefinition> {
        public int compare(PropertyDefinition o1, PropertyDefinition o2) {
            return new Integer(o1.getOrder()).compareTo(o2.getOrder());
        }
    }
}
