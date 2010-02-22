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
package org.rhq.enterprise.gui.coregui.client.admin.users;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.coregui.client.admin.roles.RolesView;

import com.google.gwt.user.client.ui.Widget;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.docs.FormLayout;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.BooleanItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SectionItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;

/**
 * @author Greg Hinkle
 */
public class UserEditView extends HLayout {

    private Subject subject;
    private Subject originalSubject;


    @Override
    protected void onInit() {
        super.onInit();

        setWidth100();
        setHeight100();

        SectionStack sectionStack = new SectionStack();
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);

        sectionStack.setWidth100();
        sectionStack.setHeight100();

        SectionStackSection subjectSection = new SectionStackSection("Edit Subject");
        subjectSection.setItems(buildSubjectEditor());

        SectionStackSection roleSection = new SectionStackSection("Edit Subject's Roles");
        roleSection.setItems(buildRoleEditor());

        sectionStack.setSections(subjectSection, roleSection );

        addMember(buildSubjectEditor());

    }

    private Canvas buildSubjectEditor() {
        DynamicForm form = new DynamicForm();
        form.setWidth100();

        SectionItem userEditSection = new SectionItem("userEditSection", "Edit User");


//        TextItem firstName = new TextItem("firstName", "First Name");
//
//        TextItem lastName = new TextItem("lastName", "Last Name");
//
//        TextItem email = new TextItem("email", "Email Address");
//
//
//        BooleanItem enabled = new BooleanItem();
//        enabled.setName("enabled");
//        enabled.setTitle("Enabled");
//
//        TextItem username = new TextItem("username", "Username");
//
//        TextItem phone = new TextItem("phone", "Phone");


//        form.setField//s(userEditSection);


        UsersDataSource ds = UsersDataSource.getInstance();
        ds.filterData(new Criteria("id", "2"));

        form.setUseAllDataSourceFields(true);
        form.setDataSource(ds);

        form.setAutoFetchData(true);

        form.getField("id").hide();

        FormItem[] forms = form.getFields();

        


        return form;


    }


    private Canvas buildRoleEditor() {

        RolesView rv = new RolesView();
        // set subject criteria
        return rv;

    }

}
