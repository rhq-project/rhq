/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.form.CheckboxEditableFormItem;
import org.rhq.enterprise.gui.coregui.client.components.form.EditableFormItem;
import org.rhq.enterprise.gui.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.enterprise.gui.coregui.client.components.form.StringLengthValidator;
import org.rhq.enterprise.gui.coregui.client.components.form.EditableFormItem.ValueEditedHandler;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A canvas that shows basic information/properties on a specific group.
 *
 * @author Ian Springer
 * @author John Mazzitelli
 */
public class GeneralProperties extends LocatableVLayout {
    private ResourceGroupGWTServiceAsync resourceGroupService = GWTServiceLookup.getResourceGroupService();
    private ResourceGroupComposite groupComposite;

    public GeneralProperties(String locatorId, ResourceGroupComposite groupComposite) {
        super(locatorId);
        this.groupComposite = groupComposite;
    }

    @Override
    protected void onInit() {
        super.onInit();

        final ResourceGroup group = this.groupComposite.getResourceGroup();

        HLayout spacer = new HLayout();
        spacer.setHeight(10);
        addMember(spacer);

        final EnhancedDynamicForm generalPropsForm = new EnhancedDynamicForm(this.extendLocatorId("General"));

        List<FormItem> formItems = new ArrayList<FormItem>();

        boolean dynamic = (group.getGroupDefinition() != null);

        StringLengthValidator notEmptyOrNullValidator = new StringLengthValidator(1, null, false);
        StringLengthValidator notNullValidator = new StringLengthValidator(null, null, false);

        final FormItem nameItem = (dynamic) ? new StaticTextItem() : new EditableFormItem();
        nameItem.setName("name");
        nameItem.setTitle(MSG.common_title_name());
        nameItem.setValue(group.getName());
        if (nameItem instanceof EditableFormItem) {
            final EditableFormItem togglableNameItem = (EditableFormItem) nameItem;
            togglableNameItem.setValidators(notEmptyOrNullValidator);
            togglableNameItem.setValueEditedHandler(new ValueEditedHandler() {
                @Override
                public void editedValue(Object newValue) {
                    final String newName = newValue.toString();
                    final String oldName = group.getName();
                    if (newName.equals(oldName)) {
                        return;
                    }
                    group.setName(newName);
                    GeneralProperties.this.resourceGroupService.updateResourceGroup(group, false,
                        new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(
                                    MSG.view_group_summary_nameUpdateFailure(String.valueOf(group.getId()), oldName,
                                        newName), caught);
                                // We failed to update it on the Server, so change back the ResourceGroup and the form item
                                // to the original value.
                                group.setName(oldName);
                                nameItem.setValue(oldName);
                            }

                            public void onSuccess(Void result) {
                                CoreGUI.getMessageCenter().notify(
                                    new Message(MSG.view_group_summary_nameUpdateSuccessful(String.valueOf(group
                                        .getId()), oldName, newName), Message.Severity.Info));
                            }
                        });
                }
            });
        }
        formItems.add(nameItem);

        StaticTextItem typeItem = new StaticTextItem("memberType", MSG.view_group_summary_memberType());
        ResourceType type = group.getResourceType();
        if (type != null) {
            typeItem.setTooltip(MSG.common_title_plugin() + ": " + type.getPlugin() + "\n<br>"
                + MSG.common_title_type() + ": " + type.getName());
            typeItem.setValue(type.getName() + " (" + type.getPlugin() + ")");
        } else {
            typeItem.setValue("<i>" + MSG.view_group_summary_mixed() + "</i>");
        }
        formItems.add(typeItem);

        StaticTextItem countItem = new StaticTextItem("memberCount", MSG.view_group_summary_memberCount());
        long memberCount = this.groupComposite.getImplicitUp() + this.groupComposite.getImplicitDown();
        countItem.setValue(memberCount);
        formItems.add(countItem);

        final FormItem descriptionItem = (dynamic) ? new StaticTextItem() : new EditableFormItem();
        descriptionItem.setName("description");
        descriptionItem.setTitle(MSG.common_title_description());
        descriptionItem.setValue(group.getDescription());
        if (descriptionItem instanceof EditableFormItem) {
            final EditableFormItem togglableDescriptionItem = (EditableFormItem) descriptionItem;
            togglableDescriptionItem.setValidators(notNullValidator);
            togglableDescriptionItem.setValueEditedHandler(new ValueEditedHandler() {
                @Override
                public void editedValue(Object newValue) {
                    final String newDescription = newValue.toString();
                    final String oldDescription = group.getDescription();
                    if (newDescription.equals(oldDescription)) {
                        return;
                    }
                    group.setDescription(newDescription);
                    GeneralProperties.this.resourceGroupService.updateResourceGroup(group, false,
                        new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(
                                    MSG.view_group_summary_descUpdateFailure(String.valueOf(group.getId())), caught);
                                // We failed to update it on the Server, so change back the ResourceGroup and the form item
                                // to the original value.
                                group.setDescription(oldDescription);
                                descriptionItem.setValue(oldDescription);
                            }

                            public void onSuccess(Void result) {
                                CoreGUI.getMessageCenter().notify(
                                    new Message(MSG.view_group_summary_descUpdateSuccessful(), Message.Severity.Info));
                            }
                        });
                }
            });
        }
        formItems.add(descriptionItem);

        StaticTextItem dynamicItem = new StaticTextItem("dynamic", MSG.view_group_summary_dynamic());
        dynamicItem.setValue(dynamic ? MSG.common_val_yes_lower() : MSG.common_val_no_lower());
        formItems.add(dynamicItem);

        EditableFormItem recursiveItem = new CheckboxEditableFormItem("recursive", MSG.view_group_summary_recursive());
        recursiveItem.setValueEditedHandler(new ValueEditedHandler() {
            @Override
            public void editedValue(Object newValue) {
                CoreGUI.getMessageCenter().notify(new Message("TODO: set recursive flag to=" + newValue));
            }
        });
        recursiveItem.setValue((group.isRecursive()) ? MSG.common_val_yes_lower() : MSG.common_val_no_lower());
        formItems.add(recursiveItem);

        StaticTextItem createdItem = new StaticTextItem("created", MSG.common_title_dateCreated());
        createdItem.setValue(new Date(group.getCtime()));
        formItems.add(createdItem);

        StaticTextItem lastModifiedItem = new StaticTextItem("lastModified", MSG.common_title_lastUpdated());
        lastModifiedItem.setValue(new Date(group.getMtime()));
        formItems.add(lastModifiedItem);

        StaticTextItem lastModifiedByItem = new StaticTextItem("lastModifiedBy", MSG.common_title_lastUpdatedBy());
        lastModifiedByItem.setValue(group.getModifiedBy());
        formItems.add(lastModifiedByItem);

        if (dynamic) {
            StaticTextItem groupDefinitionItem = new StaticTextItem("groupDefinition", MSG
                .view_group_summary_groupDefinition());
            GroupDefinition groupDefinition = group.getGroupDefinition();
            String groupDefinitionUrl = LinkManager.getGroupDefinitionLink(groupDefinition.getId());
            groupDefinitionItem
                .setValue("<a href=\"" + groupDefinitionUrl + "\">" + groupDefinition.getName() + "</a>");
            formItems.add(groupDefinitionItem);
        }

        generalPropsForm.setItems(formItems.toArray(new FormItem[formItems.size()]));
        addMember(generalPropsForm);

        if (dynamic) {
            spacer = new HLayout();
            spacer.setHeight(10);
            addMember(spacer);

            HTMLFlow note = new HTMLFlow();
            note.setContents("<b>*</b> " + MSG.view_group_summary_dynamicNote());
            note.setAlign(Alignment.CENTER);
            addMember(note);
        }

        HLayout spacer2 = new HLayout();
        spacer2.setHeight(10);
        addMember(spacer2);
    }
}
