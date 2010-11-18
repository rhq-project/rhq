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
package org.rhq.enterprise.gui.coregui.client.admin.roles;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.ItemChangedEvent;
import com.smartgwt.client.widgets.form.events.ItemChangedHandler;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.HeaderItem;

import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author Greg Hinkle
 */
public class PermissionEditorView extends CanvasItem {

    private Set<Permission> selectedPermissions = EnumSet.noneOf(Permission.class);

    private DynamicForm form;
    private RoleEditView roleEditView;

    public PermissionEditorView(String locatorId, String name, String title, RoleEditView roleEditView) {
        super(name, title);
        this.roleEditView = roleEditView;

        setCanvas(buildForm(locatorId));
    }

    public Canvas buildForm(String locatorId) {
        this.form = new LocatableDynamicForm(locatorId);
        this.form.setNumCols(8);
        this.form.setTitleWidth(80);
        this.form.setPadding(6);

        List<FormItem> items = new ArrayList<FormItem>();

        HeaderItem globalPermsHeader = new HeaderItem("globalPermissions", "Global Permissions");
        globalPermsHeader.setValue("Global Permissions");
        items.add(globalPermsHeader);
        for (Permission permission : Permission.GLOBAL_ALL) {
            CheckboxItem checkboxItem = new CheckboxItem(permission.name(), permission.name());            
            items.add(checkboxItem);
        }

        HeaderItem resourcePermsHeader = new HeaderItem("resourcePermissions", "Resource Permissions");
        resourcePermsHeader.setValue("Resource Permissions");
        items.add(resourcePermsHeader);
        for (Permission permission : Permission.RESOURCE_ALL) {
            CheckboxItem checkboxItem = new CheckboxItem(permission.name(), permission.name());
            if (permission == Permission.VIEW_RESOURCE) {
                checkboxItem.setDisabled(true);
                checkboxItem.setValue(Boolean.TRUE);
            }
            items.add(checkboxItem);
        }

        form.setItems(items.toArray(new FormItem[items.size()]));

        return form;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.selectedPermissions = permissions;
        for (Permission p : Permission.values()) {
            form.setValue(p.name(), permissions.contains(p));
        }

        form.addItemChangedHandler(new ItemChangedHandler() {
            public void onItemChanged(ItemChangedEvent itemChangedEvent) {
                for (Permission p : Permission.values()) {
                    if ((Boolean) form.getValue(p.name())) {
                        selectedPermissions.add(p);
                    } else {
                        selectedPermissions.remove(p);
                    }
                }
                roleEditView.onItemChanged();
            }
        });

        form.markForRedraw();
    }

    public Set<Permission> getPermissions() {
        return selectedPermissions;
    }
    
}
