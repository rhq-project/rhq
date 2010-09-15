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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.ResetItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpinnerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.core.domain.criteria.ResourceGroupDefinitionCriteria;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.FormUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Joseph Marques
 */
public class SingleGroupDefinitionView extends LocatableVLayout implements BookmarkableView {

    private int groupDefinitionId;
    private GroupDefinition groupDefinition;

    private ViewId viewId;

    private TextItem name;
    private TextAreaItem description;
    private CheckboxItem recursive;
    private SelectItem templateSelector;
    private TextAreaItem expression;
    private SpinnerItem recalculationInterval;

    private StaticTextItem nameStatic;
    private StaticTextItem descriptionStatic;
    private StaticTextItem recursiveStatic;
    private StaticTextItem expressionStatic;
    private StaticTextItem recalculationIntervalStatic;

    private boolean formBuilt = false;

    public SingleGroupDefinitionView(String locatorId) {
        this(locatorId, null);
    }

    public SingleGroupDefinitionView(String locatorId, GroupDefinition groupDefinition) {
        super(locatorId);

        setPadding(10);
        setOverflow(Overflow.AUTO);

        buildForm();

        this.groupDefinition = groupDefinition;
    }

    public void setGroupDefinition(GroupDefinition groupDefinition) {
        this.groupDefinition = groupDefinition;

        if (groupDefinition == null) {
            clearFormValues();
        } else {
            name.setValue(groupDefinition.getName());
            nameStatic.setValue(groupDefinition.getName());

            recursive.setValue(groupDefinition.isRecursive() ? "Yes" : "No");
            recursiveStatic.setValue(groupDefinition.isRecursive() ? "Yes" : "No");

            description.setValue(groupDefinition.getDescription());
            descriptionStatic.setValue(groupDefinition.getDescription());

            recalculationInterval.setValue(groupDefinition.getRecalculationInterval());
            recalculationIntervalStatic.setValue(groupDefinition.getRecalculationInterval());

            expression.setValue(groupDefinition.getExpression());
            expressionStatic.setValue(groupDefinition.getExpression());

            ButtonItem saveButton = new ButtonItem("save", "Save");
            ResetItem resetButton = new ResetItem("reset", "Reset");

            saveButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    createOrUpdate();
                }
            });

            LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("GroupDefinitionForm"));
            form.setFields(name, nameStatic, recursive, recursiveStatic, description, descriptionStatic,
                recalculationInterval, recalculationIntervalStatic, expression, expressionStatic, saveButton,
                resetButton);
            form.setNumCols(4);

            addMember(form);
            formBuilt = true;
        }

        markForRedraw();
    }

    public void switchToEditMode() {
        name.show();
        description.show();
        recursive.show();
        expression.show();
        recalculationInterval.show();

        nameStatic.hide();
        descriptionStatic.hide();
        recursiveStatic.hide();
        expressionStatic.hide();
        recalculationIntervalStatic.hide();

        if (groupDefinitionId == 0) {
            viewId.getBreadcrumbs().get(0).setDisplayName("New Group Definition");
        } else {
            viewId.getBreadcrumbs().get(0).setDisplayName("Editing '" + nameStatic.getValue().toString() + "'");
        }
        CoreGUI.refreshBreadCrumbTrail();

        markForRedraw();
    }

    public void switchToViewMode() {
        name.hide();
        description.hide();
        recursive.hide();
        expression.hide();
        recalculationInterval.hide();

        nameStatic.show();
        descriptionStatic.show();
        recursiveStatic.show();
        expressionStatic.show();
        recalculationIntervalStatic.show();

        markForRedraw();
    }

    public void createOrUpdate() {
        groupDefinition.setName(FormUtility.getStringSafely(name));
        groupDefinition.setDescription(FormUtility.getStringSafely(description));
        groupDefinition.setRecursive("Yes".equals(recursive.getValue()));
        groupDefinition.setExpression(FormUtility.getStringSafely(expression));
        groupDefinition.setRecalculationInterval(Long.valueOf(FormUtility.getStringSafely(recalculationInterval, "0")));

        final String name = groupDefinition.getName();
        if (groupDefinitionId == 0) {
            GWTServiceLookup.getResourceGroupService().createGroupDefinition(groupDefinition,
                new AsyncCallback<GroupDefinition>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failure to create group definition '" + name + "'",
                            caught);
                    }

                    @Override
                    public void onSuccess(GroupDefinition result) {
                        CoreGUI.getErrorHandler().handleError("Successfully created group definition '" + name + "'");
                        History.back();
                    }
                });
        } else {
            GWTServiceLookup.getResourceGroupService().updateGroupDefinition(groupDefinition,
                new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failure saving group definition '" + name + "'", caught);
                    }

                    @Override
                    public void onSuccess(Void result) {
                        CoreGUI.getErrorHandler().handleError("Successfully saved group definition '" + name + "'");
                        History.back();
                    }
                });
        }
    }

    public void clearFormValues() {
        name.clearValue();
        description.clearValue();
        recursive.clearValue();
        expression.clearValue();
        recalculationInterval.clearValue();

        nameStatic.clearValue();
        descriptionStatic.clearValue();
        recursiveStatic.clearValue();
        expressionStatic.clearValue();
        recalculationIntervalStatic.clearValue();

        markForRedraw();
    }

    private void buildForm() {
        if (!formBuilt) {

            name = new TextItem("name", "Name");
            name.setWidth(400);
            name.setDefaultValue("");
            nameStatic = new StaticTextItem("nameStatic", "Name");

            description = new TextAreaItem("description", "Description");
            description.setWidth(400);
            description.setHeight(50);
            description.setDefaultValue("");
            descriptionStatic = new StaticTextItem("descriptionStatic", "Description");

            recursive = new CheckboxItem("recursive", "Recursive");
            recursive.setValueMap("Yes", "No");
            recursive.setDefaultValue("Yes");
            recursiveStatic = new StaticTextItem("recursiveStatic", "Recursive");

            expression = new TextAreaItem("expression", "Expression");
            expression.setWidth(400);
            expression.setHeight(150);
            expression.setDefaultValue("");
            expressionStatic = new StaticTextItem("expressionStatic", "Expression");

            recalculationInterval = new SpinnerItem("recalculationInterval", "Recalculation Interval");
            recalculationInterval.setWrapTitle(false);
            recalculationInterval.setDefaultValue(0);
            recalculationIntervalStatic = new StaticTextItem("recalculationInterval", "Recalculation Interval");

            templateSelector = new SelectItem();
            templateSelector.setValueMap(getTemplates());

            formBuilt = true;
        }
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
                            "Failure loading group definition[id=" + groupDefinitionId + "]", caught);
                    }

                    public void onSuccess(PageList<GroupDefinition> result) {
                        GroupDefinition existingGroupDefinition = result.get(0);
                        setGroupDefinition(existingGroupDefinition);
                        switchToEditMode();
                    }
                });
        }
    }

    @Override
    public void renderView(ViewPath viewPath) {
        groupDefinitionId = viewPath.getCurrentAsInt();
        viewId = viewPath.getCurrent();
        lookupDetails(groupDefinitionId);
    }

}
