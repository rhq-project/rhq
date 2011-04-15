/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.AutoFitWidthApproach;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.MultipleAppearance;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.util.ValueCallback;
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
import com.smartgwt.client.widgets.form.fields.events.BlurEvent;
import com.smartgwt.client.widgets.form.fields.events.BlurHandler;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.TitleHoverEvent;
import com.smartgwt.client.widgets.form.fields.events.TitleHoverHandler;
import com.smartgwt.client.widgets.form.validator.CustomValidator;
import com.smartgwt.client.widgets.form.validator.FloatRangeValidator;
import com.smartgwt.client.widgets.form.validator.IntegerRangeValidator;
import com.smartgwt.client.widgets.form.validator.IsFloatValidator;
import com.smartgwt.client.widgets.form.validator.IsIntegerValidator;
import com.smartgwt.client.widgets.form.validator.LengthRangeValidator;
import com.smartgwt.client.widgets.form.validator.RegExpValidator;
import com.smartgwt.client.widgets.form.validator.Validator;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.configuration.AbstractPropertyMap;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
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
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIMenuButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableListGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableMenu;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableSectionStack;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableToolStrip;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWindow;

/**
 * A SmartGWT widget for editing an RHQ {@link Configuration} that conforms to a {@link ConfigurationDefinition}.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
//
// Note: There was a failed attempt at an editor composed with ListGrid components instead of DynamicForm components,
// but there were problems with having different editors active for different rows in the table at the same time.
// Smart says they're working on enhancing this area, but the DynamicForm might be a better option anyway. (ghinkle)
//
public class ConfigurationEditor extends LocatableVLayout {

    private static final LinkedHashMap<String, String> BOOLEAN_PROPERTY_ITEM_VALUE_MAP =
            new LinkedHashMap<String, String>();
    static {
        BOOLEAN_PROPERTY_ITEM_VALUE_MAP.put(Boolean.TRUE.toString(), MSG.common_val_yes());
        BOOLEAN_PROPERTY_ITEM_VALUE_MAP.put(Boolean.FALSE.toString(), MSG.common_val_no());
    }

    private ConfigurationGWTServiceAsync configurationService = GWTServiceLookup.getConfigurationService();

    private LocatableToolStrip toolStrip;

    private ConfigurationDefinition configurationDefinition;
    private Configuration configuration;
    private Configuration originalConfiguration;

    private ValuesManager topLevelPropertiesValuesManager = new ValuesManager();

    private Label loadingLabel = new Label("<b>" + MSG.common_msg_loading() + "</b>");

    private int resourceId;
    private int resourceTypeId;
    private ConfigType configType;

    private String editorTitle = null;
    private boolean readOnly = false;
    private Map<String, String> invalidPropertyNameToDisplayNameMap = new HashMap<String, String>();
    private Set<PropertyValueChangeListener> propertyValueChangeListeners = new HashSet<PropertyValueChangeListener>();

    public static enum ConfigType {
        plugin, resource
    }; // Need this extra semicolon for the qdox parser

    /**
     * This is the kind of handler that is called when this editor has loaded
     * the configuration and configuration definition. If the load failed,
     * the methods will still be called, but will be passed null.
     */
    public static interface LoadHandler {
        void loadedConfiguration(Configuration config);

        void loadedConfigurationDefinition(ConfigurationDefinition configDef);
    }

    private LoadHandler loadHandler = null;

    private ConfigurationEditor(String locatorId) {
        super(locatorId);
    }

    public ConfigurationEditor(String locatorId, int resourceId, int resourceTypeId, ConfigType configType) {
        this(locatorId);

        this.resourceId = resourceId;
        this.resourceTypeId = resourceTypeId;
        this.configType = configType;
    }

    public ConfigurationEditor(String locatorId, ConfigurationDefinition configurationDefinition,
        Configuration configuration) {
        this(locatorId);

        if (configuration == null) {
            throw new IllegalArgumentException("Null configuration.");
        }
        if (configurationDefinition == null) {
            throw new IllegalArgumentException("Null configurationDefinition.");
        }

        this.configuration = configuration;
        this.configurationDefinition = configurationDefinition;
    }

    public void setLoadHandler(LoadHandler handler) {
        this.loadHandler = handler;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public ConfigurationDefinition getConfigurationDefinition() {
        return configurationDefinition;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getEditorTitle() {
        return editorTitle;
    }

    public void setEditorTitle(String title) {
        this.editorTitle = title;
    }

    public void showError(Throwable failure) {
        addMember(new Label(failure.getMessage()));
    }

    public void showError(String message) {
        addMember(new Label(message));
    }

    public boolean validate() {
        return (this.topLevelPropertiesValuesManager.validate());
    }

    public boolean isValid() {
        return (!this.topLevelPropertiesValuesManager.hasErrors());
    }

    public void addPropertyValueChangeListener(PropertyValueChangeListener propertyValueChangeListener) {
        this.propertyValueChangeListeners.add(propertyValueChangeListener);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        addMember(loadingLabel);
        this.redraw();

        final long start = System.currentTimeMillis();

        if (configurationDefinition == null || configuration == null) {

            if (configType == ConfigType.resource) {
                configurationService.getResourceConfiguration(resourceId, new AsyncCallback<Configuration>() {
                    public void onFailure(Throwable caught) {
                        showError(caught);
                        if (loadHandler != null) {
                            loadHandler.loadedConfiguration(null);
                        }
                    }

                    public void onSuccess(Configuration result) {
                        configuration = result;
                        Log.info("Config retrieved in: " + (System.currentTimeMillis() - start));
                        reload();
                        if (loadHandler != null) {
                            loadHandler.loadedConfiguration(configuration);
                        }
                    }
                });

                ResourceTypeRepository.Cache.getInstance().getResourceTypes(new Integer[] { resourceTypeId },
                    EnumSet.of(ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
                    new ResourceTypeRepository.TypesLoadedCallback() {
                        public void onTypesLoaded(Map<Integer, ResourceType> types) {
                            com.allen_sauer.gwt.log.client.Log.debug("ConfigDef retrieved in: "
                                + (System.currentTimeMillis() - start));
                            configurationDefinition = types.get(resourceTypeId).getResourceConfigurationDefinition();
                            if (configurationDefinition == null) {
                                loadingLabel.hide();
                                showError(MSG.view_configEdit_error_1());
                            }
                            reload();
                            if (loadHandler != null) {
                                loadHandler.loadedConfigurationDefinition(configurationDefinition);
                            }
                        }
                    });

            } else if (configType == ConfigType.plugin) {
                configurationService.getPluginConfiguration(resourceId, new AsyncCallback<Configuration>() {
                    public void onFailure(Throwable caught) {
                        showError(caught);
                        if (loadHandler != null) {
                            loadHandler.loadedConfiguration(null);
                        }
                    }

                    public void onSuccess(Configuration result) {
                        configuration = result;
                        reload();
                        if (loadHandler != null) {
                            loadHandler.loadedConfiguration(configuration);
                        }
                    }
                });

                ResourceTypeRepository.Cache.getInstance().getResourceTypes(new Integer[] { resourceTypeId },
                    EnumSet.of(ResourceTypeRepository.MetadataType.pluginConfigurationDefinition),
                    new ResourceTypeRepository.TypesLoadedCallback() {
                        public void onTypesLoaded(Map<Integer, ResourceType> types) {
                            Log.debug("ConfigDef retrieved in: " + (System.currentTimeMillis() - start));
                            configurationDefinition = types.get(resourceTypeId).getPluginConfigurationDefinition();
                            if (configurationDefinition == null) {
                                showError(MSG.view_configEdit_error_2());
                            }
                            reload();
                            if (loadHandler != null) {
                                loadHandler.loadedConfigurationDefinition(configurationDefinition);
                            }
                        }
                    });
            }
        } else {
            reload();
        }
    }

    public void reload() {
        if (this.configurationDefinition == null || this.configuration == null) {
            // Wait for both to load.
            return;
        }

        if (this.originalConfiguration == null) {
            this.originalConfiguration = configuration.deepCopy();
        }

        for (Canvas childCanvas : getChildren()) {
            childCanvas.destroy();
        }

        if (configurationDefinition.getConfigurationFormat() == ConfigurationFormat.STRUCTURED
            || configurationDefinition.getConfigurationFormat() == ConfigurationFormat.STRUCTURED_AND_RAW) {
            Log.info("Building structured configuration editor...");
            LocatableVLayout structuredConfigLayout = buildStructuredPane();
            addMember(structuredConfigLayout);
        } else {
            Label label = new Label("Structured configuration is not supported.");
            addMember(label);
        }

        this.markForRedraw();
    }

    public void reset() {
        this.configuration = this.originalConfiguration;
        reload();
    }

    protected LocatableVLayout buildStructuredPane() {
        LocatableVLayout layout = new LocatableVLayout(extendLocatorId("Structured"));
        List<PropertyGroupDefinition> groupDefinitions = configurationDefinition.getGroupDefinitions();

        if (groupDefinitions.isEmpty()) {
            // No prop groups, so we just need a single form for the non-grouped props.
            List<PropertyDefinition> propertyDefinitions = new ArrayList<PropertyDefinition>(configurationDefinition
                .getNonGroupedProperties());

            DynamicForm form = buildPropertiesForm(layout.extendLocatorId("Props"), propertyDefinitions, configuration);
            form.setBorder("1px solid #AAA");
            form.validate();
            layout.addMember(form);
        } else {
            // One or more prop groups, so create a section stack with one section per group.
            final SectionStack sectionStack = new LocatableSectionStack(layout.extendLocatorId("Sections"));
            sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
            sectionStack.setWidth100();
            sectionStack.setHeight100();
            sectionStack.setScrollSectionIntoView(true);
            sectionStack.setOverflow(Overflow.AUTO);

            if (!configurationDefinition.getNonGroupedProperties().isEmpty()) {
                sectionStack.addSection(buildGroupSection(layout.extendLocatorId("NoGroup"), null));
            }

            for (PropertyGroupDefinition definition : groupDefinitions) {
                //            com.allen_sauer.gwt.log.client.Log.info("building: " + definition.getDisplayName());
                sectionStack.addSection(buildGroupSection(layout.extendLocatorId(definition.getName()), definition));
            }

            this.toolStrip = buildToolStrip(layout, sectionStack);
            layout.addMember(toolStrip);
            layout.addMember(sectionStack);
        }

        fireInitialPropertyChangedEvent();

        return layout;
    }

    private void fireInitialPropertyChangedEvent() {
        Map validationErrors = this.topLevelPropertiesValuesManager.getErrors();
        if (validationErrors != null) {
            for (Object key : validationErrors.keySet()) {
                String propertyName = (String) key;
                PropertyDefinition propertyDefinition = this.configurationDefinition.get(propertyName);
                this.invalidPropertyNameToDisplayNameMap.put(propertyName, propertyDefinition.getDisplayName());
            }
            if (!this.invalidPropertyNameToDisplayNameMap.isEmpty()) {
                PropertyValueChangeEvent event = new PropertyValueChangeEvent(null, null, true,
                        this.invalidPropertyNameToDisplayNameMap);
                firePropertyChangedEvent(event);
            }
        }
    }

    private LocatableToolStrip buildToolStrip(LocatableVLayout layout, final SectionStack sectionStack) {
        LocatableToolStrip toolStrip = new LocatableToolStrip(layout.extendLocatorId("Tools"));
        toolStrip.setBackgroundImage(null);
        toolStrip.setWidth100();
        toolStrip.setMembersMargin(3);
        toolStrip.setPadding(3);

        if (getEditorTitle() != null) {
            Label titleLabel = new Label(getEditorTitle());
            titleLabel.setWrap(false);
            toolStrip.addMember(titleLabel);
        }

        Menu menu = new LocatableMenu(toolStrip.extendLocatorId("JumpMenu"));
        for (SectionStackSection section : sectionStack.getSections()) {
            MenuItem item = new MenuItem(section.getTitle());
            item.setAttribute("name", section.getName());
            item.addClickHandler(new ClickHandler() {
                public void onClick(MenuItemClickEvent event) {
                    int index = event.getMenu().getItemNum(event.getItem());
                    sectionStack.expandSection(index);
                    sectionStack.showSection(index);
                }
            });
            menu.addItem(item);
        }
        menu.addItem(new MenuItemSeparator());

        MenuItem hideAllItem = new MenuItem(MSG.view_configEdit_hideAll());
        hideAllItem.addClickHandler(new ClickHandler() {
            public void onClick(MenuItemClickEvent event) {
                for (int i = 0; i < sectionStack.getSections().length; i++) {
                    sectionStack.collapseSection(i);
                }
            }
        });
        menu.addItem(hideAllItem);

        toolStrip.addMember(new LocatableIMenuButton(toolStrip.extendLocatorId("Jump"), MSG
            .view_configEdit_jumpToSection(), menu));

        return toolStrip;
    }

    public SectionStackSection buildGroupSection(String locatorId, PropertyGroupDefinition group) {
        SectionStackSection section;
        if (group == null) {
            section = new SectionStackSection(MSG.common_title_generalProp());
            section.setExpanded(true);
        } else {
            String title = "<div style=\"float:left; font-weight: bold;\">"
                    + group.getDisplayName()
                    + "</div>"
                    + (group.getDescription() != null ? ("<div style='padding-left: 10px; font-weight: normal; font-size: smaller; float: left;'>"
                    + " - " + group.getDescription() + "</div>")
                    : "");
            section = new SectionStackSection(title);
            section.setName(group.getName());
            section.setExpanded(!group.isDefaultHidden());
        }

        List<PropertyDefinition> propertyDefinitions = new ArrayList<PropertyDefinition>(
            ((group == null) ? configurationDefinition.getNonGroupedProperties() : configurationDefinition
                .getPropertiesInGroup(group.getName())));

        DynamicForm form = buildPropertiesForm(locatorId, propertyDefinitions, configuration);

        // If the group contains any invalid properties, expand the section so the user can see the error icons next to
        // the invalid properties.
        if (!form.validate()) {
            section.setExpanded(true);
        }

        section.addItem(form);
        return section;
    }

    protected LocatableDynamicForm buildPropertiesForm(String locatorId, Collection<PropertyDefinition> propertyDefinitions,
        AbstractPropertyMap propertyMap) {

        LocatableDynamicForm form = new LocatableDynamicForm(locatorId);
        if (propertyMap instanceof Configuration) {
            this.topLevelPropertiesValuesManager.addMember(form);
        }
        //form.setValidateOnExit(true);  // TODO: Remove this?
        form.setHiliteRequiredFields(true);
        form.setNumCols(4);
        form.setCellPadding(5);
        form.setColWidths(190, 28, 210);

        List<FormItem> fields = new ArrayList<FormItem>();
        addHeaderItems(fields);
        addItemsForPropertiesRecursively(locatorId, propertyDefinitions, propertyMap, fields);
        form.setFields(fields.toArray(new FormItem[fields.size()]));

        return form;
    }

    private void addHeaderItems(List<FormItem> fields) {
        final String CELL_STYLE = "configurationEditorHeaderCell";

        // TODO: i18n
        StaticTextItem nameHeader = new StaticTextItem();
        nameHeader.setValue("Property");
        nameHeader.setShowTitle(false);
        nameHeader.setCellStyle(CELL_STYLE);
        fields.add(nameHeader);

        StaticTextItem unsetHeader = new StaticTextItem();
        unsetHeader.setValue("Unset?");
        unsetHeader.setShowTitle(false);
        unsetHeader.setCellStyle(CELL_STYLE);
        fields.add(unsetHeader);

        StaticTextItem valueHeader = new StaticTextItem();
        valueHeader.setValue("Value");
        valueHeader.setShowTitle(false);
        valueHeader.setCellStyle(CELL_STYLE);
        fields.add(valueHeader);

        StaticTextItem descriptionHeader = new StaticTextItem();
        descriptionHeader.setValue("Description");
        descriptionHeader.setShowTitle(false);
        descriptionHeader.setCellStyle(CELL_STYLE);
        fields.add(descriptionHeader);
    }

    private void addItemsForPropertiesRecursively(String locatorId, Collection<PropertyDefinition> propertyDefinitions,
        AbstractPropertyMap propertyMap, List<FormItem> fields) {
        boolean odd = true;
        List<PropertyDefinition> sortedPropertyDefinitions = new ArrayList<PropertyDefinition>(propertyDefinitions);
        Collections.sort(sortedPropertyDefinitions, new PropertyDefinitionComparator());
        for (PropertyDefinition propertyDefinition : sortedPropertyDefinitions) {
            Property property = propertyMap.get(propertyDefinition.getName());
            if (property == null) {
                if (propertyDefinition instanceof PropertyDefinitionSimple) {
                    property = new PropertySimple(propertyDefinition.getName(), null);
                    propertyMap.put(property);
                }
            }
            addItemsForPropertyRecursively(locatorId + "_" + propertyDefinition.getName(), propertyDefinition,
                property, odd, fields);
            odd = !odd;
        }
        return;
    }

    public void addItemsForPropertyRecursively(String locatorId, PropertyDefinition propertyDefinition,
        Property property, boolean oddRow, List<FormItem> fields) {
        List<FormItem> fieldsForThisProperty;

        if (propertyDefinition instanceof PropertyDefinitionSimple) {
            final PropertyDefinitionSimple propertyDefinitionSimple = (PropertyDefinitionSimple) propertyDefinition;
            PropertySimple propertySimple = (PropertySimple) property;

            if (propertySimple == null) {
                propertySimple = new PropertySimple(propertyDefinitionSimple.getName(), null);
            }

            fieldsForThisProperty = buildFieldsForPropertySimple(propertyDefinition, propertyDefinitionSimple,
                propertySimple);
        } else if (propertyDefinition instanceof PropertyDefinitionList) {
            PropertyDefinitionList propertyDefinitionList = (PropertyDefinitionList) propertyDefinition;
            PropertyDefinition memberDefinition = propertyDefinitionList.getMemberDefinition();
            PropertyList propertyList = (PropertyList) property;
            if (propertyList == null) {
                propertyList = new PropertyList(propertyDefinitionList.getName());
            }
            fieldsForThisProperty = buildFieldsForPropertyList(locatorId, propertyDefinition, oddRow,
                propertyDefinitionList, memberDefinition, propertyList);
        } else if (propertyDefinition instanceof PropertyDefinitionMap) {
            PropertyDefinitionMap propertyDefinitionMap = (PropertyDefinitionMap) propertyDefinition;
            PropertyMap propertyMap = (PropertyMap) property;
            if (propertyMap == null) {
                propertyMap = new PropertyMap(propertyDefinitionMap.getName());
            }

            fieldsForThisProperty = buildFieldsForPropertyMap(locatorId, propertyDefinitionMap, propertyMap);
        } else {
            throw new IllegalStateException("Property definition null or of unknown type: " + propertyDefinition);
        }

        // Add the fields for this property to the master fields list and set the row background color.
        for (FormItem field : fieldsForThisProperty) {
            fields.add(field);
            field.setCellStyle(oddRow ? "OddRow" : "EvenRow");
        }
    }

    protected List<FormItem> buildFieldsForPropertySimple(PropertyDefinition propertyDefinition,
        PropertyDefinitionSimple propertyDefinitionSimple, PropertySimple propertySimple) {
        List<FormItem> fields = new ArrayList<FormItem>();

        StaticTextItem nameItem = buildNameItem(propertyDefinition);
        fields.add(nameItem);

        FormItem valueItem;
        valueItem = buildSimpleField(propertyDefinitionSimple, propertySimple);

        FormItem unsetItem = buildUnsetItem(propertyDefinitionSimple, propertySimple, valueItem);
        fields.add(unsetItem);

        fields.add(valueItem);

        StaticTextItem descriptionItem = buildDescriptionField(propertyDefinition);
        fields.add(descriptionItem);

        return fields;
    }

    protected List<FormItem> buildFieldsForPropertyList(String locatorId, PropertyDefinition propertyDefinition,
        boolean oddRow, PropertyDefinitionList propertyDefinitionList, PropertyDefinition memberDefinition,
        PropertyList propertyList) {
        List<FormItem> fields = new ArrayList<FormItem>();

        StaticTextItem nameItem = buildNameItem(propertyDefinition);
        fields.add(nameItem);

        if (memberDefinition instanceof PropertyDefinitionMap) {
            // List of Maps is a specially supported case with summary fields as columns in a table
            // Note: This field spans 3 columns. Since the description column is supplanted, show
            // the description as a tooltip when the user hovers over the property name.
            nameItem.setTooltip(propertyDefinition.getDescription());
            PropertyDefinitionMap memberDefinitionMap = (PropertyDefinitionMap) memberDefinition;
            CanvasItem listOfMapsItem = buildListOfMapsField(locatorId, memberDefinitionMap, propertyDefinitionList,
                propertyList);
            fields.add(listOfMapsItem);
        } else if (memberDefinition instanceof PropertyDefinitionSimple) {
            SpacerItem unsetItem = new SpacerItem();
            fields.add(unsetItem);

            CanvasItem listOfSimplesItem = buildListOfSimplesField(locatorId, propertyDefinitionList, propertyList);
            fields.add(listOfSimplesItem);

            StaticTextItem descriptionItem = buildDescriptionField(propertyDefinition);
            fields.add(descriptionItem);
        } else {
            Log.error("List " + propertyList + " has unsupported member type: " + memberDefinition);
            Canvas canvas = new Canvas();
            // TODO: Add label with error message to canvas.
            CanvasItem canvasItem = buildComplexPropertyField(canvas);
            canvasItem.setColSpan(3);
            canvasItem.setEndRow(true);
            fields.add(canvasItem);
        }

        return fields;
    }

    protected List<FormItem> buildFieldsForPropertyMap(String locatorId, PropertyDefinitionMap propertyDefinitionMap,
        PropertyMap propertyMap) {
        List<FormItem> fields = new ArrayList<FormItem>();

        StaticTextItem nameItem = buildNameItem(propertyDefinitionMap);
        fields.add(nameItem);

        // Note: This field spans 3 columns.
        FormItem mapField = buildMapField(locatorId, propertyDefinitionMap, propertyMap);
        fields.add(mapField);

        return fields;
    }

    protected StaticTextItem buildNameItem(PropertyDefinition propertyDefinition) {
        StaticTextItem nameItem = new StaticTextItem();
        nameItem.setStartRow(true);
        String title = "<b>"
            + (propertyDefinition.getDisplayName() != null ? propertyDefinition.getDisplayName() : propertyDefinition
                .getName()) + "</b>";
        nameItem.setValue(title);
        nameItem.setShowTitle(false);
        return nameItem;
    }

    private StaticTextItem buildDescriptionField(PropertyDefinition propertyDefinition) {
        StaticTextItem descriptionItem = new StaticTextItem();
        descriptionItem.setValue(propertyDefinition.getDescription());
        descriptionItem.setShowTitle(false);
        descriptionItem.setEndRow(true);
        return descriptionItem;
    }

    protected void firePropertyChangedEvent(Property property, PropertyDefinition propertyDefinition, boolean isValid) {
        PropertyDefinition topLevelPropertyDefinition = getTopLevelPropertyDefinition(propertyDefinition);
        boolean invalidPropertySetChanged;
        if (isValid) {
            invalidPropertySetChanged =
                    (this.invalidPropertyNameToDisplayNameMap.remove(topLevelPropertyDefinition.getName()) != null);
        } else {
            invalidPropertySetChanged =
                    (this.invalidPropertyNameToDisplayNameMap.put(topLevelPropertyDefinition.getName(),
                    topLevelPropertyDefinition.getDisplayName()) != null);
        }

        PropertyValueChangeEvent event = new PropertyValueChangeEvent(property, propertyDefinition,
                invalidPropertySetChanged, this.invalidPropertyNameToDisplayNameMap);
        firePropertyChangedEvent(event);
    }

    private void firePropertyChangedEvent(PropertyValueChangeEvent event) {
        for (PropertyValueChangeListener propertyValueChangeListener : this.propertyValueChangeListeners) {
            propertyValueChangeListener.propertyValueChanged(event);
        }
    }

    public Map<String, String> getInvalidPropertyNames() {
        return this.invalidPropertyNameToDisplayNameMap;
    }

    private FormItem buildMapField(String parentLocatorId, PropertyDefinitionMap propertyDefinitionMap,
        final PropertyMap propertyMap) {
        boolean isDynamic = isDynamic(propertyDefinitionMap);
        if (isDynamic) {
            PropertyDefinitionMap propertyDefinitionMapClone = new PropertyDefinitionMap(propertyDefinitionMap
                .getName(), propertyDefinitionMap.getDescription(), propertyDefinitionMap.isRequired());
            propertyDefinitionMapClone.setConfigurationDefinition(propertyDefinitionMap.getConfigurationDefinition());
            propertyDefinitionMapClone.setReadOnly(propertyDefinitionMap.isReadOnly());
            addMemberPropertyDefinitionsToDynamicPropertyMap(propertyDefinitionMapClone, propertyMap);
            propertyDefinitionMap = propertyDefinitionMapClone;
        }
        LocatableVLayout layout = new LocatableVLayout(parentLocatorId + "_Layout");

        final PropertyDefinitionMap propertyDefinitionMapFinal = propertyDefinitionMap;
        LocatableDynamicForm valuesCanvas = buildPropertiesForm(layout.getLocatorId(),
                propertyDefinitionMapFinal.getPropertyDefinitions().values(), propertyMap);
        layout.addMember(valuesCanvas);

        if (isDynamic && !isReadOnly(propertyDefinitionMap, propertyMap)) {
            // Map is not read-only - add footer with New and Delete buttons to allow user to add or remove members.
            LocatableToolStrip buttonBar = new LocatableToolStrip(layout.extendLocatorId("ButtonBar"));
            buttonBar.setPadding(5);
            buttonBar.setWidth100();
            buttonBar.setMembersMargin(15);
            layout.addMember(buttonBar);

            final IButton newButton = new LocatableIButton(buttonBar.extendLocatorId("New"), MSG
                .common_button_new());
            newButton.setIcon(Window.getImgURL("[SKIN]/actions/add.png"));
            newButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    SC.askforValue(MSG.view_configEdit_enterPropName(), new ValueCallback() {
                        public void execute(String propertyName) {
                            if (propertyMap.get(propertyName) != null) {
                                CoreGUI.getMessageCenter().notify(
                                    new Message(MSG.view_configEdit_error_3(propertyName), Message.Severity.Error,
                                        EnumSet.of(Message.Option.Transient)));
                            } else {
                                PropertySimple memberPropertySimple = new PropertySimple(propertyName, null);
                                addPropertyToDynamicMap(memberPropertySimple, propertyMap);
                                firePropertyChangedEvent(propertyMap, propertyDefinitionMapFinal, true);

                                reload();

                                CoreGUI.getMessageCenter().notify(
                                    new Message(MSG.view_configEdit_msg_1(propertyName), EnumSet
                                        .of(Message.Option.Transient)));
                            }
                        }
                    });
                }
            });
            buttonBar.addMember(newButton);

            HLayout spacer = new HLayout();
            spacer.setWidth(12);
            buttonBar.addMember(spacer);

            LocatableHLayout deleteControlsLayout = new LocatableHLayout(buttonBar.extendLocatorId("DeleteControls"));
            deleteControlsLayout.setMargin(3);
            deleteControlsLayout.setMembersMargin(3);
            LocatableDynamicForm deleteForm = new LocatableDynamicForm(deleteControlsLayout.extendLocatorId("Form"));
            deleteForm.setNumCols(3);
            deleteControlsLayout.addMember(deleteForm);
            buttonBar.addMember(deleteControlsLayout);

            final SelectItem selectItem = new SelectItem();
            selectItem.setValueMap(propertyDefinitionMap.getPropertyDefinitions().keySet().toArray(
                new String[propertyDefinitionMap.getPropertyDefinitions().size()]));
            selectItem.setMultiple(true);
            selectItem.setMultipleAppearance(MultipleAppearance.GRID);
            selectItem.setTitle(MSG.common_button_delete());

            final LocatableIButton okButton = new LocatableIButton(buttonBar.extendLocatorId("OK"));
            okButton.setTitle(MSG.common_button_ok());
            okButton.setDisabled(true);
            okButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    SC.confirm(MSG.view_configEdit_confirm_1(), new BooleanCallback() {
                        @Override
                        public void execute(Boolean confirmed) {
                            if (confirmed) {
                                Object value = selectItem.getValue();
                                if (value != null) {
                                    String stringValue = value.toString();
                                    String[] memberPropertyNames = stringValue.split(",");
                                    for (final String memberPropertyName : memberPropertyNames) {
                                        PropertySimple memberPropertySimple = propertyMap.getSimple(memberPropertyName);
                                        removePropertyFromDynamicMap(memberPropertySimple);
                                        firePropertyChangedEvent(propertyMap, propertyDefinitionMapFinal, true);
                                    }
                                }

                                reload();
                                CoreGUI.getMessageCenter().notify(
                                    new Message(MSG.view_configEdit_msg_2(), EnumSet.of(Message.Option.Transient)));
                            }
                        }
                    });
                }
            });
            deleteControlsLayout.addMember(okButton);

            selectItem.addChangedHandler(new ChangedHandler() {
                @Override
                public void onChanged(ChangedEvent changedEvent) {
                    Object value = changedEvent.getValue();
                    if (value != null) {
                        String stringValue = value.toString();
                        String[] memberPropertyNames = stringValue.split(",");
                        okButton.setDisabled(memberPropertyNames.length == 0);
                    }
                }
            });

            deleteForm.setFields(selectItem);
        }

        CanvasItem canvasItem = buildComplexPropertyField(layout);
        canvasItem.setColSpan(3);
        canvasItem.setEndRow(true);

        return canvasItem;
    }

    protected void addPropertyToDynamicMap(PropertySimple memberPropertySimple, PropertyMap propertyMap) {
        memberPropertySimple.setOverride(true);
        propertyMap.put(memberPropertySimple);
    }

    protected void removePropertyFromDynamicMap(PropertySimple propertySimple) {
        PropertyMap parentMap = propertySimple.getParentMap();
        parentMap.getMap().remove(propertySimple.getName());
    }

    private static boolean isDynamic(PropertyDefinitionMap propertyDefinitionMap) {
        Map<String, PropertyDefinition> memberPropertyDefinitions = propertyDefinitionMap.getPropertyDefinitions();
        return memberPropertyDefinitions == null || memberPropertyDefinitions.isEmpty();
    }

    private void addMemberPropertyDefinitionsToDynamicPropertyMap(PropertyDefinitionMap propertyDefinitionMap,
        PropertyMap propertyMap) {
        for (String propertyName : propertyMap.getMap().keySet()) {
            PropertySimple memberPropertySimple = propertyMap.getSimple(propertyName);
            if (memberPropertySimple != null) {
                PropertyDefinitionSimple memberPropertyDefinitionSimple = new PropertyDefinitionSimple(propertyName,
                    null, false, PropertySimpleType.STRING);
                propertyDefinitionMap.put(memberPropertyDefinitionSimple);
            }
        }
    }

    private CanvasItem buildComplexPropertyField(Canvas canvas) {
        CanvasItem canvasItem = new CanvasItem();
        canvasItem.setCanvas(canvas);
        canvasItem.setShowTitle(false);
        return canvasItem;
    }

    private CanvasItem buildListOfMapsField(final String locatorId,
        final PropertyDefinitionMap memberPropertyDefinitionMap, final PropertyDefinitionList propertyDefinitionList,
        final PropertyList propertyList) {
        Log.debug("Building list-of-maps grid for " + propertyList + "...");

        final LocatableListGrid summaryTable = new LocatableListGrid(locatorId);
        summaryTable.setAlternateRecordStyles(true);
        summaryTable.setShowAllRecords(true);
        summaryTable.setBodyOverflow(Overflow.VISIBLE);
        summaryTable.setOverflow(Overflow.VISIBLE);
        summaryTable.setWidth100();
        summaryTable.setAutoFitFieldWidths(true);
        summaryTable.setAutoFitWidthApproach(AutoFitWidthApproach.BOTH);
        summaryTable.setRecordEnabledProperty(null);

        List<ListGridField> fieldsList = new ArrayList<ListGridField>();
        final List<PropertyDefinition> propertyDefinitions = new ArrayList<PropertyDefinition>(memberPropertyDefinitionMap
            .getPropertyDefinitions().values());
        Collections.sort(propertyDefinitions, new PropertyDefinitionComparator());

        List<PropertyDefinition> summaryPropertyDefinitions = new ArrayList<PropertyDefinition>();
        for (PropertyDefinition subDef : propertyDefinitions) {
            if (subDef.isSummary()) {
                summaryPropertyDefinitions.add(subDef);
            }
        }
        if (summaryPropertyDefinitions.isEmpty()) {
            // An extra "feature of the config system". If no fields are labeled summary, all are considered summary.
            summaryPropertyDefinitions.addAll(propertyDefinitions);
        }

        for (PropertyDefinition summaryPropDef : summaryPropertyDefinitions) {
            ListGridField field = createListGridField(summaryPropDef);
            fieldsList.add(field);
        }

        boolean allSubDefsReadOnly = isAllReadOnly(propertyDefinitions);

        ListGridField editField = new ListGridField("edit", 20);
        editField.setType(ListGridFieldType.ICON);
        final boolean mapReadOnly = this.readOnly || allSubDefsReadOnly;
        String icon = (mapReadOnly) ? ImageManager.getViewIcon() : ImageManager.getEditIcon();
        editField.setCellIcon(Window.getImgURL(icon));
        editField.setCanEdit(false);
        editField.setCanGroupBy(false);
        editField.setCanSort(false);
        editField.setCanHide(false);
        editField.addRecordClickHandler(new RecordClickHandler() {
            public void onRecordClick(RecordClickEvent recordClickEvent) {
                PropertyMapListGridRecord record = (PropertyMapListGridRecord) recordClickEvent.getRecord();
                PropertyMap memberPropertyMap = (PropertyMap) record.getPropertyMap();
                Log.debug("Editing property map: " + memberPropertyMap);
                displayMapEditor(summaryTable, record, propertyDefinitionList, propertyList,
                        memberPropertyDefinitionMap, memberPropertyMap, mapReadOnly);
            }
        });
        fieldsList.add(editField);

        if (!(readOnly || propertyDefinitionList.isReadOnly())) {
            ListGridField removeField = new ListGridField("remove", 20);
            removeField.setType(ListGridFieldType.ICON);
            removeField.setCellIcon(Window.getImgURL(ImageManager.getRemoveIcon()));
            removeField.setCanEdit(false);
            removeField.setCanFilter(true);
            removeField.setFilterEditorType(new SpacerItem());
            removeField.setCanGroupBy(false);
            removeField.setCanSort(false);
            removeField.setCanHide(false);

            removeField.addRecordClickHandler(new RecordClickHandler() {
                public void onRecordClick(final RecordClickEvent recordClickEvent) {
                    Log.info("You want to delete: " + recordClickEvent.getRecordNum());
                    SC.confirm(MSG.view_configEdit_confirm_2(), new BooleanCallback() {
                        public void execute(Boolean confirmed) {
                            if (confirmed) {
                                PropertyMapListGridRecord recordToBeDeleted = (PropertyMapListGridRecord) recordClickEvent.getRecord();
                                propertyList.getList().remove(recordToBeDeleted.getIndex());
                                ListGridRecord[] rows = buildSummaryRecords(propertyList, propertyDefinitions);
                                summaryTable.setData(rows);
                                firePropertyChangedEvent(propertyList, propertyDefinitionList, true);
                            }
                        }
                    });
                }
            });

            editField.setEditorType(new ButtonItem("delete", MSG.common_button_delete()));
            fieldsList.add(removeField);
        }

        summaryTable.setFields(fieldsList.toArray(new ListGridField[fieldsList.size()]));

        // Now add rows containing the actual data (i.e. member property values).
        ListGridRecord[] rows = buildSummaryRecords(propertyList, propertyDefinitions);
        summaryTable.setData(rows);

        VLayout summaryTableHolder = new LocatableVLayout(locatorId);

        ToolStrip toolStrip = new ToolStrip();
        toolStrip.setWidth100();
        if (!(readOnly || propertyDefinitionList.isReadOnly())) {
            IButton addRowButton = new IButton();
            addRowButton.setIcon(Window.getImgURL("[SKIN]/actions/add.png"));
            addRowButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    displayMapEditor(summaryTable, null, propertyDefinitionList, propertyList,
                            memberPropertyDefinitionMap, null, mapReadOnly);
                }
            });
            toolStrip.addMember(addRowButton);
        }

        summaryTableHolder.setMembers(summaryTable, toolStrip);

        CanvasItem canvasItem = buildComplexPropertyField(summaryTableHolder);
        canvasItem.setColSpan(3);
        canvasItem.setEndRow(true);

        return canvasItem;
    }

    private ListGridField createListGridField(PropertyDefinition summaryPropDef) {
        ListGridField field = new ListGridField(summaryPropDef.getName(), summaryPropDef.getDisplayName(), 90);
        PropertyDefinitionSimple defSimple = (PropertyDefinitionSimple) summaryPropDef;
        PropertySimpleType propSimpleType = defSimple.getType();
        switch (propSimpleType) {
            case BOOLEAN:
                field.setType(ListGridFieldType.BOOLEAN);
                break;
            case INTEGER:
                field.setType(ListGridFieldType.INTEGER);
                break;
            case FLOAT:
            case DOUBLE:
                field.setType(ListGridFieldType.FLOAT);
                break;
            default:
                field.setType(ListGridFieldType.TEXT);
        }
        return field;
    }

    private static boolean isAllReadOnly(List<PropertyDefinition> propertyDefinitions) {
        boolean allPropsDefsReadOnly = true;
        for (PropertyDefinition subDef : propertyDefinitions) {
            if (!subDef.isReadOnly()) {
                allPropsDefsReadOnly = false;
                break;
            }
        }
        return allPropsDefsReadOnly;
    }

    private PropertyMapListGridRecord[] buildSummaryRecords(PropertyList propertyList, List<PropertyDefinition> definitions) {
        PropertyMapListGridRecord[] records = new PropertyMapListGridRecord[propertyList == null ? 0 : propertyList.getList().size()];
        List<Property> list = propertyList.getList();
        for (int index = 0, listSize = list.size(); index < listSize; index++) {
            Property row = list.get(index);
            PropertyMap rowMap = (PropertyMap) row;
            PropertyMapListGridRecord record = new PropertyMapListGridRecord(rowMap, index, definitions);
            records[index] = record;
        }
        return records;
    }

    private CanvasItem buildListOfSimplesField(String locatorId, final PropertyDefinitionList propertyDefinitionList,
        final PropertyList propertyList) {
        Log.debug("Building list-of-simples field for " + propertyList + "...");

        LocatableVLayout vLayout = new LocatableVLayout(locatorId);

        final DynamicForm listGrid = new DynamicForm();
        vLayout.addMember(listGrid);

        // TODO (ips, 03/14/11): Make this a ListGrid, rather than a SelectItem.
        final SelectItem membersItem = new SelectItem(propertyList.getName());
        membersItem.setShowTitle(false);
        membersItem.setMultiple(true);
        membersItem.setMultipleAppearance(MultipleAppearance.GRID);
        membersItem.setWidth(220);
        membersItem.setHeight(60);
        LinkedHashMap<String, String> memberValueToIndexMap = buildValueMap(propertyList);
        membersItem.setValueMap(memberValueToIndexMap);
        listGrid.setItems(membersItem);

        if (!isReadOnly(propertyDefinitionList, propertyList)) {
            // List is not read-only - add footer with New and Delete buttons to allow user to add or remove members.
            ToolStrip footer = new ToolStrip();
            footer.setPadding(5);
            footer.setWidth100();
            footer.setMembersMargin(15);
            vLayout.addMember(footer);

            final IButton deleteButton = new LocatableIButton(extendLocatorId("Delete"));
            deleteButton.setIcon(Window.getImgURL("[SKIN]/actions/remove.png"));
            deleteButton.setTooltip(MSG.view_configEdit_tooltip_1());
            deleteButton.setDisabled(true);
            deleteButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    final String[] selectedValues = membersItem.getValues();
                    final String noun = (selectedValues.length == 1) ? MSG.common_label_item() : MSG
                        .common_label_items();
                    String message = MSG.view_configEdit_confirm_3(Integer.toString(selectedValues.length), noun);
                    SC.ask(message, new BooleanCallback() {
                        public void execute(Boolean confirmed) {
                            if (confirmed) {
                                for (int i = selectedValues.length - 1; i >= 0; i--) {
                                    String selectedValue = selectedValues[i];
                                    int index = Integer.valueOf(selectedValue);
                                    propertyList.getList().remove(index);

                                    // Rebuild the select item options.
                                    LinkedHashMap<String, String> memberValueToIndexMap = buildValueMap(propertyList);
                                    membersItem.setValueMap(memberValueToIndexMap);

                                    deleteButton.disable();

                                    firePropertyChangedEvent(propertyList, propertyDefinitionList, true);
                                    CoreGUI.getMessageCenter().notify(
                                        new Message(MSG.view_configEdit_msg_3(Integer.toString(selectedValues.length),
                                            noun), EnumSet.of(Message.Option.Transient)));
                                }
                            }
                        }
                    });
                }
            });
            footer.addMember(deleteButton);

            membersItem.addChangedHandler(new ChangedHandler() {
                @Override
                public void onChanged(ChangedEvent changedEvent) {
                    String[] selectedValues = membersItem.getValues();
                    int count = selectedValues.length;
                    deleteButton.setDisabled(count < 1);
                }
            });

            final IButton newButton = new LocatableIButton(vLayout.extendLocatorId("New"), MSG.common_button_new());
            newButton.setIcon(Window.getImgURL("[SKIN]/actions/add.png"));
            newButton.setTooltip(MSG.view_configEdit_tooltip_2());
            newButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    // TODO: selenium locators
                    final Window popup = new Window();
                    popup.setTitle(MSG.view_configEdit_addItem());
                    popup.setWidth(300);
                    popup.setHeight(110);
                    popup.setIsModal(true);
                    popup.setShowModalMask(true);
                    popup.setShowCloseButton(false);
                    popup.centerInPage();

                    VLayout vLayout = new VLayout();
                    vLayout.setMargin(10);

                    final DynamicForm form = new DynamicForm();

                    PropertyDefinitionSimple memberPropertyDefinitionSimple = (PropertyDefinitionSimple) propertyDefinitionList
                        .getMemberDefinition();
                    final String propertyName = memberPropertyDefinitionSimple.getName();
                    final PropertySimple newMemberPropertySimple = new PropertySimple(propertyName, null);

                    FormItem simpleField = buildSimpleField(memberPropertyDefinitionSimple, newMemberPropertySimple);
                    simpleField.setTitle(memberPropertyDefinitionSimple.getDisplayName());
                    simpleField.setShowTitle(true);
                    simpleField.setAlign(Alignment.CENTER);
                    simpleField.setDisabled(false);
                    simpleField.setRequired(true);
                    simpleField.setEndRow(true);

                    SpacerItem spacer = new SpacerItem();
                    spacer.setHeight(9);

                    form.setItems(simpleField, spacer);
                    vLayout.addMember(form);

                    final IButton okButton = new LocatableIButton(extendLocatorId("OK"), MSG.common_button_ok());
                    okButton.disable();
                    okButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
                        public void onClick(ClickEvent clickEvent) {
                            propertyList.add(newMemberPropertySimple);

                            // Rebuild the select item options.
                            LinkedHashMap<String, String> memberValueToIndexMap = buildValueMap(propertyList);
                            membersItem.setValueMap(memberValueToIndexMap);

                            firePropertyChangedEvent(propertyList, propertyDefinitionList, true);
                            CoreGUI.getMessageCenter().notify(
                                new Message(MSG.view_configEdit_msg_4(), EnumSet.of(Message.Option.Transient)));

                            popup.destroy();
                        }
                    });

                    form.addItemChangedHandler(new ItemChangedHandler() {
                        public void onItemChanged(ItemChangedEvent itemChangedEvent) {
                            newMemberPropertySimple.setStringValue((String) itemChangedEvent.getNewValue());

                            // Only enable the OK button, allowing the user to add the property to the map, if the
                            // property is valid.
                            boolean isValid = form.validate();
                            okButton.setDisabled(!isValid);
                        }
                    });

                    final IButton cancelButton = new LocatableIButton(extendLocatorId("Cancel"), MSG
                        .common_button_cancel());
                    cancelButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
                        public void onClick(ClickEvent clickEvent) {
                            popup.destroy();
                        }
                    });

                    HLayout buttons = new HLayout();
                    buttons.setAlign(Alignment.CENTER);
                    buttons.setTop(10);
                    buttons.setMembersMargin(10);
                    buttons.setMembers(okButton, cancelButton);
                    vLayout.addMember(buttons);

                    popup.addItem(vLayout);
                    popup.show();

                    simpleField.focusInItem();
                }
            });
            footer.addMember(newButton);
        }

        return buildComplexPropertyField(vLayout);
    }

    private LinkedHashMap<String, String> buildValueMap(PropertyList propertyList) {
        LinkedHashMap<String, String> memberValueToIndexMap = new LinkedHashMap<String, String>();
        List<Property> memberProperties = propertyList.getList();
        int index = 0;
        for (Iterator<Property> iterator = memberProperties.iterator(); iterator.hasNext();) {
            Property memberProperty = iterator.next();
            PropertySimple memberPropertySimple = (PropertySimple) memberProperty;
            String memberValue = memberPropertySimple.getStringValue();
            if (memberValue == null) {
                Log.error("List " + propertyList + " contains property with null value - removing and skipping...");
                iterator.remove();
                continue;
            }
            memberValueToIndexMap.put(String.valueOf(index++), memberValue);
        }
        return memberValueToIndexMap;
    }

    protected FormItem buildSimpleField(final PropertyDefinitionSimple propertyDefinitionSimple,
        final PropertySimple propertySimple) {
        Log.debug("Building simple field for " + propertySimple + "...");

        FormItem valueItem = null;

        boolean propertyIsReadOnly = isReadOnly(propertyDefinitionSimple, propertySimple);
        // TODO (ips, 03/25/11): We eventually want to use StaticTextItems for read-only PASSWORD props too, but we have
        //                       to wait until we implement masking/unmasking of PASSWORD props at the SLSB layer first.
        if (propertyIsReadOnly && propertyDefinitionSimple.getType() != PropertySimpleType.PASSWORD) {
            valueItem = new StaticTextItem();
        } else {
            List<PropertyDefinitionEnumeration> enumeratedValues = propertyDefinitionSimple.getEnumeratedValues();
            if (enumeratedValues != null && !enumeratedValues.isEmpty()) {
                LinkedHashMap<String, String> valueOptions = new LinkedHashMap<String, String>();
                for (PropertyDefinitionEnumeration option : propertyDefinitionSimple.getEnumeratedValues()) {
                    valueOptions.put(option.getValue(), option.getName());
                }

                if (valueOptions.size() > 5) {
                    valueItem = new SelectItem();
                } else {
                    valueItem = new RadioGroupItem();
                }
                valueItem.setValueMap(valueOptions);
                if (propertySimple != null) {
                    valueItem.setValue(propertySimple.getStringValue());
                }
            } else {
                switch (propertyDefinitionSimple.getType()) {
                case STRING:
                case FILE:
                case DIRECTORY:
                    valueItem = new TextItem();
                    break;
                case LONG_STRING:
                    valueItem = new TextAreaItem();
                    break;
                case PASSWORD:
                    valueItem = new PasswordItem();
                    break;
                case BOOLEAN:
                    RadioGroupItem radioGroupItem = new RadioGroupItem();
                    radioGroupItem.setVertical(false);
                    radioGroupItem.setValueMap(BOOLEAN_PROPERTY_ITEM_VALUE_MAP);
                    valueItem = radioGroupItem;
                    break;
                case INTEGER:
                case LONG:
                    valueItem = new IntegerItem();
                    break;
                case FLOAT:
                case DOUBLE:
                    valueItem = new FloatItem();
                    break;
                }
            }

            valueItem.setDisabled(propertyIsReadOnly || isUnset(propertyDefinitionSimple, propertySimple));

            List<Validator> validators = buildValidators(propertyDefinitionSimple, propertySimple);
            valueItem.setValidators(validators.toArray(new Validator[validators.size()]));

            if ((propertySimple.getConfiguration() != null) || (propertySimple.getParentMap() != null) ||
                    (propertySimple.getParentList() != null)) {
                valueItem.addChangedHandler(new ChangedHandler() {
                    public void onChanged(ChangedEvent changedEvent) {
                        updatePropertySimpleValue(changedEvent.getValue(), propertySimple, propertyDefinitionSimple);
                        // Only fire a prop value change event if the prop's a top-level simple or a simple within a
                        // top-level map.
                        if (fireEventOnPropertyValueChange(propertyDefinitionSimple, propertySimple)) {
                            boolean isValid = changedEvent.getItem().validate();
                            firePropertyChangedEvent(propertySimple, propertyDefinitionSimple, isValid);
                        }
                    }
                });
            }
        }

        // For more robust and repeatable item locators (not positional), assign a name and a title.
        valueItem.setName(propertySimple.getName());
        valueItem.setTitle("none");
        valueItem.setShowTitle(false);

        String value = propertySimple.getStringValue();
        if (valueItem instanceof StaticTextItem) {
            // Property values are user-editable, so escape HTML when displayed as static text, to prevent XSS attacks.
            value = StringUtility.escapeHtml(value);
        }
        valueItem.setValue(value);

        valueItem.setRequired(propertyDefinitionSimple.isRequired());
        valueItem.setWidth(220);

        /*
                TODO: Click handlers seem to be turned off for disabled fields... need to find an alternative.
                valueItem.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
                    public void onClick(ClickEvent clickEvent) {
                        com.allen_sauer.gwt.log.client.Log.info("Click in value field");
                        clickEvent.getItem().setDisabled(false);
                        unsetItem.setValue(false);

                    }
                });
        */

        return valueItem;
    }

    protected boolean fireEventOnPropertyValueChange(PropertyDefinitionSimple propertyDefinitionSimple,
                                                     PropertySimple propertySimple) {
        PropertyMap parentMap = propertySimple.getParentMap();
        return propertySimple.getConfiguration() != null
            || (parentMap != null && parentMap.getConfiguration() != null);
    }

    protected void updatePropertySimpleValue(Object value, PropertySimple propertySimple,
        PropertyDefinitionSimple propertyDefinitionSimple) {
        propertySimple.setErrorMessage(null);
        propertySimple.setValue(value);
    }

    protected static PropertyDefinition getTopLevelPropertyDefinition(PropertyDefinition propertyDefinition) {
        PropertyDefinition currentPropertyDefinition = propertyDefinition;
        while (currentPropertyDefinition.getConfigurationDefinition() == null) {
            if (currentPropertyDefinition.getParentPropertyListDefinition() != null) {
                currentPropertyDefinition = currentPropertyDefinition.getParentPropertyListDefinition();
            } else if (currentPropertyDefinition.getParentPropertyMapDefinition() != null) {
                currentPropertyDefinition = currentPropertyDefinition.getParentPropertyMapDefinition();
            } else {
                Log.error("Property definition " + currentPropertyDefinition + " has no parent.");
                break;
            }
        }
        return currentPropertyDefinition;
    }

    protected static Property getTopLevelProperty(Property property) {
        Property currentProperty = property;
        while (currentProperty.getConfiguration() == null) {
            if (currentProperty.getParentList() != null) {
                currentProperty = currentProperty.getParentList();
            } else if (currentProperty.getParentMap() != null) {
                currentProperty = currentProperty.getParentMap();
            } else {
                Log.error("Property " + currentProperty + " has no parent.");
                break;
            }
        }
        return currentProperty;
    }

    protected FormItem buildUnsetItem(final PropertyDefinitionSimple propertyDefinitionSimple,
        final PropertySimple propertySimple, final FormItem valueItem) {
        FormItem item;
        if (!propertyDefinitionSimple.isRequired()) {
            final CheckboxItem unsetItem = new CheckboxItem("unset-" + valueItem.getName());
            boolean unset = isUnset(propertyDefinitionSimple, propertySimple);
            unsetItem.setValue(unset);
            unsetItem.setDisabled(isReadOnly(propertyDefinitionSimple, propertySimple));
            unsetItem.setShowLabel(false);
            unsetItem.setShowTitle(false);
            unsetItem.setLabelAsTitle(false);

            unsetItem.addChangeHandler(new ChangeHandler() {
                public void onChange(ChangeEvent changeEvent) {
                    Boolean isUnset = (Boolean) changeEvent.getValue();
                    valueItem.setDisabled(isUnset);
                    if (isUnset) {
                        updatePropertySimpleValue(null, propertySimple, propertyDefinitionSimple);
                        setValue(valueItem, null);
                    } else {
                        valueItem.focusInItem();
                    }
                    valueItem.redraw();
                    propertySimple.setValue(valueItem.getValue());
                    firePropertyChangedEvent(propertySimple, propertyDefinitionSimple, true);
                }
            });

            valueItem.addBlurHandler(new BlurHandler() {
                public void onBlur(BlurEvent event) {
                    if (event.getItem().getValue() == null) {
                        unsetItem.setValue(true);
                        valueItem.disable();
                    }
                }
            });

            item = unsetItem;
        } else {
            item = new SpacerItem();
            item.setShowTitle(false);
        }
        return item;
    }

    private boolean isUnset(PropertyDefinitionSimple propertyDefinition, PropertySimple propertySimple) {
        return (!propertyDefinition.isRequired() && (propertySimple == null || propertySimple.getStringValue() == null));
    }

    protected boolean isReadOnly(PropertyDefinition propertyDefinition, Property property) {
        if (this.readOnly) {
            return true;
        }
        String errorMessage = property.getErrorMessage();
        if ((errorMessage != null) && (!errorMessage.trim().equals(""))) {
            // special case 1: property has a plugin-set error message - allow user to edit it even if it's a read-only prop,
            // otherwise the user will have no way to give the property a new value and thereby get things to a valid state
            return false;
        }
        if (property instanceof PropertySimple) {
            PropertySimple propertySimple = (PropertySimple) property;
            if (propertyDefinition.isRequired() &&
                    (propertySimple.getStringValue() == null) || "".equals(propertySimple.getStringValue())) {
                // special case 2: required simple prop with no value - allow user to edit it even if it's a read-only prop,
                // otherwise the user will have no way to give the property a new value and thereby get things to a valid state
                return false;
            }
        }
        return (propertyDefinition.isReadOnly());
    }

    protected List<Validator> buildValidators(PropertyDefinitionSimple propertyDefinition, PropertySimple property) {
        List<Validator> validators = new ArrayList<Validator>();

        Validator typeValidator = null;
        switch (propertyDefinition.getType()) {
        case STRING:
        case LONG_STRING:
        case FILE:
        case DIRECTORY:
            LengthRangeValidator lengthRangeValidator = new LengthRangeValidator();
            lengthRangeValidator.setMax(PropertySimple.MAX_VALUE_LENGTH);
            typeValidator = lengthRangeValidator;
            break;
        case INTEGER:
        case LONG:
            typeValidator = new IsIntegerValidator();
            break;
        case FLOAT:
        case DOUBLE:
            typeValidator = new IsFloatValidator();
            break;
        }
        if (typeValidator != null) {
            validators.add(typeValidator);
        }

        Set<Constraint> constraints = propertyDefinition.getConstraints();
        if (constraints != null) {
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
                    RegExpValidator validator = new RegExpValidator("^" + constraint.getDetails() + "$");
                    validators.add(validator);
                }
            }
        }

        PluginReportedErrorValidator validator = new PluginReportedErrorValidator(property);
        validators.add(validator);

        return validators;
    }

    private void displayMapEditor(final ListGrid summaryTable, final PropertyMapListGridRecord existingRecord,
                                  final PropertyDefinitionList propertyDefinitionList, final PropertyList propertyList, PropertyDefinitionMap memberMapDefinition, final PropertyMap memberMap,
                                  final boolean mapReadOnly) {

        final List<PropertyDefinition> memberDefinitions = new ArrayList<PropertyDefinition>(memberMapDefinition
            .getPropertyDefinitions().values());
        Collections.sort(memberDefinitions, new PropertyDefinitionComparator());

        final boolean newRow = (memberMap == null);
        final PropertyMap workingMap = (newRow) ? new PropertyMap(memberMapDefinition.getName()) : memberMap.deepCopy(true);

        final LocatableWindow popup = new LocatableWindow(extendLocatorId("MapEditor"));
        String title = (mapReadOnly) ? MSG.view_configEdit_viewRow() : MSG.view_configEdit_editRow();
        popup.setTitle(title);
        popup.setWidth(800);
        popup.setHeight(600);
        popup.setIsModal(true);
        popup.setShowModalMask(true);
        popup.setShowCloseButton(false);
        popup.centerInPage();

        final LocatableVLayout layout = new LocatableVLayout(popup.extendLocatorId("Layout"));
        layout.setHeight100();
        layout.setMargin(8);

        final DynamicForm childForm = buildPropertiesForm(layout.extendLocatorId("Form"), memberDefinitions, workingMap);
        childForm.setHeight100();
        layout.addMember(childForm);

        LocatableHLayout buttonBar = new LocatableHLayout(layout.extendLocatorId("ButtonBar"));
        buttonBar.setAlign(Alignment.CENTER);
        buttonBar.setMembersMargin(10);

        final IButton okButton = new LocatableIButton(buttonBar.extendLocatorId("OK"), MSG.common_button_ok());
        if (!mapReadOnly) {
            okButton.disable();
        }
        okButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (!mapReadOnly) {
                    if (!childForm.validate()) {
                        okButton.disable();
                        return;
                    }
                    if (newRow) {
                        propertyList.add(workingMap);
                        int index = propertyList.getList().size() - 1;
                        PropertyMapListGridRecord record = new PropertyMapListGridRecord(workingMap, index,
                                memberDefinitions);
                        summaryTable.addData(record);
                    } else {
                        mergePropertyMap(workingMap, memberMap, memberDefinitions);
                        existingRecord.refresh();
                        summaryTable.updateData(existingRecord);
                    }
                    firePropertyChangedEvent(propertyList, propertyDefinitionList, true);
                    summaryTable.redraw();
                }

                layout.destroy();
                popup.destroy();
            }
        });

        // Only enable the OK button if all properties are valid.
        childForm.addItemChangedHandler(new ItemChangedHandler() {
            public void onItemChanged(ItemChangedEvent itemChangedEvent) {
                // TODO (ips, 03/14/11): Ideally, we would validate the form here, but it's preventing boolean prop
                //                       radio buttons from being selectable, most likely due to a SmartGWT bug.
                //okButton.setDisabled(!childForm.validate());
                okButton.setDisabled(false);
            }
        });
        buttonBar.addMember(okButton);

        if (!mapReadOnly) {
            final IButton cancelButton = new LocatableIButton(buttonBar.extendLocatorId("Cancel"), MSG.common_button_cancel());
            cancelButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    layout.destroy();
                    popup.destroy();
                }
            });
            buttonBar.addMember(cancelButton);
        }

        layout.addMember(buttonBar);
        popup.addItem(layout);
        popup.show();
    }

    private void mergePropertyMap(PropertyMap sourceMap, PropertyMap targetMap,
                                  List<PropertyDefinition> memberDefinitions) {
        for (PropertyDefinition subDef : memberDefinitions) {
            PropertyDefinitionSimple subDefSimple = (PropertyDefinitionSimple) subDef;
            PropertySimple propertySimple = ((PropertySimple) sourceMap.get(subDefSimple.getName()));
            String newValue = (propertySimple != null) ? propertySimple.getStringValue() : null;
            PropertySimple existingProp = targetMap.getSimple(subDefSimple.getName());
            existingProp.setStringValue(newValue);
        }
    }

    protected static void setValue(FormItem item, Object value) {
        if (value instanceof String) {
            item.setValue((String) value);
        } else if (value instanceof Boolean) {
            item.setValue((Boolean) value);
        } else if (value instanceof Integer) {
            item.setValue((Integer) value);
        } else if (value instanceof Float) {
            item.setValue((Float) value);
        } else if (value instanceof Double) {
            item.setValue((Double) value);
        } else if (value instanceof Date) {
            item.setValue((Date) value);
        } else {
            String stringValue = (value != null) ? value.toString() : null;
            item.setValue(stringValue);
        }
        item.setDefaultValue((String) null);
    }

    private static class PropertyDefinitionComparator implements Comparator<PropertyDefinition> {
        public int compare(PropertyDefinition o1, PropertyDefinition o2) {
            return new Integer(o1.getOrder()).compareTo(o2.getOrder());
        }
    }

    private class PluginReportedErrorValidator extends CustomValidator {
        private Property property;

        public PluginReportedErrorValidator(Property property) {
            this.property = property;
        }

        @Override
        protected boolean condition(Object value) {
            String errorMessage = this.property.getErrorMessage();
            boolean valid = (errorMessage == null);
            if (!valid) {
                setErrorMessage(errorMessage);
            }
            return valid;
        }
    }

}
