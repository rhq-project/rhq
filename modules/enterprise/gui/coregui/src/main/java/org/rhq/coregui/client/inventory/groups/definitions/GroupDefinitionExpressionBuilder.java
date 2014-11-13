/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.coregui.client.inventory.groups.definitions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.core.domain.criteria.Criteria.Restriction;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.ResourceTypeUtility;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.components.form.SortedSelectItem;
import org.rhq.coregui.client.components.selector.SingleResourceGroupSelectorItem;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.GroupDefinitionExpressionBuilderGWTServiceAsync;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * A dialog window that lets you build custom dynagroup expressions.
 * 
 * @author John Mazzitelli
 */
public class GroupDefinitionExpressionBuilder extends Window {

    protected Messages MSG = CoreGUI.getMessages();

    private AddExpressionHandler addExpressionHandler;

    private TextItem valueItem;
    private SelectItem compareTypeItem;
    private RadioGroupItem unsetItem;
    private SelectItem propertyNameItem;
    private SelectItem resourceTypeItem;
    private SelectItem pluginItem;
    private SelectItem expressionTypeItem;
    private SelectItem resourceItem;
    private RadioGroupItem groupByItem;
    private RadioGroupItem memberOfItem;
    private SingleResourceGroupSelectorItem memberOfGroupItem;
    private TextAreaItem expressionItem;

    private ArrayList<String> plugins = new ArrayList<String>();
    private HashMap<String, Integer> resourceTypeIds = new HashMap<String, Integer>();

    /**
     * Creates the dialog but does not show it. <code>expression</code> is
     * the form field whose value will be set to the expression string
     * when the user finishes building it.
     * 
     * @param expressionItem form item whose value will be the expression that is built
     * @param templateItem this is the template drop down item that will be cleared if an expression
     *                     is added - this is so the user isn't confused thinking the new expression
     *                     is now the value of the template text 
     */
    public GroupDefinitionExpressionBuilder(AddExpressionHandler addExpressionHandler) {
        super();

        this.addExpressionHandler = addExpressionHandler;

        setIsModal(false);
        setShowModalMask(false);
        setShowMinimizeButton(true);
        setShowMaximizeButton(true);
        setShowCloseButton(true);
        setAutoSize(true);
        setCanDragResize(true);
        setAutoCenter(true);
        centerInPage();
        addCloseClickHandler(new CloseClickHandler() {
            @Override
            public void onCloseClick(CloseClickEvent event) {
                closeExpressionBuilder();
            }
        });
    }

    @Override
    protected void onInit() {
        super.onInit();

        setTitle(MSG.view_dynagroup_exprBuilder_title());

        // build the individual components
        this.valueItem = new TextItem("value", MSG.common_title_value());
        this.valueItem.setTooltip(MSG.view_dynagroup_exprBuilder_value_tooltip());
        this.valueItem.setWidth(200);
        this.valueItem.setHoverWidth(250);
        this.valueItem.setDefaultValue("");
        this.valueItem.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                buildExpressionValue();
            }
        });

        this.compareTypeItem = new SortedSelectItem("compareType", MSG.view_dynagroup_exprBuilder_comparisonType());
        this.compareTypeItem.setTooltip(MSG.view_dynagroup_exprBuilder_comparisonType_tooltip());
        this.compareTypeItem.setHoverWidth(250);
        this.compareTypeItem.setRedrawOnChange(true);
        this.compareTypeItem.setWidth("*");
        this.compareTypeItem.setDefaultToFirstOption(true);
        this.compareTypeItem.setValueMap(MSG.view_dynagroup_exprBuilder_comparisonType_equals(), //
            MSG.view_dynagroup_exprBuilder_comparisonType_startsWith(), //
            MSG.view_dynagroup_exprBuilder_comparisonType_endsWith(), //
            MSG.view_dynagroup_exprBuilder_comparisonType_contains());
        this.compareTypeItem.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                buildExpressionValue();
            }
        });

        this.unsetItem = new RadioGroupItem("unset", MSG.view_dynagroup_exprBuilder_unset());
        this.unsetItem.setTooltip(MSG.view_dynagroup_exprBuilder_unset_tooltip());
        this.unsetItem.setHoverWidth(250);
        this.unsetItem.setDefaultValue(MSG.common_val_no());
        this.unsetItem.setValueMap(MSG.common_val_yes(), MSG.common_val_no());
        this.unsetItem.setVertical(false);
        this.unsetItem.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                enableDisableComponents();
                buildExpressionValue();
            }
        });

        this.propertyNameItem = new SelectItem("propertyName", MSG.view_dynagroup_exprBuilder_propertyName());
        this.propertyNameItem.setTooltip(MSG.view_dynagroup_exprBuilder_propertyName_tooltip());
        this.propertyNameItem.setHoverWidth(250);
        this.propertyNameItem.setRedrawOnChange(true);
        this.propertyNameItem.setWidth("*");
        this.propertyNameItem.setDefaultToFirstOption(true);
        this.propertyNameItem.setEmptyDisplayValue(MSG.view_dynagroup_exprBuilder_noProperties());
        this.propertyNameItem.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                buildExpressionValue();
            }
        });

        this.resourceTypeItem = new SortedSelectItem("resourceType", MSG.view_dynagroup_exprBuilder_resourceType());
        this.resourceTypeItem.setTooltip(MSG.view_dynagroup_exprBuilder_resourceType_tooltip());
        this.resourceTypeItem.setHoverWidth(250);
        this.resourceTypeItem.setRedrawOnChange(true);
        this.resourceTypeItem.setWidth("*");
        this.resourceTypeItem.setDefaultToFirstOption(true);
        this.resourceTypeItem.setEmptyDisplayValue(MSG.view_dynagroup_exprBuilder_noResourceTypes());
        this.resourceTypeItem.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                loadPropertyNameDropDown();
            }
        });

        this.pluginItem = new SortedSelectItem("plugin", MSG.view_dynagroup_exprBuilder_definingPlugin());
        this.pluginItem.setTooltip(MSG.view_dynagroup_exprBuilder_definingPlugin_tooltip());
        this.pluginItem.setHoverWidth(250);
        this.pluginItem.setRedrawOnChange(true);
        this.pluginItem.setWidth("*");
        this.pluginItem.setDefaultToFirstOption(true);
        this.pluginItem.setEmptyDisplayValue(MSG.view_dynagroup_exprBuilder_noPlugins());
        this.pluginItem.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                Object newValue = event.getValue();
                if (newValue != null) {
                    loadResourceTypeDropDown(newValue.toString());
                }
            }
        });
        GWTServiceLookup.getPluginService().getAgentPlugins(false, new AsyncCallback<ArrayList<Plugin>>() {
            @Override
            public void onSuccess(ArrayList<Plugin> result) {
                plugins.clear();
                for (Plugin plugin : result) {
                    if (plugin.isEnabled()) {
                        plugins.add(plugin.getName());
                    }
                }
                Collections.sort(plugins);
                pluginItem.setValueMap(plugins.toArray(new String[plugins.size()]));
                pluginItem.clearValue();

                // do the initial population of the resource type drop down 
                if (!plugins.isEmpty()) {
                    loadResourceTypeDropDown(plugins.get(0));
                }

                markForRedraw();
            }

            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_dynagroup_exprBuilder_pluginLoadFailure(), caught);
            }
        });

        this.expressionTypeItem = new SortedSelectItem("expressionType",
            MSG.view_dynagroup_exprBuilder_expressionType());
        this.expressionTypeItem.setTooltip(MSG.view_dynagroup_exprBuilder_expressionType_tooltip());
        this.expressionTypeItem.setHoverWidth(250);
        this.expressionTypeItem.setRedrawOnChange(true);
        this.expressionTypeItem.setWidth("*");
        this.expressionTypeItem.setDefaultToFirstOption(true);
        this.expressionTypeItem.setValueMap(MSG.view_dynagroup_exprBuilder_expressionType_resource(), //
            MSG.view_dynagroup_exprBuilder_expressionType_resourceType(), //
            MSG.view_dynagroup_exprBuilder_expressionType_resourceCategory(), //
            MSG.view_dynagroup_exprBuilder_expressionType_trait(), //
            MSG.view_dynagroup_exprBuilder_expressionType_pluginConfig(), //
            MSG.view_dynagroup_exprBuilder_expressionType_resourceConfig());
        this.expressionTypeItem.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                enableDisableComponents();
                loadPropertyNameDropDown();
            }
        });

        this.resourceItem = new SortedSelectItem("resource", MSG.view_dynagroup_exprBuilder_resource());
        this.resourceItem.setTooltip(MSG.view_dynagroup_exprBuilder_resource_tooltip());
        this.resourceItem.setHoverWidth(250);
        this.resourceItem.setRedrawOnChange(true);
        this.resourceItem.setWidth("*");
        this.resourceItem.setDefaultToFirstOption(true);
        this.resourceItem.setValueMap(MSG.view_dynagroup_exprBuilder_resource_resource(), //
            MSG.view_dynagroup_exprBuilder_resource_child(), //
            MSG.view_dynagroup_exprBuilder_resource_parent(), //
            MSG.view_dynagroup_exprBuilder_resource_grandparent(), //
            MSG.view_dynagroup_exprBuilder_resource_greatGrandparent(), //
            MSG.view_dynagroup_exprBuilder_resource_greatGreatGrandparent());
        this.resourceItem.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                buildExpressionValue();
            }
        });

        this.memberOfItem = new RadioGroupItem("memberOf", MSG.view_dynagroup_exprBuilder_memberOf());
        this.memberOfItem.setTooltip(MSG.view_dynagroup_exprBuilder_memberOf_tooltip());
        this.memberOfItem.setHoverWidth(250);
        this.memberOfItem.setDefaultValue(MSG.common_val_no());
        this.memberOfItem.setValueMap(MSG.common_val_yes(), MSG.common_val_no());
        this.memberOfItem.setVertical(false);
        this.memberOfItem.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                enableDisableComponents();
                buildExpressionValue();
            }
        });

        this.memberOfGroupItem = new SingleResourceGroupSelectorItem("memberOfGroup", MSG.common_title_group(), null);
        this.memberOfGroupItem.setTooltip(MSG.view_dynagroup_exprBuilder_groupBy_tooltip());
        this.memberOfGroupItem.setHoverWidth(250);
        this.memberOfGroupItem.setRedrawOnChange(true);
        this.memberOfGroupItem.setWidth("*");
        this.memberOfGroupItem.setDefaultToFirstOption(true);
        this.memberOfGroupItem.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                buildExpressionValue();
            }
        });

        this.groupByItem = new RadioGroupItem("groupBy", MSG.view_dynagroup_exprBuilder_groupBy());
        this.groupByItem.setTooltip(MSG.view_dynagroup_exprBuilder_groupBy_tooltip());
        this.groupByItem.setHoverWidth(250);
        this.groupByItem.setDefaultValue(MSG.common_val_no());
        this.groupByItem.setValueMap(MSG.common_val_yes(), MSG.common_val_no());
        this.groupByItem.setVertical(false);
        this.groupByItem.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                enableDisableComponents();
                buildExpressionValue();
            }
        });

        this.expressionItem = new TextAreaItem("expression", MSG.view_dynagroup_exprBuilder_expression());
        this.expressionItem.setTooltip(MSG.view_dynagroup_exprBuilder_expression_tooltip());
        this.expressionItem.setHoverWidth(250);
        this.expressionItem.setHeight(75);
        this.expressionItem.setWidth("250");
        this.expressionItem.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                event.cancel(); // do not allow the use to edit this field, its read only
            }
        });

        // build the add/close buttons
        ButtonItem addButton = new ButtonItem("add", MSG.view_dynagroup_exprBuilder_addExpression());
        addButton.setColSpan(2);
        addButton.setAlign(Alignment.CENTER);
        addButton.setStartRow(true);
        addButton.setEndRow(true);
        addButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                addExpression();
            }
        });
        ButtonItem closeButton = new ButtonItem("close", MSG.common_button_close());
        closeButton.setColSpan(2);
        closeButton.setAlign(Alignment.CENTER);
        closeButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                closeExpressionBuilder();
            }
        });
        closeButton.setStartRow(true);
        closeButton.setEndRow(true);

        // stitch together the UI
        DynamicForm form = new DynamicForm();
        form.setWrapItemTitles(false);
        form.setAutoWidth();
        form.setAutoHeight();
        form.setCellPadding(5);
        form.setFields(this.expressionItem, this.memberOfItem, this.memberOfGroupItem, this.groupByItem,
            this.resourceItem, this.expressionTypeItem, this.pluginItem, this.resourceTypeItem, this.propertyNameItem,
            this.unsetItem, this.compareTypeItem, this.valueItem, addButton, closeButton);

        EnhancedVLayout layout = new EnhancedVLayout();
        layout.setLayoutMargin(5);
        layout.setAutoHeight();
        layout.setAutoWidth();
        layout.addMember(form);
        addItem(layout);

        // initially enable/disable components as appropriate
        enableDisableComponents();
    }

    private void closeExpressionBuilder() {
        markForDestroy();
    }

    private void addExpression() {
        String valueAsString = this.expressionItem.getValueAsString();
        if (valueAsString != null && valueAsString.trim().length() > 0) {
            this.addExpressionHandler.addExpression(valueAsString);
        }
    }

    /**
     * Handler that is called when the user uses this expression builder to
     * add a new expression.
     */
    public interface AddExpressionHandler {
        /**
         * Called when the user adds a new expression.
         */
        public void addExpression(String newExpression);
    }

    private boolean shouldMemberOfGroupBeDisabled() {
        String expressionType = this.expressionTypeItem.getValueAsString();

        if (MSG.common_val_no().equals(this.memberOfItem.getValueAsString())) {
            return true;
        }
        return false; // they are enabled otherwise
    }

    private boolean shouldResourceAndExpressionTypeBeDisabled() {
        String expressionType = this.expressionTypeItem.getValueAsString();

        if (MSG.common_val_yes().equals(this.memberOfItem.getValueAsString())) {
            return true;
        }
        return false; // they are enabled otherwise
    }

    private boolean shouldPluginAndResourceTypeBeDisabled() {
        String expressionType = this.expressionTypeItem.getValueAsString();

        if (MSG.common_val_yes().equals(this.memberOfItem.getValueAsString())) {
            return true;
        }
        if (MSG.view_dynagroup_exprBuilder_expressionType_resource().equals(expressionType)) {
            return true;
        }
        if (MSG.view_dynagroup_exprBuilder_expressionType_resourceCategory().equals(expressionType)) {
            return true;
        }
        if ((MSG.view_dynagroup_exprBuilder_expressionType_resourceType().equals(expressionType))
            && (MSG.common_val_yes().equals(groupByItem.getValueAsString()))) {
            return true;
        }

        return false; // they are enabled otherwise
    }

    private boolean shouldComparisonTypeAndValueBeDisabled() {
        boolean memberOf = MSG.common_val_yes().equals(this.memberOfItem.getValueAsString());
        boolean groupBy = MSG.common_val_yes().equals(this.groupByItem.getValueAsString());
        boolean unset = MSG.common_val_yes().equals(this.unsetItem.getValueAsString());
        String expressionType = this.expressionTypeItem.getValueAsString();

        if (groupBy || memberOf || unset) {
            return true;
        }
        if (MSG.view_dynagroup_exprBuilder_expressionType_resourceType().equals(expressionType)) {
            return true;
        }

        return false; // they are enabled otherwise
    }

    private boolean shouldGroupByBeDisabled() {
        boolean memberOf = MSG.common_val_yes().equals(this.memberOfItem.getValueAsString());
        boolean unset = MSG.common_val_yes().equals(this.unsetItem.getValueAsString());

        if (memberOf || unset) {
            return true;
        }
        return false; // enabled otherwise
    }

    private boolean shouldUnsetBeDisabled() {
        boolean memberOf = MSG.common_val_yes().equals(this.memberOfItem.getValueAsString());
        boolean groupBy = MSG.common_val_yes().equals(this.groupByItem.getValueAsString());

        if (groupBy || memberOf) {
            return true;
        }
        return false; // enabled otherwise
    }

    private boolean shouldPropertyNameBeDisabled() {
        boolean memberOf = MSG.common_val_yes().equals(this.memberOfItem.getValueAsString());
        String expressionType = this.expressionTypeItem.getValueAsString();

        if (memberOf) {
            return true;
        }
        if (MSG.view_dynagroup_exprBuilder_expressionType_resourceType().equals(expressionType)) {
            return true;
        } else if (MSG.view_dynagroup_exprBuilder_expressionType_resourceCategory().equals(expressionType)) {
            return true;
        }
        return false; // enabled otherwise
    }

    private void loadResourceTypeDropDown(final String newPlugin) {
        ResourceTypeCriteria criteria = new ResourceTypeCriteria();
        criteria.addFilterIgnored(false); // make sure we don't let users pick a type that is ignored
        criteria.addFilterPluginName(newPlugin);
        criteria.setPageControl(PageControl.getUnlimitedInstance());
        criteria.setRestriction(Restriction.COLLECTION_ONLY);
        criteria.setStrict(true);
        GWTServiceLookup.getResourceTypeGWTService().findResourceTypesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceType>>() {
                @Override
                public void onSuccess(PageList<ResourceType> result) {
                    resourceTypeIds.clear();
                    if (result != null && !result.isEmpty()) {
                        ArrayList<String> typeNames = new ArrayList<String>();
                        for (ResourceType type : result) {
                            String typeName = ResourceTypeUtility.displayName(type);
                            typeNames.add(typeName);
                            resourceTypeIds.put(typeName, Integer.valueOf(type.getId()));
                        }
                        resourceTypeItem.setValueMap(typeNames.toArray(new String[typeNames.size()]));
                    } else {
                        resourceTypeItem.setValueMap(new String[0]);
                    }
                    resourceTypeItem.clearValue();
                    loadPropertyNameDropDown();
                    markForRedraw();
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_dynagroup_exprBuilder_resTypeLoadFailure(newPlugin),
                        caught);
                }
            });
    }

    // this will also build and populate the expression text item
    private void loadPropertyNameDropDown() {
        String expressionType = this.expressionTypeItem.getValueAsString();
        String resourceType = this.resourceTypeItem.getValueAsString();
        Integer resourceTypeId = this.resourceTypeIds.get(resourceType);

        propertyNameItem.setValueMap(new String[0]);
        propertyNameItem.clearValue();

        if (MSG.view_dynagroup_exprBuilder_expressionType_resource().equals(expressionType)) {
            propertyNameItem.setValueMap(new String[] { "id", "name", "version", "availability" });
            propertyNameItem.clearValue();
            buildExpressionValue();
        } else if (resourceTypeId != null) {

            GroupDefinitionExpressionBuilderGWTServiceAsync service;
            service = GWTServiceLookup.getGroupDefinitionExpressionBuilderService();

            if (MSG.view_dynagroup_exprBuilder_expressionType_trait().equals(expressionType)) {
                service.getTraitPropertyNames(resourceTypeId.intValue(), new AsyncCallback<ArrayList<String>>() {
                    @Override
                    public void onSuccess(ArrayList<String> result) {
                        if (result != null) {
                            propertyNameItem.setValueMap(result.toArray(new String[result.size()]));
                        }
                        propertyNameItem.clearValue();
                        buildExpressionValue();
                        markForRedraw();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_dynagroup_exprBuilder_propLoadFailure(), caught);
                    }
                });
            } else if (MSG.view_dynagroup_exprBuilder_expressionType_pluginConfig().equals(expressionType)) {
                service.getPluginConfigurationPropertyNames(resourceTypeId.intValue(),
                    new AsyncCallback<ArrayList<String>>() {
                        @Override
                        public void onSuccess(ArrayList<String> result) {
                            if (result != null) {
                                propertyNameItem.setValueMap(result.toArray(new String[result.size()]));
                            }
                            propertyNameItem.clearValue();
                            buildExpressionValue();
                            markForRedraw();
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_dynagroup_exprBuilder_propLoadFailure(),
                                caught);
                        }
                    });
            } else if (MSG.view_dynagroup_exprBuilder_expressionType_resourceConfig().equals(expressionType)) {
                service.getResourceConfigurationPropertyNames(resourceTypeId.intValue(),
                    new AsyncCallback<ArrayList<String>>() {
                        @Override
                        public void onSuccess(ArrayList<String> result) {
                            if (result != null) {
                                propertyNameItem.setValueMap(result.toArray(new String[result.size()]));
                            }
                            propertyNameItem.clearValue();
                            buildExpressionValue();
                            markForRedraw();
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_dynagroup_exprBuilder_propLoadFailure(),
                                caught);
                        }
                    });
            } else {
                buildExpressionValue();
            }
        } else {
            buildExpressionValue();
        }

        markForRedraw();
        return;
    }

    private void enableDisableComponents() {
        boolean memberOfGroupDisabled = shouldMemberOfGroupBeDisabled();
        boolean resourceAndExpressionTypeDisabled = shouldResourceAndExpressionTypeBeDisabled();
        boolean pluginAndResourceTypeDisabled = shouldPluginAndResourceTypeBeDisabled();
        boolean comparisonTypeAndValueDisabled = shouldComparisonTypeAndValueBeDisabled();
        boolean groupByDisabled = shouldGroupByBeDisabled();
        boolean unsetDisabled = shouldUnsetBeDisabled();
        boolean propertyNameDisabled = shouldPropertyNameBeDisabled();

        this.memberOfGroupItem.setDisabled(memberOfGroupDisabled);
        this.resourceItem.setDisabled(resourceAndExpressionTypeDisabled);
        this.expressionTypeItem.setDisabled(resourceAndExpressionTypeDisabled);
        this.pluginItem.setDisabled(pluginAndResourceTypeDisabled);
        this.resourceTypeItem.setDisabled(pluginAndResourceTypeDisabled);
        this.compareTypeItem.setDisabled(comparisonTypeAndValueDisabled);
        this.valueItem.setDisabled(comparisonTypeAndValueDisabled);
        this.groupByItem.setDisabled(groupByDisabled);
        this.unsetItem.setDisabled(unsetDisabled);
        this.propertyNameItem.setDisabled(propertyNameDisabled);

        markForRedraw();
    }

    private void buildExpressionValue() {

        boolean memberOf = MSG.common_val_yes().equals(this.memberOfItem.getValueAsString());
        boolean groupBy = MSG.common_val_yes().equals(groupByItem.getValueAsString());
        boolean unset = MSG.common_val_yes().equals(unsetItem.getValueAsString());

        StringBuilder buf = new StringBuilder();

        if (memberOf) {
            buf.append("memberof = " + memberOfGroupItem.getDisplayValue());

        } else {

            if (groupBy) {
                buf.append("groupby ");
            }
            if (unset) {
                buf.append("empty ");
            }

            buf.append("resource.");

            String resourceLevel = resourceItem.getValueAsString();
            if (MSG.view_dynagroup_exprBuilder_resource_resource().equals(resourceLevel)) {
                // do nothing
            } else if (MSG.view_dynagroup_exprBuilder_resource_child().equals(resourceLevel)) {
                buf.append("child.");
            } else if (MSG.view_dynagroup_exprBuilder_resource_parent().equals(resourceLevel)) {
                buf.append("parent.");
            } else if (MSG.view_dynagroup_exprBuilder_resource_grandparent().equals(resourceLevel)) {
                buf.append("grandParent.");
            } else if (MSG.view_dynagroup_exprBuilder_resource_greatGrandparent().equals(resourceLevel)) {
                buf.append("greatGrandParent.");
            } else if (MSG.view_dynagroup_exprBuilder_resource_greatGreatGrandparent().equals(resourceLevel)) {
                buf.append("greatGreatGrandParent.");
            }

            String eType = expressionTypeItem.getValueAsString();
            if (MSG.view_dynagroup_exprBuilder_expressionType_resource().equals(eType)) {
                buf.append(propertyNameItem.getValueAsString());
            } else if (MSG.view_dynagroup_exprBuilder_expressionType_resourceType().equals(eType)) {
                buf.append("type.plugin");
            } else if (MSG.view_dynagroup_exprBuilder_expressionType_resourceCategory().equals(eType)) {
                buf.append("type.category");
            } else if (MSG.view_dynagroup_exprBuilder_expressionType_trait().equals(eType)) {
                buf.append("trait[" + propertyNameItem.getValueAsString() + "]");
            } else if (MSG.view_dynagroup_exprBuilder_expressionType_pluginConfig().equals(eType)) {
                buf.append("pluginConfiguration[" + propertyNameItem.getValueAsString() + "]");
            } else if (MSG.view_dynagroup_exprBuilder_expressionType_resourceConfig().equals(eType)) {
                buf.append("resourceConfiguration[" + propertyNameItem.getValueAsString() + "]");
            }

            if (!groupBy && !unset) {

                String selectedComparison = compareTypeItem.getValueAsString();
                if (MSG.view_dynagroup_exprBuilder_comparisonType_equals().equals(selectedComparison)) {
                    // do nothing
                } else if (MSG.view_dynagroup_exprBuilder_comparisonType_contains().equals(selectedComparison)) {
                    buf.append(".contains");
                } else if (MSG.view_dynagroup_exprBuilder_comparisonType_startsWith().equals(selectedComparison)) {
                    buf.append(".startsWith");
                } else if (MSG.view_dynagroup_exprBuilder_comparisonType_endsWith().equals(selectedComparison)) {
                    buf.append(".endsWith");
                }

                buf.append(" = ");

                if (MSG.view_dynagroup_exprBuilder_expressionType_resource().equals(eType)
                    || MSG.view_dynagroup_exprBuilder_expressionType_resourceCategory().equals(eType)) {
                    buf.append(valueItem.getValueAsString());
                } else if (MSG.view_dynagroup_exprBuilder_expressionType_resourceType().equals(eType)) {
                    String d = buf.toString();
                    buf.append(pluginItem.getValueAsString());
                    buf.append("\n");
                    buf.append(d.replaceAll("plugin", "name"));
                    buf.append(resourceTypeItem.getValueAsString());
                } else if (MSG.view_dynagroup_exprBuilder_expressionType_trait().equals(eType)
                    || MSG.view_dynagroup_exprBuilder_expressionType_pluginConfig().equals(eType)
                    || MSG.view_dynagroup_exprBuilder_expressionType_resourceConfig().equals(eType)) {
                    buf.append(valueItem.getValueAsString());
                }
            } else if (MSG.view_dynagroup_exprBuilder_expressionType_resourceType().equals(eType)) {
                String d = buf.toString();
                buf.append("\n");
                buf.append(d.replaceAll("plugin", "name"));
            }
        }

        String expressionValueString = buf.toString();
        expressionItem.setValue(expressionValueString);
        markForRedraw();
        return;
    }
}
