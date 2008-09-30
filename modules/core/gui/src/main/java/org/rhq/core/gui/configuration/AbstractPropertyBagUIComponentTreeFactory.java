/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.gui.configuration;

import java.util.Collection;
import java.util.List;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIPanel;
import javax.faces.component.UIParameter;
import javax.faces.component.UISelectItem;
import javax.faces.component.UISelectOne;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.component.html.HtmlInputSecret;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlInputTextarea;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.component.html.HtmlSelectBooleanCheckbox;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.component.html.HtmlSelectOneRadio;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.configuration.AbstractPropertyMap;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.gui.RequestParameterNameConstants;
import org.rhq.core.gui.converter.PropertySimpleValueConverter;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.FacesExpressionUtility;
import org.rhq.core.gui.util.PropertyIdGeneratorUtility;
import org.rhq.core.gui.validator.PropertySimpleValueValidator;

/**
 * A factory that generates a tree of JSF components that depicts a given collection of JON {@link Property}s.
 *
 * @author Ian Springer (with some code snarfed from embedded's <code>org.jboss.on.embedded.ui.WidgetFactory</code>)
 */
public abstract class AbstractPropertyBagUIComponentTreeFactory {
    static final String DELETE_LIST_MEMBER_PROPERTY_FUNCTION = "DELETE_LIST_MEMBER_PROPERTY";
    static final String DELETE_OPEN_MAP_MEMBER_PROPERTY_FUNCTION = "DELETE_OPEN_MAP_MEMBER_PROPERTY";

    static final String PARAM_ID_SUFFIX = "-param";
    static final String PANEL_ID_SUFFIX = "-panel";

    protected static final String PROPERTY_SIMPLE_OVERRIDE_ACCESSOR_SUFFIX = "override";
    protected static final String PROPERTY_SIMPLE_VALUE_ACCESSOR_SUFFIX = "stringValue";
    protected static final String PROPERTY_LIST_VALUE_ACCESSOR_SUFFIX = "list";
    protected static final String PROPERTY_MAP_VALUE_ACCESSOR_SUFFIX = "map";

    private static final String VIEW_MAP_BUTTON_TITLE = "View Details";
    private static final String VIEW_MAP_BUTTON_LABEL = "View";
    private static final String EDIT_MAP_BUTTON_TITLE = "Edit";
    private static final String EDIT_MAP_BUTTON_LABEL = "Edit";
    private static final String DELETE_MAP_BUTTON_LABEL = "Delete";
    private static final String DELETE_MAP_BUTTON_TITLE = "Delete";
    private static final String ADD_NEW_MAP_BUTTON_LABEL = "Add New";
    private static final String ADD_NEW_MAP_BUTTON_TITLE = "Add New";

    private static final String PROPERTIES_TABLE_STYLE_CLASS = "properties-table";
    private static final String NESTED_PROPERTIES_TABLE_STYLE_CLASS = "nested-properties-table";
    private static final String PROPERTIES_TABLE_HEADER_CELL_STYLE_CLASS = "properties-table-header-cell";
    private static final String NESTED_PROPERTIES_TABLE_HEADER_CELL_STYLE_CLASS = "nested-properties-table-header-cell";
    private static final String NESTED_PROPERTIES_TABLE_INDENT_CELL_STYLE_CLASS = "nested-properties-table-indent-cell";
    private static final String DESCRIPTION_STYLE_CLASS = "description";
    private static final String PROPERTY_DISPLAY_NAME_CELL_STYLE_CLASS = "property-display-name-cell";
    private static final String PROPERTY_VALUE_CELL_STYLE_CLASS = "property-value-cell";
    private static final String PROPERTY_DESCRIPTION_CELL_STYLE_CLASS = "property-description-cell";
    private static final String PROPERTY_ENABLED_CELL_STYLE_CLASS = "property-enabled-cell";
    private static final String PROPERTY_ERROR_CELL_STYLE_CLASS = "property-error-cell";
    private static final String OPENMAP_PROPERTIES_TABLE_STYLE_CLASS = "openmap-properties-table";
    private static final String OPENMAP_PROPERTY_DISPLAY_NAME_CELL_STYLE_CLASS = "openmap-property-display-name-cell";
    private static final String OPENMAP_PROPERTY_VALUE_CELL_STYLE_CLASS = "openmap-property-value-cell";
    private static final String OPENMAP_PROPERTY_ACTIONS_CELL_STYLE_CLASS = "openmap-property-actions-cell";
    private static final String OPENMAP_PROPERTY_ERROR_CELL_STYLE_CLASS = "openmap-property-error-cell";
    private static final String LIST_PROPERTY_DISPLAY_NAME_CELL_STYLE_CLASS = "list-property-display-name-cell";
    private static final String LIST_PROPERTY_VALUE_CELL_STYLE_CLASS = "list-property-value-cell";
    private static final String LIST_PROPERTY_DESCRIPTION_CELL_STYLE_CLASS = "list-property-description-cell";
    private static final String LIST_PROPERTY_ENABLED_CELL_STYLE_CLASS = "list-property-enabled-cell";
    private static final String LIST_PROPERTY_CHILDREN_CELL_STYLE_CLASS = "list-property-children-cell";
    private static final String ERROR_MSG_STYLE_CLASS = "error-msg";
    private static final String PROPERTY_DISPLAY_NAME_TEXT_STYLE_CLASS = "property-display-name-text";
    private static final String PROPERTY_MAP_SUMMARY_TABLE_STYLE_CLASS = "property-map-summary-table";
    private static final String PROPERTY_MAP_SUMMARY_DATA_HEADER_CELL_STYLE_CLASS = "property-map-summary-data-header-cell";
    private static final String PROPERTY_MAP_SUMMARY_HEADER_TEXT_STYLE_CLASS = "property-map-summary-header-text";
    private static final String PROPERTY_MAP_SUMMARY_BUTTONS_CELL_STYLE_CLASS = "property-map-summary-buttons-cell";
    private static final String PROPERTY_MAP_SUMMARY_DATA_CELL_STYLE_CLASS = "property-map-summary-data-cell";
    private static final String PROPERTY_MAP_SUMMARY_DATA_TEXT_STYLE_CLASS = "property-map-summary-data-text";
    private static final String PROPERTY_MAP_SUMMARY_BUTTON_FOOTER_STYLE_CLASS = "property-buttonfooterrow";
    private static final String REQUIRED_MARKER_TEXT_STYLE_CLASS = "required-marker-text";
    private static final String BUTTONS_TABLE_STYLE_CLASS = "buttons-table";
    private static final String BUTTON_SMALL_STYLE_CLASS = "buttonsmall";
    private static final String ROW_ODD_STYLE_CLASS = "OddRow";
    private static final String ROW_EVEN_STYLE_CLASS = "EvenRow";

    private static final String INPUT_TEXT_WIDTH_STYLE = "width:185px;";
    private static final String INPUT_TEXT_WIDTH_STYLE_WITH_UNITS = "width:165px;";
    private static final int INPUT_TEXT_COMPONENT_WIDTH = 30;
    private static final int INPUT_TEXTAREA_COMPONENT_ROWS = 4;

    /**
     * Enums with a size equal to or greater than this threshold will be rendered as list boxes, rather than radios.
     */
    private static final int LISTBOX_THRESHOLD_ENUM_SIZE = 6;

    private ConfigUIComponent config;
    private Collection<PropertyDefinition> propertyDefinitions;
    private AbstractPropertyMap propertyMap;
    private boolean topLevel;
    private String valueExpressionFormat;
    private String overrideExpressionFormat;

    private final Log LOG = LogFactory.getLog(ConfigRenderer.class);

    public AbstractPropertyBagUIComponentTreeFactory(ConfigUIComponent config,
        Collection<PropertyDefinition> propertyDefinitions, AbstractPropertyMap propertyMap, boolean topLevel,
        String valueExpressionFormat) {
        this.config = config;
        this.propertyDefinitions = propertyDefinitions;
        this.propertyMap = propertyMap;
        this.topLevel = topLevel;
        this.valueExpressionFormat = valueExpressionFormat;
        this.overrideExpressionFormat = getOverrideExpressionFormat(valueExpressionFormat);
    }

    private String getOverrideExpressionFormat(String valueExpression) {
        // replace '.stringValue}' suffix with '.override}'
        int index = valueExpression.lastIndexOf(".");
        return valueExpression.substring(0, index) + "." + PROPERTY_SIMPLE_OVERRIDE_ACCESSOR_SUFFIX + "}";
    }

    protected Integer getListIndex() {
        return null;
    }

    public UIComponent createUIComponentTree(String rowStyleClass) {
        HtmlPanelGroup rootPanel = FacesComponentUtility.createBlockPanel(this.config,
            FacesComponentUtility.NO_STYLE_CLASS);
        if (!this.propertyDefinitions.isEmpty() || (this.propertyMap instanceof PropertyMap)) {
            if (!this.propertyDefinitions.isEmpty()) {
                String tableStyleClass = this.topLevel ? PROPERTIES_TABLE_STYLE_CLASS
                    : NESTED_PROPERTIES_TABLE_STYLE_CLASS;
                FacesComponentUtility.addVerbatimText(rootPanel, "\n\n<table class='" + tableStyleClass + "'>");
                int rowCount = 0;
                boolean alternateRowStyles = (rowStyleClass == null);
                if (containsSimpleProperties()) {
                    addSimplePropertiesTableHeaders(rootPanel); // the Name/Unset/Value/Description headers
                }

                // TODO: Display simple props before lists and maps, so the simple props immediately follow the
                //       Name/Unset/Value/Description header.
                for (PropertyDefinition propertyDefinition : this.propertyDefinitions) {
                    rowStyleClass = getRowStyleClass(rowStyleClass, rowCount, alternateRowStyles);
                    addProperty(rootPanel, propertyDefinition, rowStyleClass);
                    rowCount++;
                }
            } else {
                // If the map contains no member definitions, it means the map permits arbitrary simple properties as
                // members. We refer to such a map as an "open map."
                PropertyDefinitionMap propertyDefinitionMap = this.config.getConfigurationDefinition()
                    .getPropertyDefinitionMap(((PropertyMap) this.propertyMap).getName());
                FacesComponentUtility.addVerbatimText(rootPanel, "\n\n<table class='"
                    + OPENMAP_PROPERTIES_TABLE_STYLE_CLASS + "'>");
                addOpenMapMemberPropertiesTableHeaders(rootPanel, propertyDefinitionMap);
                for (Property property : this.propertyMap.getMap().values()) {
                    if (!(property instanceof PropertySimple)) {
                        throw new IllegalStateException("Open map " + this.propertyMap
                            + " contains non-simple property: " + property);
                    }

                    PropertySimple propertySimple = (PropertySimple) property;

                    // NOTE: Row colors never alternate for maps.
                    addOpenMapMemberProperty(rootPanel, propertyDefinitionMap, propertySimple, rowStyleClass);
                }

                addNewOpenMapMemberPropertyRow(rootPanel, propertyDefinitionMap);
            }

            FacesComponentUtility.addVerbatimText(rootPanel, "</table>");
        }

        return rootPanel;
    }

    private String getRowStyleClass(String rowStyleClass, int rowCount, boolean alternateRowStyles) {
        if (alternateRowStyles) {
            // add the odd/even row style
            rowStyleClass = ((rowCount % 2) == 0) ? ROW_ODD_STYLE_CLASS : ROW_EVEN_STYLE_CLASS;
        }

        return rowStyleClass;
    }

    private void addSimplePropertiesTableHeaders(UIComponent parent) {
        String headerCellStyleClass = this.topLevel ? PROPERTIES_TABLE_HEADER_CELL_STYLE_CLASS
            : NESTED_PROPERTIES_TABLE_HEADER_CELL_STYLE_CLASS;

        FacesComponentUtility.addVerbatimText(parent, "\n\n<tr>");

        FacesComponentUtility.addVerbatimText(parent, "<th class='" + headerCellStyleClass + "'>");
        FacesComponentUtility.addOutputText(parent, this.config, "Name", FacesComponentUtility.NO_STYLE_CLASS);
        FacesComponentUtility.addVerbatimText(parent, "</th>");

        if (config.isAggregate()) {
            FacesComponentUtility.addVerbatimText(parent, "<th class='" + headerCellStyleClass + "'>");
            FacesComponentUtility.addOutputText(parent, this.config, "Override", FacesComponentUtility.NO_STYLE_CLASS);
            FacesComponentUtility.addVerbatimText(parent, "</th>");
        }

        FacesComponentUtility.addVerbatimText(parent, "<th class='" + headerCellStyleClass + "'>");
        FacesComponentUtility.addOutputText(parent, this.config, "Unset", FacesComponentUtility.NO_STYLE_CLASS);
        FacesComponentUtility.addVerbatimText(parent, "</th>");

        FacesComponentUtility.addVerbatimText(parent, "<th class='" + headerCellStyleClass + "'>");
        FacesComponentUtility.addOutputText(parent, this.config, "Value", FacesComponentUtility.NO_STYLE_CLASS);
        FacesComponentUtility.addVerbatimText(parent, "</th>");

        FacesComponentUtility.addVerbatimText(parent, "<th class='" + headerCellStyleClass + "'>");
        FacesComponentUtility.addOutputText(parent, this.config, "Description", FacesComponentUtility.NO_STYLE_CLASS);
        FacesComponentUtility.addVerbatimText(parent, "</th>");

        FacesComponentUtility.addVerbatimText(parent, "</tr>");
    }

    private boolean containsSimpleProperties() {
        boolean containsSimples = false;
        for (PropertyDefinition propertyDefinition : this.propertyDefinitions) {
            if (propertyDefinition instanceof PropertyDefinitionSimple) {
                containsSimples = true;
                break;
            }
        }

        return containsSimples;
    }

    private void addOpenMapMemberPropertiesTableHeaders(UIComponent parent, PropertyDefinitionMap propertyDefinitionMap) {
        String headerCellStyleClass = this.topLevel ? PROPERTIES_TABLE_HEADER_CELL_STYLE_CLASS
            : NESTED_PROPERTIES_TABLE_HEADER_CELL_STYLE_CLASS;

        FacesComponentUtility.addVerbatimText(parent, "\n\n<tr>");

        FacesComponentUtility.addVerbatimText(parent, "<th class='" + headerCellStyleClass + "'>");
        FacesComponentUtility.addOutputText(parent, this.config, "Name", FacesComponentUtility.NO_STYLE_CLASS);
        FacesComponentUtility.addVerbatimText(parent, "</th>");

        FacesComponentUtility.addVerbatimText(parent, "<th class='" + headerCellStyleClass + "'>");
        FacesComponentUtility.addOutputText(parent, this.config, "Value", FacesComponentUtility.NO_STYLE_CLASS);
        FacesComponentUtility.addVerbatimText(parent, "</th>");

        if (!isReadOnly(propertyDefinitionMap)) {
            FacesComponentUtility.addVerbatimText(parent, "<th class='" + headerCellStyleClass + "'>");
            FacesComponentUtility.addOutputText(parent, this.config, "Actions", FacesComponentUtility.NO_STYLE_CLASS);
            FacesComponentUtility.addVerbatimText(parent, "</th>");
        }

        FacesComponentUtility.addVerbatimText(parent, "</tr>");
    }

    protected ConfigUIComponent getConfig() {
        return config;
    }

    private void addProperty(UIComponent parent, PropertyDefinition propertyDefinition, String rowStyleClass) {
        UIPanel propertyPanel = FacesComponentUtility.addBlockPanel(parent, this.config,
            FacesComponentUtility.NO_STYLE_CLASS);
        if (propertyDefinition instanceof PropertyDefinitionSimple) {
            addSimpleProperty(propertyPanel, (PropertyDefinitionSimple) propertyDefinition, rowStyleClass);
        } else if (propertyDefinition instanceof PropertyDefinitionList) {
            addListProperty(propertyPanel, (PropertyDefinitionList) propertyDefinition, rowStyleClass);
        } else if (propertyDefinition instanceof PropertyDefinitionMap) {
            addMapProperty(propertyPanel, (PropertyDefinitionMap) propertyDefinition, rowStyleClass);
        } else {
            throw new IllegalStateException("Unsupported subclass of " + PropertyDefinition.class.getName() + ".");
        }
    }

    private void addSimpleProperty(UIComponent parent, PropertyDefinitionSimple propertyDefinitionSimple,
        String rowStyleClass) {
        addDebug(parent, true, ".addSimpleProperty()");

        FacesComponentUtility.addVerbatimText(parent, "\n\n<tr class='" + rowStyleClass + "'>");

        // generate some elements ahead of time, so that dependent
        UIInput input = createInputForSimpleProperty(propertyDefinitionSimple);
        HtmlSelectBooleanCheckbox unsetCheckbox = FacesComponentUtility.createComponent(
            HtmlSelectBooleanCheckbox.class, this.config);
        // done generating

        FacesComponentUtility.addVerbatimText(parent, "<td class='" + PROPERTY_DISPLAY_NAME_CELL_STYLE_CLASS + "'>");
        addPropertyDisplayName(parent, propertyDefinitionSimple);
        FacesComponentUtility.addVerbatimText(parent, "</td>");

        if (config.isAggregate()) {
            FacesComponentUtility.addVerbatimText(parent, "<td class='" + PROPERTY_ENABLED_CELL_STYLE_CLASS + "'>");
            addPropertyOverrideControl(parent, propertyDefinitionSimple, input, unsetCheckbox);
            FacesComponentUtility.addVerbatimText(parent, "</td>");
        }

        FacesComponentUtility.addVerbatimText(parent, "<td class='" + PROPERTY_ENABLED_CELL_STYLE_CLASS + "'>");
        addPropertyUnsetControl(parent, propertyDefinitionSimple, input, unsetCheckbox);
        FacesComponentUtility.addVerbatimText(parent, "</td>");

        FacesComponentUtility.addVerbatimText(parent, "<td class='" + PROPERTY_VALUE_CELL_STYLE_CLASS + "'>");
        parent.getChildren().add(input);
        FacesComponentUtility.addVerbatimText(parent, "<br/>");
        addErrorMessages(parent, input, propertyDefinitionSimple);
        FacesComponentUtility.addVerbatimText(parent, "</td>");

        FacesComponentUtility.addVerbatimText(parent, "<td class='" + PROPERTY_DESCRIPTION_CELL_STYLE_CLASS + "'>");
        addPropertyDescription(parent, propertyDefinitionSimple);
        FacesComponentUtility.addVerbatimText(parent, "</td>");

        FacesComponentUtility.addVerbatimText(parent, "</tr>");

        addDebug(parent, false, ".addSimpleProperty()");
    }

    private void addOpenMapMemberProperty(HtmlPanelGroup parent, PropertyDefinitionMap propertyDefinitionMap,
        PropertySimple propertySimple, String rowStyleClass) {
        addDebug(parent, true, ".addOpenMapMemberProperty()");
        HtmlPanelGroup panel = FacesComponentUtility.addBlockPanel(parent, getConfig(),
            FacesComponentUtility.NO_STYLE_CLASS);
        String mapName = ((PropertyMap) this.propertyMap).getName();
        String memberName = propertySimple.getName();
        String panelId = PropertyIdGeneratorUtility.getIdentifier(propertySimple, getListIndex(), PANEL_ID_SUFFIX);
        panel.setId(panelId);

        FacesComponentUtility.addVerbatimText(panel, "\n\n<tr class='" + rowStyleClass + "'>");

        FacesComponentUtility.addVerbatimText(panel, "<td class='" + OPENMAP_PROPERTY_DISPLAY_NAME_CELL_STYLE_CLASS
            + "'>");
        FacesComponentUtility.addOutputText(panel, this.config, propertySimple.getName(),
            PROPERTY_DISPLAY_NAME_TEXT_STYLE_CLASS);
        FacesComponentUtility.addVerbatimText(panel, "</td>");

        FacesComponentUtility.addVerbatimText(panel, "<td class='" + OPENMAP_PROPERTY_VALUE_CELL_STYLE_CLASS + "'>");
        UIInput input = createInputForSimpleProperty(propertySimple);
        panel.getChildren().add(input);
        FacesComponentUtility.addVerbatimText(panel, "</td>");

        addErrorMessages(panel, input, propertySimple);

        if (!isReadOnly(propertyDefinitionMap)) {
            // add Actions column w/ delete button
            FacesComponentUtility.addVerbatimText(panel, "<td class='" + OPENMAP_PROPERTY_ACTIONS_CELL_STYLE_CLASS
                + "'>");
            HtmlCommandLink deleteLink = FacesComponentUtility.addCommandLink(panel, this.config);
            deleteLink.setTitle(DELETE_MAP_BUTTON_TITLE);
            //deleteLink.setImmediate(true); // skip validation (we only want to validate upon Save)

            FacesComponentUtility.addParameter(deleteLink, this.config, RequestParameterNameConstants.FUNCTION_PARAM,
                DELETE_OPEN_MAP_MEMBER_PROPERTY_FUNCTION);
            FacesComponentUtility.addParameter(deleteLink, this.config, RequestParameterNameConstants.MAP_NAME_PARAM,
                mapName);
            FacesComponentUtility.addParameter(deleteLink, this.config,
                RequestParameterNameConstants.MEMBER_NAME_PARAM, memberName);

            FacesComponentUtility.addButton(deleteLink, DELETE_MAP_BUTTON_LABEL, BUTTON_SMALL_STYLE_CLASS);
            FacesComponentUtility.addVerbatimText(panel, "</td>");
        }

        FacesComponentUtility.addVerbatimText(panel, "</tr>");
        addDebug(parent, false, ".addOpenMapMemberProperty()");
    }

    /**
     * Adds a single row property
     */
    private void addListProperty(UIComponent parent, PropertyDefinitionList listPropertyDefinition, String rowStyleClass) {
        addDebug(parent, true, ".addListProperty()");
        if (!this.topLevel) {
            throw new IllegalStateException("Lists are only supported at the top level of a Configuration.");
        }

        PropertyList listProperty = this.propertyMap.getList(listPropertyDefinition.getName());

        FacesComponentUtility.addVerbatimText(parent, "\n\n<tr class='" + rowStyleClass + "'>");

        FacesComponentUtility.addVerbatimText(parent, "<td class='" + LIST_PROPERTY_DISPLAY_NAME_CELL_STYLE_CLASS
            + "'>");
        addPropertyDisplayName(parent, listPropertyDefinition);
        FacesComponentUtility.addVerbatimText(parent, "</td>");

        FacesComponentUtility.addVerbatimText(parent, "<td class='" + LIST_PROPERTY_ENABLED_CELL_STYLE_CLASS + "' />");

        FacesComponentUtility.addVerbatimText(parent, "<td class='" + LIST_PROPERTY_VALUE_CELL_STYLE_CLASS + "' />");

        FacesComponentUtility
            .addVerbatimText(parent, "<td class='" + LIST_PROPERTY_DESCRIPTION_CELL_STYLE_CLASS + "'>");
        addPropertyDescription(parent, listPropertyDefinition);
        FacesComponentUtility.addVerbatimText(parent, "</td>");

        FacesComponentUtility.addVerbatimText(parent, "</tr>");

        PropertyDefinition listMemberPropertyDefinition = listPropertyDefinition.getMemberDefinition();
        if (listMemberPropertyDefinition instanceof PropertyDefinitionMap) {
            addListMemberMapProperties(parent, listProperty, (PropertyDefinitionMap) listMemberPropertyDefinition,
                rowStyleClass);
        } else {
            addErrorRow(parent, "Rendering of lists of " + listMemberPropertyDefinition.getClass().getSimpleName()
                + " properties is not yet supported. Here's the list's toString() value for now: "
                + ((listProperty != null) ? listProperty.getList() : null), rowStyleClass);
        }

        addDebug(parent, false, ".addListProperty()");
    }

    private void addListMemberMapProperties(UIComponent parent, PropertyList listProperty,
        PropertyDefinitionMap listMemberPropertyDefinition, String rowStyleClass) {
        addDebug(parent, true, ".addListMemberMapProperties()");
        PropertyDefinitionMap listMemberMapPropertyDefinition = listMemberPropertyDefinition;
        validateMapDefinition(listMemberMapPropertyDefinition);

        FacesComponentUtility.addVerbatimText(parent, "\n\n<tr class='" + rowStyleClass + "'>\n");
        FacesComponentUtility.addVerbatimText(parent, "<td colspan='4' class='"
            + LIST_PROPERTY_CHILDREN_CELL_STYLE_CLASS + "'>");

        Collection<PropertyDefinition> mapSummaryPropertyDefinitions = listMemberMapPropertyDefinition
            .getSummaryPropertyDefinitions();

        // Start the "List of maps" table
        FacesComponentUtility.addVerbatimText(parent, "\n<!-- start list of maps -->\n");
        FacesComponentUtility.addVerbatimText(parent, "<table class='" + PROPERTY_MAP_SUMMARY_TABLE_STYLE_CLASS
            + "'>\n");
        FacesComponentUtility.addVerbatimText(parent, "<tbody>\n");

        // Add the row for the headers
        FacesComponentUtility.addVerbatimText(parent, "<tr>\n");

        // Add column header cells...
        for (PropertyDefinition summaryPropertyDefinition : listMemberMapPropertyDefinition
            .getSummaryPropertyDefinitions()) {
            // We can safely cast here, because we already validated the map member definitions.
            PropertyDefinitionSimple mapMemberSimplePropertyDefinition = (PropertyDefinitionSimple) listMemberMapPropertyDefinition
                .get(summaryPropertyDefinition.getName());
            FacesComponentUtility.addVerbatimText(parent, "<th class='"
                + PROPERTY_MAP_SUMMARY_DATA_HEADER_CELL_STYLE_CLASS + "'>");
            FacesComponentUtility.addOutputText(parent, this.config,
                mapMemberSimplePropertyDefinition.getDisplayName(), PROPERTY_MAP_SUMMARY_HEADER_TEXT_STYLE_CLASS);
            FacesComponentUtility.addVerbatimText(parent, "</th>\n");
        }

        // add the "Actions" header
        FacesComponentUtility.addVerbatimText(parent, "<th class='" + PROPERTY_MAP_SUMMARY_DATA_HEADER_CELL_STYLE_CLASS
            + "'>");
        FacesComponentUtility.addOutputText(parent, this.config, "Actions",
            PROPERTY_MAP_SUMMARY_HEADER_TEXT_STYLE_CLASS);
        FacesComponentUtility.addVerbatimText(parent, "</th>\n");
        FacesComponentUtility.addVerbatimText(parent, "</tr>\n");

        // Add body cells...
        // NOTE: We only display the View button if the config component is read-only. If the list is read-only, members
        //       cannot be added or deleted, but existing members can still be edited.
        String viewEditButtonLabel = (this.config.isReadOnly()) ? VIEW_MAP_BUTTON_LABEL : EDIT_MAP_BUTTON_LABEL;
        String viewEditButtonTitle = (this.config.isReadOnly()) ? VIEW_MAP_BUTTON_TITLE : EDIT_MAP_BUTTON_TITLE;
        for (int index = 0; index < listProperty.getList().size(); index++) {
            addListMemberMapProperty(parent, listProperty, listMemberMapPropertyDefinition, viewEditButtonLabel,
                viewEditButtonTitle, index);
        }
        //HtmlPanelGrid buttonsPanelGrid = FacesComponentUtility.addPanelGrid(parent, this.configurationUIBean, 1, BUTTONS_TABLE_STYLE_CLASS);

        if (!isReadOnly(listMemberMapPropertyDefinition.getParentPropertyListDefinition())) {
            FacesComponentUtility.addVerbatimText(parent, "<tr>\n");
            FacesComponentUtility.addVerbatimText(parent, "<td colspan='" + (mapSummaryPropertyDefinitions.size() + 1)
                + "' class='" + PROPERTY_MAP_SUMMARY_BUTTON_FOOTER_STYLE_CLASS + "'>");

            // add-new button
            HtmlCommandLink addNewLink = FacesComponentUtility.addCommandLink(parent, this.config);
            addNewLink.setTitle(ADD_NEW_MAP_BUTTON_TITLE);
            MethodExpression actionExpression = FacesExpressionUtility.createMethodExpression(
                "#{ConfigHelperUIBean.addNewMap}", String.class, new Class[0]);
            addNewLink.setActionExpression(actionExpression);
            FacesComponentUtility.addParameter(addNewLink, this.config, RequestParameterNameConstants.LIST_NAME_PARAM,
                listProperty.getName());
            FacesComponentUtility.addParameter(addNewLink, this.config, RequestParameterNameConstants.LIST_INDEX_PARAM,
                String.valueOf(listProperty.getList().size()));
            FacesComponentUtility.addButton(addNewLink, ADD_NEW_MAP_BUTTON_LABEL, BUTTON_SMALL_STYLE_CLASS);
            FacesComponentUtility.addVerbatimText(parent, "</td></tr>\n");
        }

        FacesComponentUtility.addVerbatimText(parent, "</tbody></table>\n\n");
        FacesComponentUtility.addVerbatimText(parent, "<!-- end List of Maps -->\n\n");

        FacesComponentUtility.addVerbatimText(parent, "</td></tr>");
        addDebug(parent, false, ".addListMemberMapProperties()");
    }

    private void addListMemberMapProperty(UIComponent parent, PropertyList listProperty,
        PropertyDefinitionMap listMemberMapPropertyDefinition, String viewEditButtonLabel, String viewEditButtonTitle,
        int index) {
        addDebug(parent, true, ".addListMemberMapProperty()");
        Property listMemberProperty = listProperty.getList().get(index);
        String listName = listProperty.getName();
        if (!(listMemberProperty instanceof PropertyMap)) {
            throw new IllegalStateException("Property '" + listName
                + "' is defined as a list of maps but contains one or more non-map members.");
        }

        PropertyMap listMemberPropertyMap = (PropertyMap) listMemberProperty;

        HtmlPanelGroup panel = FacesComponentUtility.addBlockPanel(parent, getConfig(),
            FacesComponentUtility.NO_STYLE_CLASS);
        String listIndex = String.valueOf(index);
        String panelId = PropertyIdGeneratorUtility.getIdentifier(listMemberProperty, index, PANEL_ID_SUFFIX);
        panel.setId(panelId);

        FacesComponentUtility.addVerbatimText(panel, "<tr>\n");

        // add the simple property data cells
        addPropertyMapSummaryDataCells(panel, listMemberMapPropertyDefinition, listMemberPropertyMap);

        int numberOfButtons = 1;
        if (!this.config.isReadOnly()) {
            numberOfButtons++;
        }

        // add the Actions cell
        FacesComponentUtility.addVerbatimText(panel, "<td class='" + PROPERTY_MAP_SUMMARY_BUTTONS_CELL_STYLE_CLASS
            + "'>");
        HtmlPanelGrid buttonsPanelGrid = FacesComponentUtility.addPanelGrid(panel, this.config, numberOfButtons,
            BUTTONS_TABLE_STYLE_CLASS);

        // view/edit button
        HtmlCommandLink viewEditLink = FacesComponentUtility.addCommandLink(buttonsPanelGrid, this.config);
        viewEditLink.setTitle(viewEditButtonTitle);
        MethodExpression actionExpression = FacesExpressionUtility.createMethodExpression(
            "#{ConfigHelperUIBean.accessMap}", String.class, new Class[0]);
        viewEditLink.setActionExpression(actionExpression);
        FacesComponentUtility.addParameter(viewEditLink, this.config, RequestParameterNameConstants.LIST_NAME_PARAM,
            listName);
        FacesComponentUtility.addParameter(viewEditLink, this.config, RequestParameterNameConstants.LIST_INDEX_PARAM,
            listIndex);
        FacesComponentUtility.addButton(viewEditLink, viewEditButtonLabel, BUTTON_SMALL_STYLE_CLASS);

        if (!isReadOnly(listMemberMapPropertyDefinition.getParentPropertyListDefinition())) {
            // delete button
            HtmlCommandLink deleteLink = FacesComponentUtility.addCommandLink(buttonsPanelGrid, this.config);
            deleteLink.setTitle(DELETE_MAP_BUTTON_TITLE);
            //deleteLink.setImmediate(true);

            FacesComponentUtility.addParameter(deleteLink, this.config, RequestParameterNameConstants.FUNCTION_PARAM,
                DELETE_LIST_MEMBER_PROPERTY_FUNCTION);
            FacesComponentUtility.addParameter(deleteLink, this.config, RequestParameterNameConstants.LIST_NAME_PARAM,
                listName);
            UIParameter listIndexParam = FacesComponentUtility.addParameter(deleteLink, this.config,
                RequestParameterNameConstants.LIST_INDEX_PARAM, listIndex);
            String paramId = PropertyIdGeneratorUtility.getIdentifier(listMemberProperty, index, PARAM_ID_SUFFIX);
            listIndexParam.setId(paramId);

            FacesComponentUtility.addButton(deleteLink, DELETE_MAP_BUTTON_LABEL, BUTTON_SMALL_STYLE_CLASS);
        }

        FacesComponentUtility.addVerbatimText(panel, "</td>\n");

        FacesComponentUtility.addVerbatimText(panel, "</tr>\n");
        addDebug(parent, false, ".addListMemberMapProperty()");
    }

    private void addPropertyMapSummaryDataCells(UIComponent parent,
        PropertyDefinitionMap listMemberMapPropertyDefinition, PropertyMap listMemberMapProperty) {
        for (PropertyDefinition summaryPropertyDefinition : listMemberMapPropertyDefinition
            .getSummaryPropertyDefinitions()) {
            Property mapMemberProperty = listMemberMapProperty.get(summaryPropertyDefinition.getName());
            if (!(mapMemberProperty instanceof PropertySimple)) {
                throw new IllegalStateException("Property '" + mapMemberProperty.getName()
                    + "' is defined as a map of simples but contains one or more non-simple members.");
            }

            PropertySimple mapMemberSimpleProperty = (PropertySimple) mapMemberProperty;
            FacesComponentUtility.addVerbatimText(parent, "<td class='" + PROPERTY_MAP_SUMMARY_DATA_CELL_STYLE_CLASS
                + "'>");
            if (mapMemberSimpleProperty.getStringValue() == null) {
                FacesComponentUtility.addOutputText(parent, this.config, "not set", REQUIRED_MARKER_TEXT_STYLE_CLASS);
            } else {
                FacesComponentUtility.addOutputText(parent, this.config, mapMemberSimpleProperty.getStringValue(),
                    PROPERTY_MAP_SUMMARY_DATA_TEXT_STYLE_CLASS);
            }

            FacesComponentUtility.addVerbatimText(parent, "</td>\n");
        }
    }

    private void validateMapDefinition(PropertyDefinitionMap propertyDefinitionMap) {
        for (PropertyDefinition mapMemberPropertyDefinition : propertyDefinitionMap.getPropertyDefinitions().values()) {
            if (!(mapMemberPropertyDefinition instanceof PropertyDefinitionSimple)) {
                throw new IllegalStateException("Only maps of simple properties are supported.");
            }
        }
    }

    private void addMapProperty(UIComponent parent, PropertyDefinitionMap propertyDefinitionMap, String rowStyleClass) {
        if (!this.topLevel) {
            throw new IllegalStateException(
                "Maps are only supported at the top level of a Configuration or within a top-level List.");
        }

        validateMapDefinition(propertyDefinitionMap);

        FacesComponentUtility.addVerbatimText(parent, "\n\n<tr class='" + rowStyleClass + "'>");

        FacesComponentUtility.addVerbatimText(parent, "<td class='" + LIST_PROPERTY_DISPLAY_NAME_CELL_STYLE_CLASS
            + "'>");
        addPropertyDisplayName(parent, propertyDefinitionMap);
        FacesComponentUtility.addVerbatimText(parent, "</td>");

        FacesComponentUtility.addVerbatimText(parent, "<td class='" + LIST_PROPERTY_ENABLED_CELL_STYLE_CLASS + "' />");

        FacesComponentUtility.addVerbatimText(parent, "<td class='" + LIST_PROPERTY_VALUE_CELL_STYLE_CLASS + "' />");

        FacesComponentUtility
            .addVerbatimText(parent, "<td class='" + LIST_PROPERTY_DESCRIPTION_CELL_STYLE_CLASS + "'>");
        addPropertyDescription(parent, propertyDefinitionMap);
        FacesComponentUtility.addVerbatimText(parent, "</td>");

        FacesComponentUtility.addVerbatimText(parent, "</tr>");

        addMapMemberProperties(parent, propertyDefinitionMap.getName(), rowStyleClass);
    }

    private void addMapMemberProperties(UIComponent parent, String mapName, String rowStyleClass) {
        FacesComponentUtility.addVerbatimText(parent, "\n\n<tr class='" + rowStyleClass + "'>\n");
        FacesComponentUtility.addVerbatimText(parent, "<td colspan='4' class='"
            + LIST_PROPERTY_CHILDREN_CELL_STYLE_CLASS + "'>");
        HtmlPanelGrid spacerPanelGrid = FacesComponentUtility.addPanelGrid(parent, this.config, 2,
            FacesComponentUtility.NO_STYLE_CLASS);
        spacerPanelGrid.setWidth("100%");
        spacerPanelGrid.setColumnClasses(NESTED_PROPERTIES_TABLE_INDENT_CELL_STYLE_CLASS + ","
            + FacesComponentUtility.NO_STYLE_CLASS);
        FacesComponentUtility.addBlockPanel(spacerPanelGrid, this.config, FacesComponentUtility.NO_STYLE_CLASS);
        AbstractPropertyBagUIComponentTreeFactory propertyListUIComponentTreeFactory = new MapInConfigurationUIComponentTreeFactory(
            this.config, mapName);
        spacerPanelGrid.getChildren().add(
            propertyListUIComponentTreeFactory.createUIComponentTree(ROW_EVEN_STYLE_CLASS));
        parent.getChildren().add(spacerPanelGrid);
        FacesComponentUtility.addVerbatimText(parent, "</td></tr>\n");
    }

    private void addNewOpenMapMemberPropertyRow(UIComponent parent, PropertyDefinitionMap propertyDefinitionMap) {
        if (!isReadOnly(propertyDefinitionMap)) {
            FacesComponentUtility.addVerbatimText(parent, "\n\n<tr><td colspan='3' class='"
                + PROPERTY_MAP_SUMMARY_BUTTON_FOOTER_STYLE_CLASS + "'>");

            // add-new button
            HtmlCommandLink addNewLink = FacesComponentUtility.addCommandLink(parent, this.config);
            addNewLink.setTitle(ADD_NEW_MAP_BUTTON_TITLE);

            //addNewLink.setImmediate(true); // skip validation (we only want to validate upon Save)
            MethodExpression actionExpression = FacesExpressionUtility.createMethodExpression(
                "#{ConfigHelperUIBean.addNewOpenMapMemberProperty}", String.class, new Class[0]);
            addNewLink.setActionExpression(actionExpression);
            FacesComponentUtility.addParameter(addNewLink, this.config, RequestParameterNameConstants.MAP_NAME_PARAM,
                propertyDefinitionMap.getName());
            FacesComponentUtility.addButton(addNewLink, ADD_NEW_MAP_BUTTON_LABEL, BUTTON_SMALL_STYLE_CLASS);
            FacesComponentUtility.addVerbatimText(parent, "</td></tr>");
        }
    }

    private void addErrorRow(UIComponent parent, String errorMsg, String rowStyleClass) {
        addDebug(parent, true, ".addErrorRow()");
        FacesComponentUtility.addVerbatimText(parent, "\n\n<tr class='" + rowStyleClass + "'><td colspan='4' class='"
            + PROPERTY_ERROR_CELL_STYLE_CLASS + "'><h4>" + errorMsg + "</h4></td></tr>");
        addDebug(parent, false, ".addErrorRow()");
    }

    @NotNull
    private HtmlSelectBooleanCheckbox createInputOverrideForSimpleProperty(
        PropertyDefinitionSimple propertyDefinitionSimple) {
        HtmlSelectBooleanCheckbox input = FacesComponentUtility.createComponent(HtmlSelectBooleanCheckbox.class,
            this.config);

        // Find the actual property corresponding to this property def, and use that to create the component id.
        Property property = this.propertyMap.get(propertyDefinitionSimple.getName());
        if (property != null) {
            // add suffix to prevent collision with value input identifier for property
            String propertyId = PropertyIdGeneratorUtility.getIdentifier(property, getListIndex(), "override");
            input.setId(propertyId);

            Boolean shouldOverride = ((PropertySimple) property).getOverride();
            if (shouldOverride == null) {
                FacesComponentUtility.setOverride(input, false);
            } else {
                FacesComponentUtility.setOverride(input, shouldOverride);
            }
        }

        setInputOverrideExpression(input, propertyDefinitionSimple.getName());

        return input;
    }

    @NotNull
    private UIInput createInputForSimpleProperty(PropertyDefinitionSimple propertyDefinitionSimple) {
        UIInput input;
        switch (propertyDefinitionSimple.getType()) {
        case BOOLEAN: {
            input = createInputForBooleanProperty();
            break;
        }

        case LONG_STRING: {
            input = createInputForLongStringProperty();
            break;
        }

        case PASSWORD: {
            input = createInputForPasswordProperty();
            break;
        }

        default: {
            if (isEnum(propertyDefinitionSimple)) {
                input = createInputForEnumProperty(propertyDefinitionSimple);
            } else {
                input = createInputForStringProperty();
            }

            String propertyValue = getSimplePropertyValue(propertyDefinitionSimple);
            addTitleAttribute(input, propertyValue);
        }
        }

        boolean isUnset = isUnset(propertyDefinitionSimple);
        boolean isReadOnly = isReadOnly(propertyDefinitionSimple);

        // Find the actual property corresponding to this property def, and use that to create the component id.
        Property property = this.propertyMap.get(propertyDefinitionSimple.getName());
        if (property != null) {
            String propertyId = PropertyIdGeneratorUtility.getIdentifier(property, getListIndex());
            input.setId(propertyId);
        }

        setInputValueExpression(input, propertyDefinitionSimple.getName());

        // It's important to set the label attribute, since we include it in our validation error messages.
        input.getAttributes().put("label", propertyDefinitionSimple.getDisplayName());

        // The below adds an inert attribute to the input that contains the name of the associated property - useful
        // for debugging (i.e. when viewing source of the page or using a JavaScript debugger).
        input.getAttributes().put("ondblclick", "//" + propertyDefinitionSimple.getName());

        FacesComponentUtility.setUnset(input, isUnset);
        FacesComponentUtility.setReadonly(input, isReadOnly);

        addValidatorsAndConverter(input, propertyDefinitionSimple);

        return input;
    }

    @NotNull
    private UIInput createInputForSimpleProperty(PropertySimple propertySimple) {
        UIInput input = createInputForStringProperty();
        setInputValueExpression(input, propertySimple.getName());
        FacesComponentUtility.setReadonly(input, this.config.isReadOnly());
        addTitleAttribute(input, propertySimple.getStringValue());
        return input;
    }

    private void addTitleAttribute(UIInput input, String propertyValue) {
        if (input instanceof HtmlInputText) {
            //         TODO: will this still work now that we use the style def'n "width:185px;" to define the input field width?
            // For text inputs with values that are too long to fit in the input text field, add a "title" attribute set to
            // the value, so the user can see the untruncated value via a tooltip.
            // (see http://jira.jboss.com/jira/browse/JBNADM-1608)
            HtmlInputText inputText = (HtmlInputText) input;
            if ((propertyValue != null) && (propertyValue.length() > INPUT_TEXT_COMPONENT_WIDTH)) {
                inputText.setTitle(propertyValue);
            }

            inputText.setOnchange("setInputTitle(this)");
        }
    }

    private String getSimplePropertyValue(PropertyDefinitionSimple propertyDefinitionSimple) {
        String propertyValueExpression = String.format(this.valueExpressionFormat, propertyDefinitionSimple.getName());
        return FacesExpressionUtility.getValue(propertyValueExpression, String.class);
    }

    private boolean isEnum(PropertyDefinitionSimple simplePropertyDefinition) {
        return !simplePropertyDefinition.getEnumeratedValues().isEmpty();
    }

    private void addValidatorsAndConverter(UIInput input, PropertyDefinitionSimple propertyDefinitionSimple) {
        if (!this.config.isReadOnly()) {
            input.setRequired(propertyDefinitionSimple.isRequired());
            input.addValidator(new PropertySimpleValueValidator(propertyDefinitionSimple));
            input.setConverter(new PropertySimpleValueConverter());
        }
    }

    private void addErrorMessages(UIComponent parent, UIInput input, PropertyDefinitionSimple propertyDefinitionSimple) {
        addDebug(parent, true, ".addErrorMessages()");
        addMessageComponentForInput(parent, input);

        // Pre-validate the property's value, in case the PC sent us an invalid live config.
        PropertySimpleValueValidator validator = new PropertySimpleValueValidator(propertyDefinitionSimple);
        PropertySimple propertySimple = this.propertyMap.getSimple(propertyDefinitionSimple.getName());
        prevalidatePropertyValue(input, propertySimple, validator);

        // If there is a PC-detected error associated with the property, associate it with the input.
        addPluginContainerDetectedErrorMessage(input, propertySimple);
        addDebug(parent, false, ".addErrorMessages()");
    }

    private void addErrorMessages(UIComponent parent, UIInput input, PropertySimple propertySimple) {
        addDebug(parent, true, ".addErrorMessages()");
        addMessageComponentForInput(parent, input);

        // Pre-validate the property's value, in case the PC sent us an invalid live config.
        PropertySimpleValueValidator validator = new PropertySimpleValueValidator();
        if (this.config.isPrevalidate()) {
            prevalidatePropertyValue(input, propertySimple, validator);
        }

        // If there is a PC-detected error associated with the property, associate it with the input.
        addPluginContainerDetectedErrorMessage(input, propertySimple);
        addDebug(parent, false, ".addErrorMessages()");
    }

    private void addMessageComponentForInput(UIComponent parent, UIInput input) {
        // <h:message for="#{input-component-id}" showDetail="true" errorClass="error-msg" />
        FacesComponentUtility.addMessage(parent, this.config, input.getId(), ERROR_MSG_STYLE_CLASS);
    }

    private void prevalidatePropertyValue(UIInput input, PropertySimple propertySimple,
        PropertySimpleValueValidator validator) {
        FacesContext facesContext = FacesContextUtility.getFacesContext();
        try {
            String value = (propertySimple != null) ? propertySimple.getStringValue() : null;
            validator.validate(facesContext, input, value);
        } catch (ValidatorException e) {
            // NOTE: It's vital to pass the client id, *not* the component id, to addMessage().
            facesContext.addMessage(input.getClientId(facesContext), e.getFacesMessage());
        }
    }

    private void addPluginContainerDetectedErrorMessage(UIInput input, PropertySimple propertySimple) {
        String errorMsg = (propertySimple != null) ? propertySimple.getErrorMessage() : null;
        if ((errorMsg != null) && !errorMsg.equals("")) {
            FacesContext facesContext = FacesContextUtility.getFacesContext();
            FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, errorMsg, null);

            // NOTE: It's vital to pass the client id, *not* the component id, to addMessage().
            facesContext.addMessage(input.getClientId(facesContext), facesMsg);
        }
    }

    // <h:outputLabel value="DISPLAY_NAME" styleClass="..." />
    private void addPropertyDisplayName(UIComponent parent, PropertyDefinition propertyDefinition) {
        FacesComponentUtility.addOutputText(parent, this.config, propertyDefinition.getDisplayName(),
            PROPERTY_DISPLAY_NAME_TEXT_STYLE_CLASS);
        if (!this.config.isReadOnly() && propertyDefinition.isRequired()
            && (propertyDefinition instanceof PropertyDefinitionSimple)) {
            // Print a required marker next to required simples.
            // Ignore the required field for maps and lists, as it is has no significance for them.
            FacesComponentUtility.addOutputText(parent, this.config, " * ", REQUIRED_MARKER_TEXT_STYLE_CLASS);
        }
    }

    private void addPropertyDescription(UIComponent parent, PropertyDefinition propertyDefinition) {
        // <span class="description">DESCRIPTION</span>
        if (propertyDefinition.getDescription() != null) {
            FacesComponentUtility.addOutputText(parent, this.config, propertyDefinition.getDescription(),
                DESCRIPTION_STYLE_CLASS);
        }
    }

    private void addPropertyOverrideControl(UIComponent parent, PropertyDefinitionSimple propertyDefinitionSimple,
        UIInput valueInput, HtmlSelectBooleanCheckbox unsetCheckbox) {
        HtmlSelectBooleanCheckbox overrideCheckbox = createInputOverrideForSimpleProperty(propertyDefinitionSimple);
        parent.getChildren().add(overrideCheckbox);
        overrideCheckbox.setValue(isOverride(propertyDefinitionSimple));
        if (isReadOnly(propertyDefinitionSimple)) {
            FacesComponentUtility.setDisabled(overrideCheckbox, true);
        }

        /* TODO: use unsetCheckbox and valueInput to implement the following logic against dom elements in javascript:
         *
         * if override input is not checked, disable unsetCheckbox and dissble valueInput dom elements   * if override
         * input is checked    , enabled unsetCheckbox   ** only cascade enable valueInput dom elements if the
         * unsetCheckbox is NOT unset
         */
        StringBuilder onchange = new StringBuilder();
        for (String valueInputHtmlDomReference : ConfigRenderer.getHtmlDomReferences(valueInput)) {
            onchange.append("setInputOverride(").append(valueInputHtmlDomReference).append(", this.checked);");
        }

        overrideCheckbox.setOnchange(onchange.toString());
    }

    private void addPropertyUnsetControl(UIComponent parent, PropertyDefinitionSimple propertyDefinitionSimple,
        UIInput valueInput, HtmlSelectBooleanCheckbox unsetCheckbox) {
        if (!propertyDefinitionSimple.isRequired()) {
            parent.getChildren().add(unsetCheckbox);
            unsetCheckbox.setValue(String.valueOf(isUnset(propertyDefinitionSimple)));
            if (isReadOnly(propertyDefinitionSimple)) {
                FacesComponentUtility.setDisabled(unsetCheckbox, true);
            } else {
                // Add JavaScript that will disable/enable the corresponding input element when the unset checkbox is
                // checked/unchecked.
                // IMPORTANT: We must use document.formName.inputName, rather than document.getElementById('inputId'),
                //            to reference the HTML DOM element, because the id of the HTML DOM input element is not the same as the
                //            id of the corresponding JSF input component in some cases (e.g. radio buttons). However, the
                //            name property that JSF renders on the HTML DOM input element does always match the JSF
                //            component id. (ips, 05/31/07)
                StringBuilder onchange = new StringBuilder();
                for (String htmlDomReference : ConfigRenderer.getHtmlDomReferences(valueInput)) {
                    onchange.append("setInputUnset(").append(htmlDomReference).append(", this.checked);");
                }

                unsetCheckbox.setOnchange(onchange.toString());
            }
        }
    }

    private UIInput createInputForBooleanProperty() {
        // <h:selectOneRadio id="#{identifier}" value="#{beanValue}" layout="pageDirection" styleClass="radiolabels">
        //    <f:selectItems value="#{itemValues}"></f:selectItems>
        // </h:selectOneRadio>
        HtmlSelectOneRadio selectOneRadio = FacesComponentUtility
            .createComponent(HtmlSelectOneRadio.class, this.config);
        selectOneRadio.setLayout("lineDirection");
        // TODO: We may want to use CSS to get less space between the radio buttons
        //      (see http://jira.jboss.com/jira/browse/JBMANCON-21).

        UISelectItem selectItem = FacesComponentUtility.createComponent(UISelectItem.class, this.config);
        selectItem.setItemLabel("Yes");
        selectItem.setItemValue("true");
        selectOneRadio.getChildren().add(selectItem);
        selectItem = FacesComponentUtility.createComponent(UISelectItem.class, this.config);
        selectItem.setItemLabel("No");
        selectItem.setItemValue("false");
        selectOneRadio.getChildren().add(selectItem);
        return selectOneRadio;
    }

    // <h:selectOneRadio id="#{identifier}" value="#{beanValue}" layout="pageDirection" styleClass="radiolabels">
    //    <f:selectItems value="#{itemValues}"></f:selectItems>
    // </h:selectOneRadio>
    private UIInput createInputForEnumProperty(PropertyDefinitionSimple propertyDefinitionSimple) {
        UISelectOne selectOne;
        if (propertyDefinitionSimple.getEnumeratedValues().size() >= LISTBOX_THRESHOLD_ENUM_SIZE) {
            // Use a drop down menu for larger enums...
            HtmlSelectOneMenu menu = FacesComponentUtility.createComponent(HtmlSelectOneMenu.class, this.config);

            // TODO: Use CSS to set the width of the menu.
            selectOne = menu;
        } else {
            // ...and a radio for smaller ones.
            HtmlSelectOneRadio radio = FacesComponentUtility.createComponent(HtmlSelectOneRadio.class, this.config);
            radio.setLayout("pageDirection");

            // TODO: We may want to use CSS to get less space between the radio buttons
            //      (see http://jira.jboss.com/jira/browse/JBMANCON-21).
            selectOne = radio;
        }

        List<PropertyDefinitionEnumeration> options = propertyDefinitionSimple.getEnumeratedValues();
        for (PropertyDefinitionEnumeration option : options) {
            UISelectItem selectItem = FacesComponentUtility.createComponent(UISelectItem.class, this.config);
            selectItem.setItemLabel(option.getName());
            selectItem.setItemValue(option.getValue());
            selectOne.getChildren().add(selectItem);
        }

        return selectOne;
    }

    private UIInput createInputForStringProperty() {
        HtmlInputText inputText = FacesComponentUtility.createComponent(HtmlInputText.class, this.config);

        //TODO: check if this has units, then apply the correct style
        inputText.setStyle(INPUT_TEXT_WIDTH_STYLE);
        //      inputText.setStyle(INPUT_TEXT_WIDTH_STYLE_WITH_UNITS);
        inputText.setMaxlength(PropertySimple.MAX_VALUE_LENGTH);

        // Disable browser auto-completion.
        inputText.setAutocomplete("off");
        return inputText;
    }

    private UIInput createInputForPasswordProperty() {
        HtmlInputSecret inputSecret = FacesComponentUtility.createComponent(HtmlInputSecret.class, this.config);
        inputSecret.setStyle(INPUT_TEXT_WIDTH_STYLE);
        inputSecret.setMaxlength(PropertySimple.MAX_VALUE_LENGTH);

        // TODO: Remove the below line, as it's not secure, and improve support for displaying/validating password fields.
        inputSecret.setRedisplay(true);

        // Disable browser auto-completion.
        inputSecret.setAutocomplete("off");
        return inputSecret;
    }

    private UIInput createInputForLongStringProperty() {
        HtmlInputTextarea inputTextarea = FacesComponentUtility.createComponent(HtmlInputTextarea.class, this.config);
        inputTextarea.setRows(INPUT_TEXTAREA_COMPONENT_ROWS);
        inputTextarea.setStyle(INPUT_TEXT_WIDTH_STYLE);
        return inputTextarea;
    }

    /**
     * Binds the value of the specified UIInput to an EL expression corresponding to the Configuration property's
     * override attribute with the specified name.
     */
    @SuppressWarnings("deprecation")
    private void setInputOverrideExpression(HtmlSelectBooleanCheckbox input, String propertyName) {
        // e.g.: #{configuration.simpleProperties['useJavaContext'].override}
        String expression = String.format(this.overrideExpressionFormat, propertyName);
        ValueExpression valueExpression = FacesExpressionUtility.createValueExpression(expression, Boolean.class);
        input.setValueExpression("value", valueExpression);
    }

    /**
     * Binds the value of the specified UIInput to an EL expression corresponding to the Configuration property with the
     * specified name.
     */
    // TODO: Add support for properties inside lists.
    @SuppressWarnings( { "JavaDoc" })
    private void setInputValueExpression(UIInput input, String propertyName) {
        // e.g.: #{configuration.simpleProperties['useJavaContext'].stringValue}
        String expression = String.format(this.valueExpressionFormat, propertyName);
        ValueExpression valueExpression = FacesExpressionUtility.createValueExpression(expression, String.class);
        input.setValueExpression("value", valueExpression);
    }

    private Boolean isOverride(PropertyDefinition propertyDefinition) {
        if (!(propertyDefinition instanceof PropertyDefinitionSimple)) {
            return false;
        }

        Property property = this.propertyMap.get(propertyDefinition.getName());
        if (property == null) {
            return false;
        }

        Boolean override = ((PropertySimple) property).getOverride();
        return ((override != null) && override);
    }

    private boolean isReadOnly(PropertyDefinition propertyDefinition) {
        // a fully editable config overrides any other means of setting read only
        return (!this.config.isFullyEditable() && (this.config.isReadOnly() || (propertyDefinition.isReadOnly() && !isUndefinedRequiredProperty(propertyDefinition))));
    }

    private boolean isUndefinedRequiredProperty(PropertyDefinition propertyDefinition) {
        boolean isUndefinedRequiredProperty = false;
        if ((propertyDefinition instanceof PropertyDefinitionSimple) && propertyDefinition.isRequired()) {
            PropertySimple propertySimple = this.propertyMap.getSimple(propertyDefinition.getName());
            if ((propertySimple.getStringValue() == null) || propertySimple.getStringValue().equals("")) {
                // Required properties with a value of null or "" should never be set to read-only, otherwise the user
                // will have no way to give the property a value and thereby get things to a valid state.
                isUndefinedRequiredProperty = true;
            }
        }

        return isUndefinedRequiredProperty;
    }

    private boolean isUnset(PropertyDefinition propertyDefinition) {
        if (propertyDefinition instanceof PropertyDefinitionSimple) {
            return !propertyDefinition.isRequired()
                && ((this.propertyMap.get(propertyDefinition.getName()) == null) || (this.propertyMap.getSimple(
                    propertyDefinition.getName()).getStringValue() == null));
        } else {
            // For now, maps and lists cannot be unset (doesn't make much sense).
            return false;
        }
    }

    static String unwrapExpressionString(String configurationExpressionString) {
        return configurationExpressionString.substring(2, configurationExpressionString.length() - 1);
    }

    /**
     * simple method that will add debugging HTML comments so that I can figure out which part of the HTML is generated
     * by which part of this class
     *
     * @param component  the component this comment should be added
     * @param start      true if this is the "START" comment, false if it is the "END" comment
     * @param methodName the name of the method this is calling from
     */
    private void addDebug(UIComponent component, boolean start, String methodName) {
        if (LOG.isDebugEnabled()) {
            StringBuilder msg = new StringBuilder("\n<!--");
            msg.append(start ? " START " : " END ");
            msg.append(this.getClass().getSimpleName());
            msg.append(methodName);
            msg.append(" -->\n ");
            FacesComponentUtility.addVerbatimText(component, msg);
        }
    }
}