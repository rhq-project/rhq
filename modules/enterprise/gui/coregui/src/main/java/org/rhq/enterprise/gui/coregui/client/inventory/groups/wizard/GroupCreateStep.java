/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.wizard;

import java.util.LinkedHashMap;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.ValuesManager;
import com.smartgwt.client.widgets.form.fields.AutoFitTextAreaItem;
import com.smartgwt.client.widgets.form.fields.BooleanItem;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.IPickTreeItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.coregui.client.components.form.RadioGroupWithComponentsItem;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypePluginTreeDataSource;

/**
 * @author Greg Hinkle
 */
public class GroupCreateStep implements WizardStep {


    private DynamicForm form;

    public Canvas getCanvas() {


        if (form == null) {

            form = new DynamicForm();
            form.setValuesManager(new ValuesManager());
            form.setNumCols(4);

            TextItem name = new TextItem("name", "Name");
            name.setRequired(true);

            TextItem location = new TextItem("location", "Location");

            TextAreaItem description = new AutoFitTextAreaItem("description", "Description");

            CheckboxItem recursive = new CheckboxItem("recursive","Recursive");

            LinkedHashMap<String, Canvas> options = new LinkedHashMap<String, Canvas>();

            options.put("Mixed", null);

            IPickTreeItem typeSelectItem = new IPickTreeItem("type", "Type");
            typeSelectItem.setDataSource(new ResourceTypePluginTreeDataSource());
            typeSelectItem.setValueField("id");
            typeSelectItem.setCanSelectParentItems(true);
            typeSelectItem.setLoadDataOnDemand(false);
            typeSelectItem.setEmptyMenuMessage("Loading...");
            typeSelectItem.setShowIcons(true);

            DynamicForm form2 = new DynamicForm();
            form2.setValuesManager(form.getValuesManager());
            form2.setFields(typeSelectItem);
            options.put("Compatible", form2);


            RadioGroupWithComponentsItem kind = new RadioGroupWithComponentsItem("kind", "Group Type", options, form);
            kind.setValue("Mixed Resources");

            form.setFields(name, location, description, recursive, kind);

        }
        return form;
    }

    public boolean nextPage() {
        return form.validate();
    }

    public String getName() {
        return "Group Settings";
    }

    public ResourceGroup getGroup() {
        ResourceGroup group = new ResourceGroup(form.getValueAsString("name"));
        group.setDescription(form.getValueAsString("description"));
        group.setLocation(form.getValueAsString("location"));
        group.setRecursive(form.getValue("recursive") != null ? true : false);

        if (form.getValue("type") != null) {
            ResourceType type = new ResourceType();
            type.setId(Integer.parseInt(form.getValueAsString("type")));
            group.setResourceType(type);
        }
        return group;
    }
}
