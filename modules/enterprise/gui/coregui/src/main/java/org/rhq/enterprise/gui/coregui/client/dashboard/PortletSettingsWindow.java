/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.dashboard;

import com.smartgwt.client.types.LayoutPolicy;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * This is a window for displaying portlet settings. The window contains a form which in turn will contain the
 * widgets for a portlet's settings. Those widgets will have to be passed to the window since the window is intended
 * to support all portlets. The layout and sizing strategy for this window is to be as large as the widgets contained
 * within it. And the form will stretch and resize as the window is resized. This flexible layout strategy is used
 * since there are different settings (with different widgets) for different portlets which will in turn result in
 * different sizes for the parent window.
 *
 * @author John Sanda
 */
public class PortletSettingsWindow extends Window {

    private String portletTitle;

    public PortletSettingsWindow(String portletTitle) {
        this.portletTitle = portletTitle;
        setTitle("RHQ Dashboard Settings: " + portletTitle);
        setOverflow(Overflow.VISIBLE);
        setAutoSize(true);
        setCanDragResize(true);
        setCanDragReposition(true);
    }

    @Override
    protected void onInit() {
        super.onInit();

        VLayout layout = new VLayout();
        layout.setWidth100();
        layout.setHeight100();
        layout.setPadding(15);
        layout.setLayoutMargin(20);
        layout.setVPolicy(LayoutPolicy.FILL);

        final DynamicForm form = new DynamicForm();
        form.setGroupTitle("Display Settings");
        form.setIsGroup(true);
        form.setNumCols(2);
        form.setWidth100();
        form.setHeight100();
        form.setCellPadding(5);

        TextItem username = new TextItem();
        username.setTitle("Username");

        TextItem password = new TextItem();
        password.setTitle("Password");

        form.setFields(username, password);

        layout.addMember(form);
        addItem(layout);
    }

}
