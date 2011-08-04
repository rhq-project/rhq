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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.definitions;

import static org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupDataSourceField.CATEGORY;
import static org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupDataSourceField.DESCRIPTION;
import static org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupDataSourceField.NAME;
import static org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupDataSourceField.PLUGIN;
import static org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupDataSourceField.TYPE;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.DSOperationType;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.SpinnerItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.FormItemClickHandler;
import com.smartgwt.client.widgets.form.fields.events.FormItemIconClickEvent;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.ResourceGroupDefinitionCriteria;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoadedListener;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoader;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.table.EscapedHtmlCellFormatter;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupsDataSource;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.definitions.GroupDefinitionExpressionBuilder.AddExpressionHandler;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Joseph Marques
 */
public class SingleGroupDefinitionView extends LocatableVLayout implements BookmarkableView {

    private static final String TEMPLATE_JBOSSAS4_CLUSTERS = MSG.view_dynagroup_template_jbossas4_clusters();
    private static final String TEMPLATE_JBOSSAS5_CLUSTERS = MSG.view_dynagroup_template_jbossas5_clusters(); // true for AS 5 and 6
    private static final String TEMPLATE_JBOSSAS4_EAR_CLUSTERS = MSG.view_dynagroup_template_jbossas4_earClusters();
    private static final String TEMPLATE_JBOSSAS4_UNIQUE_VERSIONS = MSG
        .view_dynagroup_template_jbossas4_uniqueVersions();
    private static final String TEMPLATE_PLATFORMS = MSG.view_dynagroup_template_platforms();
    private static final String TEMPLATE_UNIQUE_RESOURCE_TYPES = MSG.view_dynagroup_template_uniqueResourceTypes();
    private static final String TEMPLATE_JBOSSAS4_HOSTING_APP = MSG.view_dynagroup_template_jbossas4_hostingApp();
    private static final String TEMPLATE_JBOSSAS4_NONSECURED = MSG.view_dynagroup_template_jbossas4_nonsecured();
    private static final String TEMPLATE_DOWNED_RESOURCES = MSG.view_dynagroup_template_downedResources();

    private int groupDefinitionId;
    private GroupDefinition groupDefinition;
    private String basePath;

    private GroupDefinitionExpressionBuilder builder;

    // editable form
    private TextItem id;
    private TextItem name;
    private TextAreaItem description;
    private CheckboxItem recursive;
    private SpacerItem templateSelectorTitleSpacer;
    private SelectItem templateSelector;
    private LinkedHashMap<String, String> templateStrings;
    private TextAreaItem expression;
    private SpinnerItem recalculationInterval;

    public SingleGroupDefinitionView(String locatorId) {
        super(locatorId);
        buildForm();
        setWidth100();
    }

    public void setGroupDefinition(final GroupDefinition groupDefinition) {
        this.groupDefinition = groupDefinition;

        // form setup
        id.setValue(groupDefinition.getId());
        name.setValue(groupDefinition.getName());
        recursive.setValue(groupDefinition.isRecursive());
        description.setValue(groupDefinition.getDescription());
        recalculationInterval.setValue(groupDefinition.getRecalculationInterval());
        expression.setValue(groupDefinition.getExpression());

        final LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("GroupDefinitionForm"));
        form.setFields(id, name, description, templateSelectorTitleSpacer, templateSelector, expression, recursive,
            recalculationInterval);
        form.setDataSource(GroupDefinitionDataSource.getInstance());
        form.setHiliteRequiredFields(true);
        form.setRequiredTitleSuffix(" <span style=\"color: red;\">* </span>:");
        DSOperationType saveOperationType = (groupDefinition.getId() == 0) ? DSOperationType.ADD
            : DSOperationType.UPDATE;
        form.setSaveOperationType(saveOperationType);

        final DynaGroupChildrenView dynaGroupChildrenView = new DynaGroupChildrenView(extendLocatorId("DynaGroups"),
            groupDefinitionId);

        // button setup
        IButton saveButton = new LocatableIButton(this.extendLocatorId("save"), MSG.common_button_save());
        //saveButton.addClickHandler(new SaveOrUpdateClickHandler(form, operationType, dynaGroupChildrenView));
        saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                saveForm(form, dynaGroupChildrenView, false);
            }
        });

        IButton recalculateButton = new LocatableIButton(this.extendLocatorId("saveAndRecalculate"), MSG
            .view_dynagroup_saveAndRecalculate());
        recalculateButton.setWidth(150);
        recalculateButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                saveForm(form, dynaGroupChildrenView, true);
            }
        });

        IButton resetButton = new LocatableIButton(this.extendLocatorId("Reset"), MSG.common_button_reset());
        resetButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                form.reset();
            }
        });

        HLayout buttonLayout = new HLayout(10); // margin between members
        buttonLayout.setMargin(10); // margin around layout widget
        buttonLayout.addMember(saveButton);
        buttonLayout.addMember(recalculateButton);
        buttonLayout.addMember(resetButton);

        // canvas setup
        addMember(form);
        addMember(buttonLayout);
        addMember(dynaGroupChildrenView);

        markForRedraw();
    }

    private void saveForm(final LocatableDynamicForm form, final DynaGroupChildrenView dynaGroupChildrenView,
        final boolean recalc) {
        if (form.validate()) {
            form.saveData(new DSCallback() {
                @Override
                public void execute(DSResponse response, Object rawData, DSRequest request) {
                    if (form.isNewRecord()) {
                        Record[] results = response.getData();
                        if (results.length != 1) {
                            CoreGUI.getErrorHandler().handleError(
                                MSG.view_dynagroup_singleSaveFailure(String.valueOf(results.length)));
                        } else {
                            Record newRecord = results[0];
                            GroupDefinition newGroupDefinition = GroupDefinitionDataSource.getInstance().copyValues(
                                newRecord);
                            if (recalc) {
                                recalculate(dynaGroupChildrenView, newGroupDefinition.getId());
                            }
                            History.newItem(basePath + "/" + newGroupDefinition.getId());
                        }
                    } else {
                        dynaGroupChildrenView.refresh();
                        if (recalc) {
                            recalculate(dynaGroupChildrenView, groupDefinitionId);
                        }
                    }
                }
            });
        }
    }

    private void recalculate(final DynaGroupChildrenView dynaGroupChildrenView, int groupDefId) {
        GWTServiceLookup.getResourceGroupService(600000).recalculateGroupDefinitions(new int[] { groupDefId },
            new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_dynagroup_recalcFailure(), caught);
                }

                @Override
                public void onSuccess(Void result) {
                    dynaGroupChildrenView.refresh();
                    CoreGUI.getMessageCenter()
                        .notify(new Message(MSG.view_dynagroup_recalcSuccessful(), Severity.Info));
                }
            });
    }

    class DynaGroupChildrenView extends Table<ResourceGroupsDataSource> {
        public DynaGroupChildrenView(String locatorId, int groupDefinitionId) {
            super(locatorId, MSG.view_dynagroup_children(), new Criteria("groupDefinitionId", String
                .valueOf(groupDefinition.getId())));
            setDataSource(ResourceGroupsDataSource.getInstance());
        }

        @Override
        protected void configureTable() {
            // i couldn't use percentage widths to work for some reason

            ListGridField idField = new ListGridField("id", MSG.common_title_id());
            idField.setType(ListGridFieldType.INTEGER);
            idField.setWidth("50");

            ListGridField nameField = new ListGridField(NAME.propertyName(), NAME.title());
            nameField.setWidth("*");
            nameField.setCellFormatter(new CellFormatter() {
                @Override
                public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                    String linkName = record.getAttributeAsString(NAME.propertyName());
                    String linkUrl = LinkManager.getResourceGroupLink(record.getAttributeAsInt("id"));
                    return "<a href=\"" + linkUrl + "\">" + StringUtility.escapeHtml(linkName) + "</a>";
                }
            });

            ListGridField descriptionField = new ListGridField(DESCRIPTION.propertyName(), DESCRIPTION.title());
            descriptionField.setWidth("300");
            descriptionField.setCellFormatter(new EscapedHtmlCellFormatter());

            ListGridField typeNameField = new ListGridField(TYPE.propertyName(), TYPE.title());
            typeNameField.setWidth("100");

            ListGridField pluginNameField = new ListGridField(PLUGIN.propertyName(), PLUGIN.title());
            pluginNameField.setWidth("100");

            ListGridField categoryField = new ListGridField(CATEGORY.propertyName(), CATEGORY.title());
            categoryField.setWidth("30");
            categoryField.setAlign(Alignment.CENTER);
            categoryField.setTitle("&nbsp;");
            categoryField.setType(ListGridFieldType.ICON);
            HashMap<String, String> categoryImages = new HashMap<String, String>(2);
            categoryImages.put(GroupCategory.COMPATIBLE.name(), ImageManager.getGroupIcon(GroupCategory.COMPATIBLE));
            categoryImages.put(GroupCategory.MIXED.name(), ImageManager.getGroupIcon(GroupCategory.MIXED));
            categoryField.setValueIcons(categoryImages);
            categoryField.setShowHover(true);
            categoryField.setHoverCustomizer(new HoverCustomizer() {
                @Override
                public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                    String category = record.getAttributeAsString(CATEGORY.propertyName());
                    if (GroupCategory.COMPATIBLE.name().equals(category)) {
                        return MSG.view_dynagroup_compatible();
                    } else if (GroupCategory.MIXED.name().equals(category)) {
                        return MSG.view_dynagroup_mixed();
                    } else {
                        return category; // should never happen
                    }
                }
            });

            setListGridFields(idField, categoryField, nameField, descriptionField, typeNameField, pluginNameField);
            setListGridDoubleClickHandler(new DoubleClickHandler() {
                @Override
                public void onDoubleClick(DoubleClickEvent event) {
                    ListGrid listGrid = (ListGrid) event.getSource();
                    ListGridRecord[] selectedRows = listGrid.getSelection();
                    if (selectedRows != null && selectedRows.length == 1) {
                        String groupUrl = LinkManager.getResourceGroupLink(selectedRows[0].getAttributeAsInt("id"));
                        CoreGUI.goToView(groupUrl);
                    }
                }
            });
        }
    }

    public void switchToEditMode() {
        name.show();
        description.show();
        recursive.show();
        templateSelectorTitleSpacer.show();
        templateSelector.show();
        expression.show();
        recalculationInterval.show();
        markForRedraw();
    }

    private void buildForm() {
        FormItemIcon expressionBuilderIcon = new FormItemIcon();
        expressionBuilderIcon.setSrc("[SKIN]/actions/add.png");
        expressionBuilderIcon.setPrompt(MSG.view_dynagroup_expressionBuilderIconTooltip());
        expressionBuilderIcon.addFormItemClickHandler(new FormItemClickHandler() {
            @Override
            public void onFormItemClick(FormItemIconClickEvent event) {
                showExpressionBuilder();
            }
        });

        id = new TextItem("id", MSG.common_title_id());
        id.setVisible(false);

        name = new TextItem("name", MSG.common_title_name());
        name.setWidth(400);
        name.setDefaultValue("");

        description = new TextAreaItem("description", MSG.common_title_description());
        description.setWidth(400);
        description.setHeight(50);
        description.setDefaultValue("");

        recursive = new CheckboxItem("recursive", MSG.view_dynagroup_recursive());

        templateSelectorTitleSpacer = new SpacerItem();
        templateSelectorTitleSpacer.setShowTitle(false);
        templateSelectorTitleSpacer.setColSpan(1);
        templateSelectorTitleSpacer.setEndRow(false);

        templateSelector = new SelectItem("templateSelector", MSG.view_dynagroup_exprBuilder_savedExpression());
        templateStrings = getTemplates();
        templateSelector.setValueMap(templateStrings.keySet().toArray(new String[templateStrings.size()]));
        templateSelector.setAllowEmptyValue(true);
        templateSelector.setWidth(300);
        templateSelector.setColSpan(1);
        templateSelector.setEndRow(true);
        templateSelector.setStartRow(false);
        templateSelector.setIcons(expressionBuilderIcon);
        templateSelector.setHoverWidth(200);

        expression = new TextAreaItem("expression", MSG.view_dynagroup_expression());
        expression.setWidth(400);
        expression.setHeight(150);
        expression.setDefaultValue("");
        expression.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                // the user changed the expression text, clear the template drop down
                // so the user isn't confused thinking this new value is the template text
                templateSelector.clearValue();
            }
        });
        if (builder != null) {
            builder.destroy();
            builder = null;
        }

        templateSelector.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                if (event.getValue() != null) {
                    String selectedTemplateId = event.getValue().toString();
                    // user picked one of the canned templates - put it in the expression text area
                    String selectedTemplate = templateStrings.get(selectedTemplateId);
                    expression.setValue((selectedTemplate != null) ? selectedTemplate : "");
                } else {
                    expression.setValue("");
                }
            }
        });

        recalculationInterval = new SpinnerItem("recalculationInterval", MSG.view_dynagroup_recalculationInterval());
        //recalculationInterval.setWrapTitle(false); // do not set this - it causes the form to grow abnormally width-wise for some reason
        recalculationInterval.setMin(0);
        recalculationInterval.setDefaultValue(0);
        recalculationInterval.setStep(60000); // the recalc interval is in milliseconds, step up one minute at a time
    }

    public static LinkedHashMap<String, String> getTemplates() {
        LinkedHashMap<String, String> items = new LinkedHashMap<String, String>();

        // grouped items (these can potentially create multiple groups)
        items.put(TEMPLATE_JBOSSAS4_CLUSTERS, //
            buildTemplate("groupby resource.trait[partitionName]", //
                "resource.type.plugin = JBossAS", //
                "resource.type.name = JBossAS Server"));
        items.put(TEMPLATE_JBOSSAS5_CLUSTERS, //
            buildTemplate("groupby resource.trait[MCBean|ServerConfig|*|partitionName]", //
                "resource.type.plugin = JBossAS5", //
                "resource.type.name = JBossAS Server"));
        items.put(TEMPLATE_JBOSSAS4_EAR_CLUSTERS, //
            buildTemplate("groupby resource.parent.trait[partitionName]", //
                "groupby resource.name", //
                "resource.type.plugin = JBossAS", //
                "resource.type.name = Enterprise Application (EAR)"));
        items.put(TEMPLATE_JBOSSAS4_UNIQUE_VERSIONS, //
            buildTemplate("groupby resource.trait[jboss.system:type=Server:VersionName]", //
                "resource.type.plugin = JBossAS", //
                "resource.type.name = JBossAS Server"));
        items.put(TEMPLATE_PLATFORMS, //
            buildTemplate("resource.type.category = PLATFORM", // 
                "groupby resource.name"));
        items.put(TEMPLATE_UNIQUE_RESOURCE_TYPES, //
            buildTemplate("groupby resource.type.plugin", //
                "groupby resource.type.name"));

        // simple items (these create one group)
        items.put(TEMPLATE_JBOSSAS4_HOSTING_APP, //
            buildTemplate("resource.type.plugin = JBossAS", //
                "resource.type.name = JBossAS Server", //
                "resource.child.name.contains = my"));
        items.put(TEMPLATE_JBOSSAS4_NONSECURED, //
            buildTemplate("empty resource.pluginConfiguration[principal]", //
                "resource.type.plugin = JBossAS", //
                "resource.type.name = JBossAS Server"));
        items.put(TEMPLATE_DOWNED_RESOURCES, //
            buildTemplate("resource.availability = DOWN"));

        return items;
    }

    private static String buildTemplate(String... pieces) {
        StringBuilder results = new StringBuilder();
        boolean first = true;
        for (String next : pieces) {
            if (first) {
                first = false;
            } else {
                results.append('\n');
            }
            results.append(next);
        }
        return results.toString();
    }

    private void lookupDetails(final int groupDefinitionId) {
        ResourceGroupDefinitionCriteria criteria = new ResourceGroupDefinitionCriteria();
        criteria.addFilterId(groupDefinitionId);

        if (groupDefinitionId == 0) {
            GroupDefinition newGroupDefinition = new GroupDefinition();
            setGroupDefinition(newGroupDefinition);
            switchToEditMode();
        } else {
            GWTServiceLookup.getResourceGroupService().findGroupDefinitionsByCriteria(criteria,
                new AsyncCallback<PageList<GroupDefinition>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(
                            MSG.view_dynagroup_loadDefinitionFailure(String.valueOf(groupDefinitionId)), caught);
                        History.back();
                    }

                    public void onSuccess(PageList<GroupDefinition> result) {
                        if (result.size() == 0) {
                            CoreGUI.getErrorHandler().handleError(
                                MSG.view_dynagroup_loadDefinitionMissing(String.valueOf(groupDefinitionId)));
                            History.back();
                        } else {
                            GroupDefinition existingGroupDefinition = result.get(0);
                            setGroupDefinition(existingGroupDefinition);
                            switchToEditMode();
                        }
                    }
                });
        }
    }

    private void showExpressionBuilder() {
        if (builder == null) {
            builder = new GroupDefinitionExpressionBuilder(extendLocatorId("exprBuilder"), new AddExpressionHandler() {
                @Override
                public void addExpression(String newExpression) {
                    String currentExpression = "";
                    String value = expression.getValueAsString();
                    if (value != null) {
                        currentExpression = value + "\n";
                    }
                    expression.setValue(currentExpression + newExpression);

                    // the user changed the expression text, clear the template drop down
                    // so the user isn't confused thinking this new value is the template text
                    templateSelector.clearValue();
                }
            });
        }
        builder.show();
    }

    @Override
    public void renderView(final ViewPath viewPath) {
        new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {
            @Override
            public void onPermissionsLoaded(Set<Permission> permissions) {
                if (permissions != null && permissions.contains(Permission.MANAGE_INVENTORY)) {
                    groupDefinitionId = viewPath.getCurrentAsInt();
                    basePath = viewPath.getPathToCurrent();
                    lookupDetails(groupDefinitionId);
                } else {
                    handleAuthorizationFailure();
                }
            }

            private void handleAuthorizationFailure() {
                CoreGUI.getErrorHandler().handleError(MSG.view_dynagroup_permDenied());
                History.back();
            }
        });
    }

}
