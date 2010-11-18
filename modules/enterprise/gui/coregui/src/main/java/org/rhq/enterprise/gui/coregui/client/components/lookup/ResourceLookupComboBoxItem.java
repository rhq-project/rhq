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
package org.rhq.enterprise.gui.coregui.client.components.lookup;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.TextMatchStyle;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.grid.ListGridField;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDatasource;

/**
 * @author Greg Hinkle
 */
public class ResourceLookupComboBoxItem extends ComboBoxItem {
    private static final Messages MSG = CoreGUI.getMessages();

    public ResourceLookupComboBoxItem(String name, String title) {
        super(name, title);
        setWidth(300);
        setHint("resource search");
        setShowHintInField(false);

        setOptionDataSource(new ResourceDatasource());

        ListGridField nameField = new ListGridField("name", MSG.common_title_name(), 250);
        ListGridField descriptionField = new ListGridField("description", MSG.common_title_description());
        ListGridField typeNameField = new ListGridField("typeName", MSG.common_title_type(), 130);
        ListGridField pluginNameField = new ListGridField("pluginName", MSG.common_title_plugin(), 100);
        ListGridField categoryField = new ListGridField("category", MSG.common_title_category(), 60);
        ListGridField availabilityField = new ListGridField("currentAvailability", MSG.common_title_availability(), 55);
        availabilityField.setAlign(Alignment.CENTER);

        setPickListFields(nameField, descriptionField, typeNameField, pluginNameField, categoryField, availabilityField);

        setValueField("id");
        setDisplayField("name");
        setPickListWidth(800);
        setTextMatchStyle(TextMatchStyle.SUBSTRING);
        setCompleteOnTab(true);

    }
}
