/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.installer.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Side;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.enterprise.gui.installer.client.gwt.InstallerGWTServiceAsync;

/**
 * The GWT {@link EntryPoint entry point} to the RHQ Installer GUI.
 *
 * @author John Mazzitelli
 */
public class Installer implements EntryPoint {

    // This must come first to ensure proper I18N class loading for dev mode
    private static final Messages MSG = GWT.create(Messages.class);

    private InstallerGWTServiceAsync installerService = InstallerGWTServiceAsync.Util.getInstance();

    public void onModuleLoad() {
        Canvas header = createHeader();
        TabSet tabSet = createTabSet();

        VLayout layout = new VLayout();
        layout.setWidth100();
        layout.setHeight100();
        layout.setLayoutMargin(10);
        layout.setMembersMargin(5);
        layout.setDefaultLayoutAlign(Alignment.CENTER);
        layout.addMember(header);
        layout.addMember(tabSet);
        layout.draw();

        // Remove loading image in case we don't completely cover it
        Element loadingPanel = DOM.getElementById("Loading-Panel");
        loadingPanel.removeFromParent();
    }

    private Canvas createHeader() {
        ToolStrip strip = new ToolStrip();
        strip.setWidth100();
        strip.setAlign(Alignment.CENTER);

        Label title = new Label();
        title.setWidth100();
        title.setHeight100();
        title.setWrap(false);
        title.setValign(VerticalAlignment.CENTER);
        title.setAlign(Alignment.CENTER);
        title.setContents("<span style=\"font-size:16pt;font-weight:bold;\">" + MSG.welcome_title() + "</span>");
        strip.addMember(title);
        return strip;
    }

    private TabSet createTabSet() {
        final TabSet topTabSet = new TabSet();
        topTabSet.setTabBarPosition(Side.TOP);
        topTabSet.setTabBarAlign(Side.LEFT);
        topTabSet.setWidth("80%");
        topTabSet.setHeight("75%");

        final Tab welcomeTab = new Tab(MSG.tab_welcome());
        Label welcomeLabel = new Label(MSG.tab_welcome_content());
        welcomeTab.setPane(welcomeLabel);

        final Tab databaseTab = new Tab(MSG.tab_database());
        DynamicForm databaseForm = createDatabaseForm();
        databaseTab.setPane(databaseForm);

        final Tab systemSettingsTab = new Tab(MSG.tab_systemSettings());
        DynamicForm systemSettingsForm = createSystemSettingsForm();
        systemSettingsTab.setPane(systemSettingsForm);

        topTabSet.addTab(welcomeTab);
        topTabSet.addTab(databaseTab);
        topTabSet.addTab(systemSettingsTab);

        return topTabSet;
    }

    private DynamicForm createSystemSettingsForm() {
        DynamicForm systemSettingsForm = new DynamicForm();
        TextItem nameTextItem = new TextItem();
        nameTextItem.setTitle("Your Name");
        systemSettingsForm.setFields(nameTextItem);
        return systemSettingsForm;
    }

    private DynamicForm createDatabaseForm() {
        DynamicForm databaseForm = new DynamicForm();
        TextItem dbUsernameTextItem = new TextItem();
        dbUsernameTextItem.setTitle("Database User");
        databaseForm.setFields(dbUsernameTextItem);
        return databaseForm;
    }
}
