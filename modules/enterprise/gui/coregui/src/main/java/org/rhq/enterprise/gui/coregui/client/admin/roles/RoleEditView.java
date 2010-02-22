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

import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SubmitItem;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * @author Greg Hinkle
 */
public class RoleEditView extends VLayout {

    private Label message = new Label("Select a role to edit...");
    private DynamicForm form;
    @Override
    protected void onInit() {
        super.onInit();

//        addMember(message);

        addMember(buildRoleForm());
        
    }

    DynamicForm buildRoleForm() {

        form = new DynamicForm();
        form.setAutoFetchData(true);
        form.setDataSource(RolesDataSource.getInstance());

        form.setUseAllDataSourceFields(true);

        form.setItems(new SubmitItem("save","Save"));
        form.setItems(new SubmitItem("cancel","Cancel"));
//        form.hide();

        return form;
    }

    public void editRecord(Record record) {

        form.editRecord(record);
//        message.hide();
        form.show();
        form.redraw();
    }

    public void editNone() {
//        form.hide();
//        message.show();

    }
}
