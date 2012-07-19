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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Side;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.EditCompleteEvent;
import com.smartgwt.client.widgets.grid.events.EditCompleteHandler;
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

    private static final String PROPERTY_NAME = "propertyName";
    private static final String PROPERTY_VALUE = "propertyValue";

    private InstallerGWTServiceAsync installerService = InstallerGWTServiceAsync.Util.getInstance();
    private HashMap<String, String> serverProperties = new HashMap<String, String>();
    private HashMap<String, String> originalProperties = new HashMap<String, String>();

    private ListGrid advancedPropertyItemGrid;

    public void onModuleLoad() {
        Canvas header = createHeader();
        Canvas installButton = createMainInstallButton();
        Canvas tabSet = createTabSet();

        VLayout layout = new VLayout();
        layout.setWidth100();
        layout.setHeight100();
        layout.setLayoutMargin(10);
        layout.setMembersMargin(5);
        layout.setDefaultLayoutAlign(Alignment.CENTER);
        layout.addMember(header);
        layout.addMember(installButton);
        layout.addMember(tabSet);
        layout.draw();

        // Remove loading image in case we don't completely cover it
        Element loadingPanel = DOM.getElementById("Loading-Panel");
        loadingPanel.removeFromParent();

        // get the server properties from the server
        loadServerProperties();

    }

    private void loadServerProperties() {
        // load the initial server properties
        installerService.getServerProperties(new AsyncCallback<HashMap<String, String>>() {
            public void onSuccess(HashMap<String, String> result) {
                if (result.size() == 0) {
                    SC.say("Initial server properties are missing.");
                }
                serverProperties.clear();
                serverProperties.putAll(result);

                // remember these original properties in case the user wants to reset them back
                originalProperties.clear();
                originalProperties.putAll(result);

                // refresh the advanced view with the new data
                refreshAdvancedView();
            }

            public void onFailure(Throwable caught) {
                SC.say("Cannot load properties: " + caught);
            }
        });
    }

    private void refreshAdvancedView() {
        ArrayList<ListGridRecord> records = new ArrayList<ListGridRecord>();
        for (Map.Entry<String, String> entry : serverProperties.entrySet()) {
            ListGridRecord record = new ListGridRecord();
            record.setAttribute(PROPERTY_NAME, entry.getKey());
            record.setAttribute(PROPERTY_VALUE, entry.getValue());
            records.add(record);
        }
        advancedPropertyItemGrid.setData(records.toArray(new ListGridRecord[records.size()]));
        advancedPropertyItemGrid.markForRedraw();
        return;
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

    private Canvas createMainInstallButton() {
        Button installButton = new Button(MSG.start_installation_label());
        installButton.setWrap(false);
        installButton.setAutoFit(true);
        installButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                SC.say("TODO: this should start the install");
            }
        });

        return installButton;
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

        final Tab advancedViewTab = new Tab(MSG.tab_advancedView());
        Canvas advancedView = createAdvancedView();
        advancedViewTab.setPane(advancedView);

        topTabSet.addTab(welcomeTab);
        topTabSet.addTab(databaseTab);
        topTabSet.addTab(systemSettingsTab);
        topTabSet.addTab(advancedViewTab);

        return topTabSet;
    }

    private Canvas createAdvancedView() {
        VLayout layout = new VLayout();

        ToolStrip strip = new ToolStrip();
        strip.setWidth100();

        IButton saveButton = new IButton(MSG.save_label());
        saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                installerService.saveServerProperties(serverProperties, new AsyncCallback<Void>() {
                    public void onSuccess(Void result) {
                        originalProperties.clear();
                        originalProperties.putAll(serverProperties);
                        SC.say("Properties saved to server");
                    }

                    public void onFailure(Throwable caught) {
                        SC.say("Failed to save properties to server");
                    }
                });
            }
        });
        IButton resetButton = new IButton(MSG.reset_label());
        resetButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                serverProperties.clear();
                serverProperties.putAll(originalProperties);
                refreshAdvancedView();
            }
        });
        strip.addMember(saveButton);
        strip.addMember(resetButton);
        layout.addMember(strip);

        advancedPropertyItemGrid = new ListGrid();
        advancedPropertyItemGrid.setWidth100();
        advancedPropertyItemGrid.setHeight100();
        advancedPropertyItemGrid.setData(new ListGridRecord[0]);

        ListGridField nameField = new ListGridField(PROPERTY_NAME, MSG.property_name_label());
        nameField.setCanEdit(false);

        ListGridField valueField = new ListGridField(PROPERTY_VALUE, MSG.property_value_label());
        valueField.setCanEdit(true);

        advancedPropertyItemGrid.setFields(nameField, valueField);
        advancedPropertyItemGrid.setSortField(PROPERTY_NAME);

        advancedPropertyItemGrid.addEditCompleteHandler(new EditCompleteHandler() {
            public void onEditComplete(EditCompleteEvent event) {
                String newValue = (String) event.getNewValues().values().iterator().next().toString();
                String changedProperty = event.getOldRecord().getAttribute(PROPERTY_NAME);
                serverProperties.put(changedProperty, newValue);
            }
        });

        layout.addMember(advancedPropertyItemGrid);

        return layout;
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
