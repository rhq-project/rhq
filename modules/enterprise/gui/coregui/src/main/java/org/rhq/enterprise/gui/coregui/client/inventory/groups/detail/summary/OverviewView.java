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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.summary;

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
import org.rhq.enterprise.gui.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.enterprise.gui.coregui.client.components.form.TogglableTextItem;
import org.rhq.enterprise.gui.coregui.client.components.form.ValueUpdatedHandler;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * The group Inventory>Overview subtab.
 *
 * @author Ian Springer
 */
public class OverviewView extends LocatableVLayout {
    private ResourceGroupGWTServiceAsync resourceGroupService = GWTServiceLookup.getResourceGroupService();
    private ResourceGroupComposite groupComposite;

    public OverviewView(String locatorId, ResourceGroupComposite groupComposite) {
        super(locatorId);
        this.groupComposite = groupComposite;
    }

    @Override
    protected void onInit() {
        super.onInit();

        final ResourceGroup group = this.groupComposite.getResourceGroup();

        HLayout spacer = new HLayout();
        spacer.setHeight(15);
        addMember(spacer);

        final EnhancedDynamicForm generalPropsForm = new EnhancedDynamicForm(this.extendLocatorId("General"));

        List<FormItem> formItems = new ArrayList<FormItem>();

        // TODO: Uncomment the below header if we decide to add other stuff to this page besides the general props.
        //HeaderItem headerItem = new HeaderItem("header", "General Properties");
        //headerItem.setValue("General Properties");
        //formItems.add(headerItem);        

        boolean dynamic = (group.getGroupDefinition() != null);

        final FormItem nameItem = (dynamic) ? new StaticTextItem() : new TogglableTextItem();
        nameItem.setName("name");
        nameItem.setTitle(MSG.common_title_name());
        nameItem.setValue(group.getName());
        if (nameItem instanceof TogglableTextItem) {
            final TogglableTextItem togglableNameItem = (TogglableTextItem) nameItem;
            togglableNameItem.addValueUpdatedHandler(new ValueUpdatedHandler() {
                public void onValueUpdated(final String newName) {
                    final String oldName = group.getName();
                    if (newName.equals(oldName)) {
                        return;
                    }
                    group.setName(newName);
                    OverviewView.this.resourceGroupService.updateResourceGroup(group, false, new AsyncCallback<Void>() {
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
                                new Message(MSG.view_group_summary_nameUpdateSuccessful(String.valueOf(group.getId()),
                                    oldName, newName), Message.Severity.Info));
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

        final FormItem descriptionItem = (dynamic) ? new StaticTextItem() : new TogglableTextItem();
        descriptionItem.setName("description");
        descriptionItem.setTitle(MSG.common_title_description());
        descriptionItem.setValue(group.getDescription());
        if (descriptionItem instanceof TogglableTextItem) {
            final TogglableTextItem togglableDescriptionItem = (TogglableTextItem) descriptionItem;
            togglableDescriptionItem.addValueUpdatedHandler(new ValueUpdatedHandler() {
                public void onValueUpdated(final String newDescription) {
                    final String oldDescription = group.getDescription();
                    if (newDescription.equals(oldDescription)) {
                        return;
                    }
                    group.setDescription(newDescription);
                    OverviewView.this.resourceGroupService.updateResourceGroup(group, false, new AsyncCallback<Void>() {
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

        StaticTextItem recursiveItem = new StaticTextItem("recursive", MSG.view_group_summary_recursive());
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
    }
}
