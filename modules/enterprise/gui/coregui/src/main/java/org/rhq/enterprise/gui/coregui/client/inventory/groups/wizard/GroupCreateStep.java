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
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.IPickTreeItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.coregui.client.components.form.RadioGroupWithComponentsItem;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypePluginTreeDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author Greg Hinkle
 */
public class GroupCreateStep extends AbstractWizardStep {

    private DynamicForm form;

    public Canvas getCanvas() {

        if (form == null) {

            form = new LocatableDynamicForm("GroupCreate");
            form.setValuesManager(new ValuesManager());
            form.setWidth100();
            form.setNumCols(2);

            TextItem name = new TextItem("name", "Name");
            name.setRequired(true);
            name.setWidth(300);

            TextAreaItem description = new AutoFitTextAreaItem("description", "Description");
            description.setWidth(300);

            TextItem location = new TextItem("location", "Location");
            location.setWidth(300);

            CheckboxItem recursive = new CheckboxItem("recursive", "Recursive");

            LinkedHashMap<String, Canvas> options = new LinkedHashMap<String, Canvas>();
            options.put("Mixed", null);

            IPickTreeItem typeSelectItem = new IPickTreeItem("type", "Type");
            typeSelectItem.setDataSource(new ResourceTypePluginTreeDataSource());
            typeSelectItem.setValueField("id");
            typeSelectItem.setCanSelectParentItems(true);
            typeSelectItem.setLoadDataOnDemand(false);
            typeSelectItem.setEmptyMenuMessage("Loading...");
            typeSelectItem.setShowIcons(true);

            DynamicForm form2 = new LocatableDynamicForm("TypeTree");
            form2.setValuesManager(form.getValuesManager());
            form2.setFields(typeSelectItem);
            options.put("Compatible", form2);

            RadioGroupWithComponentsItem kind = new RadioGroupWithComponentsItem("groupType", "Group Type", options,
                form);
            kind.setValue("Mixed");
            form.setFields(name, description, location, recursive, kind);

        }
        return form;
    }

    public boolean nextPage() {
        boolean valid = form.validate();
        if (valid) {
            RadioGroupWithComponentsItem kind = (RadioGroupWithComponentsItem) form.getField("groupType");
            if ("Compatible".equals(kind.getSelected())) {
                DynamicForm form2 = (DynamicForm) kind.getSelectedComponent();
                valid = (null != form2.getValue("type"));
            }
        }

        return valid;
    }

    public String getName() {
        return "Group Settings";
    }

    public ResourceGroup getGroup() {
        ResourceGroup group = new ResourceGroup(form.getValueAsString("name"));
        group.setDescription(form.getValueAsString("description"));
        group.setLocation(form.getValueAsString("location"));
        group.setRecursive(form.getValue("recursive") != null ? true : false);

        RadioGroupWithComponentsItem kind = (RadioGroupWithComponentsItem) form.getField("groupType");
        if ("Compatible".equals(kind.getSelected())) {
            DynamicForm form2 = (DynamicForm) kind.getSelectedComponent();
            if (null != form2.getValue("type")) {
                ResourceType rt = new ResourceType();
                rt.setId(Integer.parseInt(form2.getValueAsString("type")));
                group.setResourceType(rt);
            }
        }

        return group;
    }
}
