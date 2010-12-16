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
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpinnerItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
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
import org.rhq.enterprise.gui.coregui.client.ViewId;
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

    private int groupDefinitionId;
    private GroupDefinition groupDefinition;
    private String basePath;
    private ViewId viewId;

    // editable form
    private TextItem id;
    private TextItem name;
    private TextAreaItem description;
    private CheckboxItem recursive;
    private SelectItem templateSelector;
    private TextAreaItem expression;
    private SpinnerItem recalculationInterval;

    public SingleGroupDefinitionView(String locatorId) {
        super(locatorId);

        setPadding(10);
        setOverflow(Overflow.VISIBLE);
        setWidth(5);

        buildForm();
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
        form.setFields(id, name, description, expression, recursive, recalculationInterval);
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
                            GWTServiceLookup.getResourceGroupService().recalculateGroupDefinitions(
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
    }

    public void switchToEditMode() {
        name.show();
        description.show();
        recursive.show();
        expression.show();
        recalculationInterval.show();

        if (groupDefinitionId == 0) {
            viewId.getBreadcrumbs().get(0).setDisplayName(MSG.view_dynagroup_newGroupDefinition());
        } else {
            viewId.getBreadcrumbs().get(0).setDisplayName(MSG.view_dynagroup_editing(name.getValue().toString()));
        }
        CoreGUI.refreshBreadCrumbTrail();

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

        expression = new TextAreaItem("expression", MSG.view_dynagroup_expression());
        expression.setWidth(400);
        expression.setHeight(150);
        expression.setDefaultValue("");

        recalculationInterval = new SpinnerItem("recalculationInterval", MSG.view_dynagroup_recalculationInterval());
        recalculationInterval.setWrapTitle(false);
        recalculationInterval.setMin(0);
        recalculationInterval.setDefaultValue(0);

        templateSelector = new SelectItem();
        templateSelector.setValueMap(getTemplates());
    }

    public static LinkedHashMap<String, String> getTemplates() {
        LinkedHashMap<String, String> items = new LinkedHashMap<String, String>();

        // grouped items
        items.put("JBossAS clusters in the system", //
            get("groupby resource.trait[partitionName]", //
                "resource.type.plugin = JBossAS", //
                "resource.type.name = JBossAS Server"));
        items.put("Clustered enterprise application archive (EAR)", //
            get("groupby resource.parent.trait[partitionName]", //
                "groupby resource.name", //
                "resource.type.plugin = JBossAS", //
                "resource.type.name = Enterprise Application (EAR)"));
        items.put("Unique JBossAS versions in inventory", //
            get("groupby resource.trait[jboss.system:type=Server:VersionName]", //
                "resource.type.plugin = JBossAS", //
                "resource.type.name = JBossAS Server"));
        items.put("Platform resource in inventory", //
            get("resource.type.category = PLATFORM", // 
                "groupby resource.name"));
        items.put("Unique resource type in inventory", //
            get("groupby resource.type.plugin", //
                "groupby resource.type.name"));

        // simple items
        items.put("All JBossAS hosting any version of 'my' app", //
            get("resource.type.plugin = JBossAS", //
                "resource.type.name = JBossAS Server", //
                "resource.child.name.contains = my"));
        items.put("All Non-secured JBossAS servers", //
            get("empty resource.pluginConfiguration[principal]", //
                "resource.type.plugin = JBossAS", //
                "resource.type.name = JBossAS Server"));
        items.put("All resources currently down", //
            get("resource.availability = DOWN"));

        return items;
    }

    private static String get(String... pieces) {
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
                    viewId = viewPath.getCurrent();
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
