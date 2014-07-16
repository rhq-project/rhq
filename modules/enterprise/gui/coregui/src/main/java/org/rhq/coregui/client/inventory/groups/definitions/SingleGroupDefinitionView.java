/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.coregui.client.inventory.groups.definitions;

import static org.rhq.coregui.client.inventory.groups.ResourceGroupDataSourceField.CATEGORY;
import static org.rhq.coregui.client.inventory.groups.ResourceGroupDataSourceField.DESCRIPTION;
import static org.rhq.coregui.client.inventory.groups.ResourceGroupDataSourceField.NAME;
import static org.rhq.coregui.client.inventory.groups.ResourceGroupDataSourceField.PLUGIN;
import static org.rhq.coregui.client.inventory.groups.ResourceGroupDataSourceField.TYPE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
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
import com.smartgwt.client.widgets.form.validator.IsIntegerValidator;
import com.smartgwt.client.widgets.form.validator.RegExpValidator;
import com.smartgwt.client.widgets.form.validator.Validator;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.ResourceGroupDefinitionCriteria;
import org.rhq.core.domain.plugin.CannedGroupExpression;
import org.rhq.core.domain.resource.group.DuplicateExpressionTypeException;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.BookmarkableView;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.PermissionsLoadedListener;
import org.rhq.coregui.client.PermissionsLoader;
import org.rhq.coregui.client.ViewPath;
import org.rhq.coregui.client.components.table.EscapedHtmlCellFormatter;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.groups.ResourceGroupsDataSource;
import org.rhq.coregui.client.inventory.groups.definitions.GroupDefinitionExpressionBuilder.AddExpressionHandler;
import org.rhq.coregui.client.util.StringUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * @author Joseph Marques
 */
public class SingleGroupDefinitionView extends EnhancedVLayout implements BookmarkableView {

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
    private TreeMap<String,CannedGroupExpression> cannedExpressions;
    private TextAreaItem expression;
    private SpinnerItem recalculationInterval;

    public SingleGroupDefinitionView() {
        super();
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
        recalculationInterval.setValue(groupDefinition.getRecalculationInterval() / (60 * 1000));
        Validator intervalValidator = new IsIntegerValidator();
        intervalValidator.setErrorMessage(MSG.view_dynagroup_recalculationInterval_error());
        recalculationInterval.setValidators(intervalValidator);
        expression.setValue(groupDefinition.getExpression());

        Validator nameValidator = new RegExpValidator("^[^\\<\\$\\'\\{\\[]{1,100}$");
        nameValidator.setErrorMessage("Name must not contain following characters: < $ ' [ {");
        name.setValidators(nameValidator);

        final DynamicForm form = new DynamicForm();
        form.setFields(id, name, description, templateSelectorTitleSpacer, templateSelector, expression, recursive,
            recalculationInterval);
        form.setDataSource(GroupDefinitionDataSource.getInstance());
        form.setHiliteRequiredFields(true);
        form.setRequiredTitleSuffix(" <span style=\"color: red;\">* </span>:");
        DSOperationType saveOperationType = (groupDefinition.getId() == 0) ? DSOperationType.ADD
            : DSOperationType.UPDATE;
        form.setSaveOperationType(saveOperationType);

        final DynaGroupChildrenView dynaGroupChildrenView = new DynaGroupChildrenView(groupDefinitionId);

        // button setup
        IButton saveButton = new EnhancedIButton(MSG.common_button_save(), ButtonColor.BLUE);
        saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (form.validate()) {
                    saveFormCheckCannedExpr(form, dynaGroupChildrenView, false);
                }
            }
        });

        IButton recalculateButton = new EnhancedIButton(MSG.view_dynagroup_saveAndRecalculate());
        recalculateButton.setWidth(150);
        recalculateButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (form.validate()) {
                    saveFormCheckCannedExpr(form, dynaGroupChildrenView, true);
                }
            }
        });

        IButton resetButton = new EnhancedIButton(MSG.common_button_reset());
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
    public void setCannedExpressions(final ArrayList<CannedGroupExpression> list) {
        this.cannedExpressions = new TreeMap<String, CannedGroupExpression>();
        for (CannedGroupExpression cge : list) {
            this.cannedExpressions.put(cge.getPlugin() + " - " + cge.getName(), cge);
        }
        templateSelector.setValueMap(cannedExpressions.keySet().toArray(new String[cannedExpressions.size()]));
    }
    
    private void saveFormCheckCannedExpr(final DynamicForm form, final DynaGroupChildrenView dynaGroupChildrenView,
        final boolean recalc) {
        if (this.groupDefinition.getCannedExpression() != null) {
            SC.ask(MSG.view_dynagroup_saveCannedDefWarning(this.groupDefinition.getCannedExpression().replaceAll(":.*", "")), new BooleanCallback() {
                public void execute(Boolean confirmed) {
                    if (confirmed) {
                        saveForm(form, dynaGroupChildrenView, recalc);
                    }
                }
            });
        } else {
            saveForm(form, dynaGroupChildrenView, recalc);
        }
    }

    private void saveForm(final DynamicForm form, final DynaGroupChildrenView dynaGroupChildrenView,
        final boolean recalc) {
            form.saveData(new DSCallback() {
                @Override
                public void execute(DSResponse response, Object rawData, DSRequest request) {
                    boolean hasDuplicateNameError = false;
                    boolean hasParseExpressionError = false;
                    if (form.isNewRecord()) {
                        Record[] results = response.getData();
                        if (results.length != 1) {

                            // handle the special case for name already exists error
                            for (Object entryObject : response.getErrors().entrySet()) {
                                Map.Entry thisEntry = (Map.Entry) entryObject;
                                String fieldKey = (String) thisEntry.getKey();
                                // the duplicate name error will be keyed by 'name' in the errorMap
                                if (fieldKey.equals("name")) {
                                    String errorValue = (String) thisEntry.getValue();
                                    CoreGUI.getErrorHandler().handleError(errorValue);
                                    hasDuplicateNameError = true;
                                } else if (fieldKey.equals("expression")) {
                                    String errorValue = (String) thisEntry.getValue();
                                    CoreGUI.getErrorHandler().handleError(errorValue);
                                    hasParseExpressionError = true;
                                }
                            }

                            if (!hasDuplicateNameError && !hasParseExpressionError) {
                                CoreGUI.getErrorHandler().handleError(
                                    MSG.view_dynagroup_singleSaveFailure(String.valueOf(results.length)));
                            }

                        } else {
                            Record newRecord = results[0];
                            GroupDefinition newGroupDefinition = GroupDefinitionDataSource.getInstance().copyValues(
                                newRecord);
                            if (recalc) {
                                recalculate(dynaGroupChildrenView, newGroupDefinition.getId());
                            }
                            CoreGUI.goToView(basePath + "/" + newGroupDefinition.getId());
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

    private void recalculate(final DynaGroupChildrenView dynaGroupChildrenView, int groupDefId) {
        GWTServiceLookup.getResourceGroupService(600000).recalculateGroupDefinitions(new int[] { groupDefId },
            new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {
                    if (caught instanceof DuplicateExpressionTypeException) {
                        CoreGUI.getMessageCenter().notify(new Message(caught.getMessage(), Message.Severity.Warning));
                    } else {
                        CoreGUI.getErrorHandler().handleError(MSG.view_dynagroup_recalcFailure(), caught);
                    }
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
        public DynaGroupChildrenView(int groupDefinitionId) {
            super(MSG.view_dynagroup_children(), new Criteria("groupDefinitionId", String.valueOf(groupDefinition
                .getId())));
            setDataSource(ResourceGroupsDataSource.getInstance());
        }

        @Override
        protected void configureTable() {
            // i couldn't get percentage widths to work for some reason

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
                        throw new IllegalStateException("Unknown group category: " + category);
                    }
                }
            });

            setListGridFields(idField, categoryField, nameField, descriptionField, typeNameField, pluginNameField);
            setListGridDoubleClickHandler(new DoubleClickHandler() {
                @Override
                public void onDoubleClick(DoubleClickEvent event) {
                    ListGrid listGrid = (ListGrid) event.getSource();
                    ListGridRecord[] selectedRows = listGrid.getSelectedRecords();
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
        templateSelector.setAllowEmptyValue(true);
        templateSelector.setWidth(400);
        templateSelector.setColSpan(1);
        templateSelector.setEndRow(true);
        templateSelector.setStartRow(false);
        templateSelector.setIcons(expressionBuilderIcon);
        templateSelector.setHoverWidth(200);
        templateSelector.setValueMap("");

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
                    // user picked one of the canned expressions - update field values
                    CannedGroupExpression cge = cannedExpressions.get(selectedTemplateId);
                    if (cge != null) {
                        StringBuilder expr = new StringBuilder();
                        for (String e : cge.getExpression()) {
                            expr.append(e+"\n");
                        }
                        expression.setValue(expr.toString());
                        recalculationInterval.setValue(cge.getRecalcInMinutes());
                        description.setValue(cge.getDescription());
                        recursive.setValue(cge.isRecursive());
                        name.setValue(cge.getName());
                    }
                    else {
                        expression.setValue("");
                    }
                } else {
                    expression.setValue("");
                }
            }
        });

        recalculationInterval = new SpinnerItem("recalculationInterval", MSG.view_dynagroup_recalculationInterval());
        recalculationInterval.setMin(0);
        recalculationInterval.setMax(60 * 24 * 7); // max set to 1 week
        recalculationInterval.setDefaultValue(0);
        recalculationInterval.setStep(1); // the recalc interval is in milliseconds, step up one minute at a time
    }

    private void lookupCannedExpressions() {
        GWTServiceLookup.getPluginService().getCannedGroupExpressions(new AsyncCallback<ArrayList<CannedGroupExpression>>() {

            @Override
            public void onFailure(Throwable arg0) {
            }

            @Override
            public void onSuccess(ArrayList<CannedGroupExpression> arg0) {
                setCannedExpressions(arg0);
            }
        });;
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
            builder = new GroupDefinitionExpressionBuilder(new AddExpressionHandler() {
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
                    lookupCannedExpressions();
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
