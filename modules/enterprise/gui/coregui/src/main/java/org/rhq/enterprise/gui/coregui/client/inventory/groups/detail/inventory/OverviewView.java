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

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;

import java.util.ArrayList;
import java.util.Date;

/**
 * The group Inventory>Overview tab.
 *
 * @author Ian Springer
 */
public class OverviewView extends VLayout {
    private ResourceGroupComposite groupComposite;

    public OverviewView(ResourceGroupComposite groupComposite) {
        super();
        this.groupComposite = groupComposite;
    }

    @Override
    protected void onInit() {
        super.onInit();

        ResourceGroup group = this.groupComposite.getResourceGroup();

        HLayout spacer = new HLayout();
        spacer.setHeight(15);
        addMember(spacer);

        DynamicForm generalPropsForm = new DynamicForm();
        generalPropsForm.setNumCols(4);
        generalPropsForm.setColWidths("25%", "25%", "25%", "25%");
        generalPropsForm.setWrapItemTitles(false);
        setWidth("90%");

        ArrayList<FormItem> formItems = new ArrayList<FormItem>();
        ArrayList<String> itemIds = new ArrayList<String>();

        //HeaderItem headerItem = new HeaderItem("header", "General Properties");
        //headerItem.setValue("General Properties");
        //formItems.add(headerItem);

        StaticTextItem nameItem = new StaticTextItem("nameItem", "Name");
        nameItem.setValue(group.getName());
        formItems.add(nameItem);
        itemIds.add(nameItem.getName());

        StaticTextItem typeItem = new StaticTextItem("typeItem", "Member Type");
        ResourceType type = group.getResourceType();
        if (type != null) {
            typeItem.setTooltip("Plugin: " + type.getPlugin() + "\n<br>" + "Type: " + type.getName());
            typeItem.setValue(type.getName() + " (" + type.getPlugin() + ")");
        } else {
            typeItem.setValue("<i>Mixed</i>");
        }
        formItems.add(typeItem);
        itemIds.add(typeItem.getName());

        StaticTextItem countItem = new StaticTextItem("countItem", "Member Count");
        long memberCount = this.groupComposite.getImplicitUp() + this.groupComposite.getImplicitDown();
        countItem.setValue(memberCount);
        formItems.add(countItem);
        itemIds.add(countItem.getName());

        StaticTextItem descriptionItem = new StaticTextItem("descriptionItem", "Description");
        String description = group.getDescription();
        descriptionItem.setValue((description != null) ? description : "<i>none</i>");
        formItems.add(descriptionItem);
        itemIds.add(descriptionItem.getName());

        StaticTextItem dynamicItem = new StaticTextItem("dynamicItem", "Dynamic?");
        dynamicItem.setValue((group.getGroupDefinition() != null) ? "yes" : "no");
        formItems.add(dynamicItem);
        itemIds.add(dynamicItem.getName());

        StaticTextItem recursiveItem = new StaticTextItem("recursiveItem", "Recursive?");
        recursiveItem.setValue((group.isRecursive()) ? "yes" : "no");
        formItems.add(recursiveItem);
        itemIds.add(recursiveItem.getName());

        StaticTextItem createdItem = new StaticTextItem("createdItem", "Created");
        createdItem.setValue(new Date(group.getCtime()));
        formItems.add(createdItem);
        itemIds.add(createdItem.getName());

        StaticTextItem lastModifiedItem = new StaticTextItem("lastModifiedItem", "Last Modified");
        lastModifiedItem.setValue(new Date(group.getMtime()));
        formItems.add(lastModifiedItem);
        itemIds.add(lastModifiedItem.getName());

        StaticTextItem lastModifiedByItem = new StaticTextItem("lastModifiedByItem", "Last Modified By");
        lastModifiedByItem.setValue(group.getModifiedBy());
        formItems.add(lastModifiedByItem);
        itemIds.add(lastModifiedByItem.getName());

        StaticTextItem groupDefinitionItem = new StaticTextItem("groupDefinitionItem", "Group Definition");
        GroupDefinition groupDefinition = group.getGroupDefinition();
        // TODO (ips): Make this a link to the group def.
        groupDefinitionItem.setValue((groupDefinition != null) ? groupDefinition.getName() : "<i>none</i>");
        formItems.add(groupDefinitionItem);
        itemIds.add(groupDefinitionItem.getName());
        
        generalPropsForm.setItems(formItems.toArray(new FormItem[formItems.size()]));
        addMember(generalPropsForm);

        if (groupDefinition != null) {
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
