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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.inventory;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.enterprise.gui.coregui.client.components.form.TogglableTextItem;
import org.rhq.enterprise.gui.coregui.client.components.form.ValueUpdatedHandler;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The group Inventory>Overview subtab.
 *
 * @author Ian Springer
 */
public class OverviewView extends VLayout {
    private ResourceGroupGWTServiceAsync resourceGroupService = GWTServiceLookup.getResourceGroupService();
    private ResourceGroupComposite groupComposite;

    public OverviewView(ResourceGroupComposite groupComposite) {
        super();
        this.groupComposite = groupComposite;
    }

    @Override
    protected void onInit() {
        super.onInit();

        final ResourceGroup group = this.groupComposite.getResourceGroup();

        HLayout spacer = new HLayout();
        spacer.setHeight(15);
        addMember(spacer);

        final EnhancedDynamicForm generalPropsForm = new EnhancedDynamicForm();

        List<FormItem> formItems = new ArrayList<FormItem>();

        // TODO: Uncomment the below header if we decide to add other stuff to this page besides the general props.
        //HeaderItem headerItem = new HeaderItem("header", "General Properties");
        //headerItem.setValue("General Properties");
        //formItems.add(headerItem);        

        boolean dynamic = (group.getGroupDefinition() != null);

        final FormItem nameItem = (dynamic) ? new StaticTextItem() : new TogglableTextItem();
        nameItem.setName("name");
        nameItem.setTitle("Name");
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
                    OverviewView.this.resourceGroupService.updateResourceGroup(group, new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError("Failed to change name of Resource group with id "
                                                + group.getId()
                                                + " from \"" + oldName + "\" to \"" + newName + "\".", caught);
                            // We failed to update it on the Server, so change back the ResourceGroup and the form item
                            // to the original value.
                            group.setName(oldName);
                            nameItem.setValue(oldName);
                        }

                        public void onSuccess(Void result) {
                            CoreGUI.getMessageCenter().notify(new Message("Name of Resource group with id "
                                                + group.getId() + " was changed from \""
                                                + oldName + "\" to \"" + newName + "\".", Message.Severity.Info));
                        }
                    });
                }
            });
        }
        formItems.add(nameItem);

        StaticTextItem typeItem = new StaticTextItem("memberType", "Member Type");
        ResourceType type = group.getResourceType();
        if (type != null) {
            typeItem.setTooltip("Plugin: " + type.getPlugin() + "\n<br>" + "Type: " + type.getName());
            typeItem.setValue(type.getName() + " (" + type.getPlugin() + ")");
        } else {
            typeItem.setValue("<i>Mixed</i>");
        }
        formItems.add(typeItem);

        StaticTextItem countItem = new StaticTextItem("memberCount", "Member Count");
        long memberCount = this.groupComposite.getImplicitUp() + this.groupComposite.getImplicitDown();
        countItem.setValue(memberCount);
        formItems.add(countItem);

        final FormItem descriptionItem = (dynamic) ? new StaticTextItem() : new TogglableTextItem();
        descriptionItem.setName("description");
        descriptionItem.setTitle("Description");        
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
                    OverviewView.this.resourceGroupService.updateResourceGroup(group, new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError("Failed to change description of Resource group with id "
                                                + group.getId()
                                                + " from \"" + oldDescription + "\" to \"" + newDescription + "\".", caught);
                            // We failed to update it on the Server, so change back the ResourceGroup and the form item
                            // to the original value.
                            group.setDescription(oldDescription);
                            descriptionItem.setValue(oldDescription);
                        }

                        public void onSuccess(Void result) {
                            CoreGUI.getMessageCenter().notify(new Message("Description of Resource group with id "
                                                + group.getId() + " was changed from \""
                                                + oldDescription + "\" to \"" + newDescription + "\".", Message.Severity.Info));
                        }
                    });
                }
            });
        }
        formItems.add(descriptionItem);

        final FormItem locationItem = (dynamic) ? new StaticTextItem() : new TogglableTextItem();
        locationItem.setName("location");
        locationItem.setTitle("Location");
        locationItem.setValue(group.getLocation());
        if (locationItem instanceof TogglableTextItem) {
            final TogglableTextItem togglableLocationItem = (TogglableTextItem) locationItem;
            togglableLocationItem.addValueUpdatedHandler(new ValueUpdatedHandler() {
                public void onValueUpdated(final String newLocation) {
                    final String oldLocation = group.getLocation();
                    if (newLocation.equals(oldLocation)) {
                        return;
                    }
                    group.setLocation(newLocation);
                    OverviewView.this.resourceGroupService.updateResourceGroup(group, new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError("Failed to change location of Resource group with id "
                                                + group.getId()
                                                + " from \"" + oldLocation + "\" to \"" + newLocation + "\".", caught);
                            // We failed to update it on the Server, so change back the ResourceGroup and the form item
                            // to the original value.
                            group.setLocation(oldLocation);
                            locationItem.setValue(oldLocation);
                        }

                        public void onSuccess(Void result) {
                            CoreGUI.getMessageCenter().notify(new Message("Location of Resource group with id "
                                                + group.getId() + " was changed from \""
                                                + oldLocation + "\" to \"" + newLocation + "\".", Message.Severity.Info));
                        }
                    });
                }
            });
        }
        formItems.add(locationItem);

        StaticTextItem dynamicItem = new StaticTextItem("dynamic", "Dynamic?");
        dynamicItem.setValue(dynamic ? "yes" : "no");
        formItems.add(dynamicItem);

        StaticTextItem recursiveItem = new StaticTextItem("recursive", "Recursive?");
        recursiveItem.setValue((group.isRecursive()) ? "yes" : "no");
        formItems.add(recursiveItem);

        StaticTextItem createdItem = new StaticTextItem("created", "Created");
        createdItem.setValue(new Date(group.getCtime()));
        formItems.add(createdItem);

        StaticTextItem lastModifiedItem = new StaticTextItem("lastModified", "Last Modified");
        lastModifiedItem.setValue(new Date(group.getMtime()));
        formItems.add(lastModifiedItem);

        StaticTextItem lastModifiedByItem = new StaticTextItem("lastModifiedBy", "Last Modified By");
        lastModifiedByItem.setValue(group.getModifiedBy());
        formItems.add(lastModifiedByItem);

        if (dynamic) {
            StaticTextItem groupDefinitionItem = new StaticTextItem("groupDefinition", "Group Definition");
            GroupDefinition groupDefinition = group.getGroupDefinition();
            // TODO (ips): Make this a link to the group def.
            groupDefinitionItem.setValue(groupDefinition.getName());
            formItems.add(groupDefinitionItem);
        }
        
        generalPropsForm.setItems(formItems.toArray(new FormItem[formItems.size()]));
        addMember(generalPropsForm);

        if (dynamic) {
            spacer = new HLayout();
            spacer.setHeight(10);
            addMember(spacer);
            
            HTMLFlow note = new HTMLFlow();
            note.setContents("<b>*</b> Dynamic group names and descriptions are managed, and therefore are not editable.");
            note.setAlign(Alignment.CENTER);
            addMember(note);
        }
    }
}
