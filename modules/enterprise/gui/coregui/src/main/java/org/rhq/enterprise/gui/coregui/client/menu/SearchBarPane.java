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
package org.rhq.enterprise.gui.coregui.client.menu;

import com.google.gwt.user.client.ui.TextBox;
import com.smartgwt.client.types.FormLayoutType;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuButton;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;

import java.util.LinkedHashMap;

/**
 * @author Greg Hinkle
 */
public class SearchBarPane extends HLayout {

    public SearchBarPane() {
        super();
        setWidth100();
        setHeight(45);
    }

    @Override
    protected void onInit() {
        super.onInit();

        DynamicForm form = new DynamicForm();
        form.setNumCols(4);
        form.setItemLayout(FormLayoutType.ABSOLUTE);
//        form.setWidth100();


        LinkedHashMap<String, String> values = new LinkedHashMap<String, String>();
        values.put("Resources", "Resources");
        values.put("Resources Groups", "Resources Groups");
        values.put("Bundles", "Bundles");
        values.put("Packages", "Package");
        values.put("Users", "Users");
        values.put("Roles", "Roles");

        SelectItem searchType = new SelectItem("searchType", "Search");
        searchType.setWidth(150);
        searchType.setValueMap(values);


        TextItem query = new TextItem("query");
        query.setLeft(210);
        query.setWidth(400);
        query.setShowTitle(false);

        ButtonItem search = new ButtonItem("Search", "Search");
        search.setLeft(620);
        search.setIcon(Window.getImgURL("[SKIN]/actions/view.png"));


        form.setItems(searchType, query, search);

        addMember(form);
    }
}
