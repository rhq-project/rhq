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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.definitions;

import static org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupDataSourceField.CATEGORY;
import static org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupDataSourceField.DESCRIPTION;
import static org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupDataSourceField.NAME;
import static org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupDataSourceField.PLUGIN;
import static org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupDataSourceField.TYPE;

import java.util.LinkedHashMap;
import java.util.Set;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.DSOperationType;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.SpinnerItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.ResourceGroupDefinitionCriteria;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoadedListener;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoader;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupsDataSource;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Joseph Marques
 */
public class SingleGroupDefinitionView extends LocatableVLayout implements BookmarkableView {

    private static final String TEMPLATE_JBOSSAS_CLUSTERS = MSG.view_dynagroup_template_jbossAsClusters();
    private static final String TEMPLATE_EAR_CLUSTERS = MSG.view_dynagroup_template_earClusters();
    private static final String TEMPLATE_UNIQUE_JBOSSAS_VERSIONS = MSG.view_dynagroup_template_uniqueJBossASVersions();
    private static final String TEMPLATE_PLATFORMS = MSG.view_dynagroup_template_platforms();
    private static final String TEMPLATE_UNIQUE_RESOURCE_TYPES = MSG.view_dynagroup_template_uniqueResourceTypes();
    private static final String TEMPLATE_JBOSSAS_HOSTING_APP = MSG.view_dynagroup_template_jbossAsHostingApp();
    private static final String TEMPLATE_NONSECURED_JBOSSAS = MSG.view_dynagroup_template_nonsecuredJBossAS();
    private static final String TEMPLATE_DOWNED_RESOURCES = MSG.view_dynagroup_template_downedResources();

    private int groupDefinitionId;
    private GroupDefinition groupDefinition;
    private String basePath;

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
        setAutoWidth();
        setOverflow(Overflow.AUTO);
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
        if (groupDefinition.getId() == 0) {
            form.setSaveOperationType(DSOperationType.ADD);
        } else {
            form.setSaveOperationType(DSOperationType.UPDATE);
        }

        final DynaGroupChildrenView dynaGroupChildrenView = new DynaGroupChildrenView(extendLocatorId("DynaGroups"),
            groupDefinitionId);

        // button setup
        IButton saveButton = new LocatableIButton(this.extendLocatorId("Save"), MSG.common_button_save());
        //saveButton.addClickHandler(new SaveOrUpdateClickHandler(form, operationType, dynaGroupChildrenView));
        saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
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
                                    GroupDefinition newGroupDefinition = GroupDefinitionDataSource.getInstance()
                                        .copyValues((ListGridRecord) newRecord);
                                    History.newItem(basePath + "/" + newGroupDefinition.getId());
                                }
                            } else {
                                dynaGroupChildrenView.refresh();
                            }
                        }
                    });
                }
            }
        });

        IButton recalculateButton = new LocatableIButton(this.extendLocatorId("Recalculate"), MSG
            .view_dynagroup_saveAndRecalculate());
        recalculateButton.setWidth(150);
        recalculateButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (form.validate()) {
                    form.saveData(new DSCallback() {
                        @Override
                        public void execute(DSResponse response, Object rawData, DSRequest request) {
                            GWTServiceLookup.getResourceGroupService(600000).recalculateGroupDefinitions(
                                new int[] { groupDefinitionId }, new AsyncCallback<Void>() {
                                    @Override
                                    public void onFailure(Throwable caught) {
                                        CoreGUI.getErrorHandler().handleError(MSG.view_dynagroup_recalcFailure(),
                                            caught);
                                    }

                                    @Override
                                    public void onSuccess(Void result) {
                                        dynaGroupChildrenView.refresh();
                                        CoreGUI.getMessageCenter().notify(
                                            new Message(MSG.view_dynagroup_recalcSuccessful(), Severity.Info));
                                    }
                                });
                        }
                    });
                }
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

    class DynaGroupChildrenView extends Table {
        public DynaGroupChildrenView(String locatorId, int groupDefinitionId) {
            super(locatorId, MSG.view_dynagroup_children(), new Criteria("groupDefinitionId", String
                .valueOf(groupDefinition.getId())));
            setDataSource(ResourceGroupsDataSource.getInstance());
            setMinHeight(250);
        }

        @Override
        protected void configureTable() {
            ListGridField idField = new ListGridField("id", MSG.common_title_id());
            idField.setType(ListGridFieldType.INTEGER);
            idField.setWidth("50");

            ListGridField nameField = new ListGridField(NAME.propertyName(), NAME.title());
            nameField.setWidth("100");

            ListGridField descriptionField = new ListGridField(DESCRIPTION.propertyName(), DESCRIPTION.title());
            descriptionField.setWidth("100");

            ListGridField typeNameField = new ListGridField(TYPE.propertyName(), TYPE.title());
            typeNameField.setWidth("100");

            ListGridField pluginNameField = new ListGridField(PLUGIN.propertyName(), PLUGIN.title());
            pluginNameField.setWidth("100");

            ListGridField categoryField = new ListGridField(CATEGORY.propertyName(), CATEGORY.title());
            categoryField.setWidth("100");

            setListGridFields(idField, nameField, descriptionField, typeNameField, pluginNameField, categoryField);
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

        templateSelector = new SelectItem("templateSelector");
        templateStrings = getTemplates();
        templateSelector.setValueMap(templateStrings.keySet().toArray(new String[templateStrings.size()]));
        templateSelector.setAllowEmptyValue(true);
        templateSelector.setWidth(300);
        templateSelector.setShowTitle(false);
        templateSelector.setColSpan(1);
        templateSelector.setEndRow(true);
        templateSelector.setStartRow(false);

        expression = new TextAreaItem("expression", MSG.view_dynagroup_expression());
        expression.setWidth(400);
        expression.setHeight(150);
        expression.setDefaultValue("");

        templateSelector.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                if (event.getValue() != null) {
                    String selectedTemplateId = event.getValue().toString();
                    String selectedTemplate = templateStrings.get(selectedTemplateId);
                    expression.setValue((selectedTemplate != null) ? selectedTemplate : "");
                } else {
                    expression.setValue("");
                }
            }
        });

        recalculationInterval = new SpinnerItem("recalculationInterval", MSG.view_dynagroup_recalculationInterval());
        recalculationInterval.setWrapTitle(false);
        recalculationInterval.setMin(0);
        recalculationInterval.setDefaultValue(0);
    }

    public static LinkedHashMap<String, String> getTemplates() {
        LinkedHashMap<String, String> items = new LinkedHashMap<String, String>();

        // grouped items
        items.put(TEMPLATE_JBOSSAS_CLUSTERS, //
            buildTemplate("groupby resource.trait[partitionName]", //
                "resource.type.plugin = JBossAS", //
                "resource.type.name = JBossAS Server"));
        items.put(TEMPLATE_EAR_CLUSTERS, //
            buildTemplate("groupby resource.parent.trait[partitionName]", //
                "groupby resource.name", //
                "resource.type.plugin = JBossAS", //
                "resource.type.name = Enterprise Application (EAR)"));
        items.put(TEMPLATE_UNIQUE_JBOSSAS_VERSIONS, //
            buildTemplate("groupby resource.trait[jboss.system:type=Server:VersionName]", //
                "resource.type.plugin = JBossAS", //
                "resource.type.name = JBossAS Server"));
        items.put(TEMPLATE_PLATFORMS, //
            buildTemplate("resource.type.category = PLATFORM", // 
                "groupby resource.name"));
        items.put(TEMPLATE_UNIQUE_RESOURCE_TYPES, //
            buildTemplate("groupby resource.type.plugin", //
                "groupby resource.type.name"));

        // simple items
        items.put(TEMPLATE_JBOSSAS_HOSTING_APP, //
            buildTemplate("resource.type.plugin = JBossAS", //
                "resource.type.name = JBossAS Server", //
                "resource.child.name.contains = my"));
        items.put(TEMPLATE_NONSECURED_JBOSSAS, //
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
