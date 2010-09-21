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

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.ValuesManager;
import com.smartgwt.client.widgets.form.fields.AutoFitTextAreaItem;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;

import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;
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

            form.setFields(name, description, location, recursive);
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

        return group;
    }
}
