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
import java.util.LinkedHashMap;
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
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.SpinnerItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
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
import org.rhq.enterprise.gui.installer.client.shared.ServerProperties;

/**
 * The GWT {@link EntryPoint entry point} to the RHQ Installer GUI.
 *
 * @author John Mazzitelli
 */
public class Installer implements EntryPoint {

    // This must come first to ensure proper I18N class loading for dev mode
    public static final Messages MSG = GWT.create(Messages.class);
    public static final ServerPropertiesMessages PROPS_MSG = GWT.create(ServerPropertiesMessages.class);

    private static final String PROPERTY_NAME = "propertyName";
    private static final String PROPERTY_VALUE = "propertyValue";

    private InstallerGWTServiceAsync installerService = InstallerGWTServiceAsync.Util.getInstance();
    private HashMap<String, String> serverProperties = new HashMap<String, String>();
    private HashMap<String, String> originalProperties = new HashMap<String, String>();

    private ListGrid advancedPropertyItemGrid;
    private Button mainInstallButton;
    private TextItem dbConnectionUrl;
    private TextItem dbUsername;
    private PasswordItem dbPassword;
    private SelectItem dbExistingSchemaOption;
    private ButtonItem testConnectionButton;
    private SelectItem dbType;
    private TextItem serverSettingServerName;
    private TextItem serverSettingPublicAddress;
    private SpinnerItem serverSettingWebHttpPort;
    private SpinnerItem serverSettingWebSecureHttpPort;
    private SelectItem registeredServersSelection;
    private TextItem serverSettingEmailSMTPHostname;
    private TextItem serverSettingEmailFromAddress;

    public void onModuleLoad() {
        Canvas header = createHeader();
        mainInstallButton = createMainInstallButton();
        Canvas tabSet = createTabSet();

        VLayout layout = new VLayout();
        layout.setWidth100();
        layout.setHeight100();
        layout.setLayoutMargin(10);
        layout.setMembersMargin(5);
        layout.setDefaultLayoutAlign(Alignment.CENTER);
        layout.addMember(header);
        layout.addMember(mainInstallButton);
        layout.addMember(tabSet);
        layout.draw();

        // Remove loading image in case we don't completely cover it
        Element loadingPanel = DOM.getElementById("Loading-Panel");
        loadingPanel.removeFromParent();

        // get the server properties from the server
        loadServerProperties();

    }

    private void updateServerProperty(String name, Object value) {
        serverProperties.put(name, value == null ? "" : value.toString());
        refreshAdvancedView();
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

                // refresh the simple view with the new data
                refreshSimpleView();

                // refresh the advanced view with the new data
                refreshAdvancedView();
            }

            public void onFailure(Throwable caught) {
                SC.say("Cannot load properties: " + caught);
            }
        });
    }

    private void refreshSimpleView() {
        // DB SETTINGS
        dbType.setValue(serverProperties.get(ServerProperties.PROP_DATABASE_TYPE));
        dbConnectionUrl.setValue(serverProperties.get(ServerProperties.PROP_DATABASE_CONNECTION_URL));
        dbUsername.setValue(serverProperties.get(ServerProperties.PROP_DATABASE_USERNAME));
        // do not prefill the database password - force the user to know it and type it in for security purposes

        // SERVER SETTINGS
        serverSettingServerName.setValue(serverProperties.get(ServerProperties.PROP_HIGH_AVAILABILITY_NAME));
        serverSettingWebHttpPort.setValue(serverProperties.get(ServerProperties.PROP_WEB_HTTP_PORT));
        serverSettingWebSecureHttpPort.setValue(serverProperties.get(ServerProperties.PROP_WEB_HTTPS_PORT));
        serverSettingEmailSMTPHostname.setValue(serverProperties.get(ServerProperties.PROP_EMAIL_SMTP_HOST));
        serverSettingEmailFromAddress.setValue(serverProperties.get(ServerProperties.PROP_EMAIL_FROM_ADDRESS));

        dbExistingSchemaOption.hide();
        testConnectionButton.setIcon(null);
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

    private Button createMainInstallButton() {
        Button installButton = new Button(MSG.button_startInstallation());
        installButton.setWrap(false);
        installButton.setAutoFit(true);
        installButton.setDisabled(true); // we can't allow the user to install yet
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

        final Tab databaseTab = new Tab(MSG.tab_simpleView());
        Canvas databaseForm = createSimpleForm();
        databaseTab.setPane(databaseForm);

        final Tab advancedViewTab = new Tab(MSG.tab_advancedView());
        Canvas advancedView = createAdvancedView();
        advancedViewTab.setPane(advancedView);

        topTabSet.addTab(welcomeTab);
        topTabSet.addTab(databaseTab);
        topTabSet.addTab(advancedViewTab);

        return topTabSet;
    }

    private Canvas createAdvancedView() {
        VLayout layout = new VLayout();

        ToolStrip strip = new ToolStrip();
        strip.setWidth100();

        IButton saveButton = new IButton(MSG.button_save());
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
        IButton resetButton = new IButton(MSG.button_reset());
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
                refreshSimpleView();
            }
        });

        layout.addMember(advancedPropertyItemGrid);

        return layout;
    }

    private Canvas createSimpleForm() {

        final int fieldWidth = 250;

        ////////////////////////////////////////////////////////
        // The Database form

        final DynamicForm databaseForm = new DynamicForm();
        databaseForm.setAutoWidth();
        databaseForm.setPadding(5);
        databaseForm.setCellPadding(5);
        databaseForm.setWrapItemTitles(false);
        databaseForm.setIsGroup(true);
        databaseForm.setGroupTitle(MSG.tab_simpleView_database());

        dbConnectionUrl = new TextItem(ServerProperties.PROP_DATABASE_CONNECTION_URL,
            PROPS_MSG.rhq_server_database_connection_url());
        dbConnectionUrl.setWidth(fieldWidth);
        dbConnectionUrl.setValue("jdbc:postgresql://127.0.0.1:5432/rhq");
        dbConnectionUrl.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                updateServerProperty(ServerProperties.PROP_DATABASE_CONNECTION_URL, String.valueOf(event.getValue()));
            }
        });

        dbUsername = new TextItem(ServerProperties.PROP_DATABASE_USERNAME,
            PROPS_MSG.rhq_server_database_user_name());
        dbUsername.setWidth(fieldWidth);
        dbUsername.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                updateServerProperty(ServerProperties.PROP_DATABASE_USERNAME, String.valueOf(event.getValue()));
            }
        });

        dbPassword = new PasswordItem(ServerProperties.PROP_DATABASE_PASSWORD,
            PROPS_MSG.rhq_server_database_password());
        dbPassword.setWidth(fieldWidth);
        dbPassword.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                updateServerProperty(ServerProperties.PROP_DATABASE_PASSWORD, String.valueOf(event.getValue()));
            }
        });

        dbExistingSchemaOption = new SelectItem("existingSchemaOption", MSG.schema_update_question());
        final LinkedHashMap<String, String> schemaOpt = new LinkedHashMap<String, String>();
        schemaOpt.put("keep", MSG.schema_update_keep());
        schemaOpt.put("overwrite", MSG.schema_update_overwrite());
        schemaOpt.put("skip", MSG.schema_update_skip());
        dbExistingSchemaOption.setValueMap(schemaOpt);
        dbExistingSchemaOption.setDefaultToFirstOption(true);
        dbExistingSchemaOption.setVisible(false);
        dbExistingSchemaOption.setWidth(fieldWidth);
        dbExistingSchemaOption.setWrapTitle(true);

        testConnectionButton = new ButtonItem("testConnectionButton", MSG.button_testConnection());
        testConnectionButton.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
                final String connectionUrl = dbConnectionUrl.getValueAsString();
                final String username = dbUsername.getValueAsString();
                final String password = dbPassword.getValueAsString();

                installerService.testConnection(connectionUrl, username, password, new AsyncCallback<String>() {
                    public void onSuccess(String result) {
                        if (result != null) {
                            SC.say("Could not connect to the database: " + result);
                            testConnectionButton.setIcon("[SKIN]/actions/exclamation.png");
                            mainInstallButton.disable();
                            dbExistingSchemaOption.hide();
                        } else {
                            testConnectionButton.setIcon("[SKIN]/actions/ok.png");
                            installerService.isDatabaseSchemaExist(connectionUrl, username, password,
                                new AsyncCallback<Boolean>() {
                                    public void onSuccess(Boolean schemaExists) {
                                        if (schemaExists) {
                                            dbExistingSchemaOption.show();
                                        } else {
                                            dbExistingSchemaOption.hide();
                                        }
                                        mainInstallButton.enable();
                                    }

                                    public void onFailure(Throwable caught) {
                                        SC.say("Cannot determine the status of the database schema: " + caught);
                                    }
                                });
                        }
                    }

                    public void onFailure(Throwable caught) {
                        SC.say("Failed to test connection: " + caught.toString());
                        testConnectionButton.setIcon("[SKIN]/actions/exclamation.png");
                        mainInstallButton.disable();
                        dbExistingSchemaOption.hide();
                    }
                });

            }
        });

        dbType = new SelectItem(ServerProperties.PROP_DATABASE_TYPE,
            PROPS_MSG.rhq_server_database_type_mapping());
        final LinkedHashMap<String, String> dbs = new LinkedHashMap<String, String>();
        dbs.put("PostgreSQL", "PostgreSQL");
        dbs.put("Oracle", "Oracle");
        dbType.setValueMap(dbs);
        dbType.setDefaultToFirstOption(true);
        dbType.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                // force the user to re-test the connection
                mainInstallButton.disable();
                testConnectionButton.setIcon(null);
                dbExistingSchemaOption.setVisible(false);

                String newDBType = (String) event.getValue();
                String newURL = "";
                String dialect = "";
                String quartzDriverDelegateClass = "org.quartz.impl.jdbcjobstore.StdJDBCDelegate";
                String quartzSelectWithLockSQL = "SELECT * FROM {0}LOCKS ROWLOCK WHERE LOCK_NAME = ? FOR UPDATE";
                String quartzLockHandlerClass = "org.quartz.impl.jdbcjobstore.StdRowLockSemaphore";

                if ("PostgreSQL".equalsIgnoreCase(newDBType)) {
                    newURL = "jdbc:postgresql://127.0.0.1:5432/rhq";
                    dialect = "org.hibernate.dialect.PostgreSQLDialect";
                    quartzDriverDelegateClass = "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate";
                } else if ("Oracle".equalsIgnoreCase(newDBType)) {
                    newURL = "jdbc:oracle:thin:@127.0.0.1:1521:rhq";
                    dialect = "org.hibernate.dialect.Oracle10gDialect";
                    quartzDriverDelegateClass = "org.quartz.impl.jdbcjobstore.oracle.OracleDelegate";
                }
                dbConnectionUrl.setValue(newURL);

                serverProperties.put(ServerProperties.PROP_DATABASE_CONNECTION_URL, newURL);
                serverProperties.put(ServerProperties.PROP_DATABASE_HIBERNATE_DIALECT, dialect);
                serverProperties.put(ServerProperties.PROP_QUARTZ_DRIVER_DELEGATE_CLASS, quartzDriverDelegateClass);
                serverProperties.put(ServerProperties.PROP_QUARTZ_SELECT_WITH_LOCK_SQL, quartzSelectWithLockSQL);
                serverProperties.put(ServerProperties.PROP_QUARTZ_LOCK_HANDLER_CLASS, quartzLockHandlerClass);
                updateServerProperty(ServerProperties.PROP_DATABASE_TYPE, newDBType); // this refreshes the advanced view, too
            }
        });

        // use this to move the button over to the second column - it looks better this way
        SpacerItem buttonSpacer = new SpacerItem();
        buttonSpacer.setEndRow(false);
        testConnectionButton.setStartRow(false);

        databaseForm.setFields(dbType, dbConnectionUrl, dbUsername, dbPassword, buttonSpacer, testConnectionButton,
            dbExistingSchemaOption);

        ////////////////////////////////////////////////////////
        // The Server Settings form

        final DynamicForm serverSettingsForm = new DynamicForm();
        serverSettingsForm.setPadding(5);
        serverSettingsForm.setCellPadding(5);
        serverSettingsForm.setAutoWidth();
        serverSettingsForm.setIsGroup(true);
        serverSettingsForm.setWrapItemTitles(false);
        serverSettingsForm.setGroupTitle(MSG.tab_simpleView_serverSettings());

        serverSettingServerName = new TextItem(ServerProperties.PROP_HIGH_AVAILABILITY_NAME,
            PROPS_MSG.rhq_server_high_availability_name());
        serverSettingServerName.setWidth(fieldWidth);
        serverSettingServerName.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                updateServerProperty(ServerProperties.PROP_HIGH_AVAILABILITY_NAME, String.valueOf(event.getValue()));
            }
        });

        serverSettingPublicAddress = new TextItem("serverPublicAddress",
            MSG.tab_simpleView_serverSettings_publicAddress());
        serverSettingPublicAddress.setWidth(fieldWidth);

        serverSettingWebHttpPort = new SpinnerItem(ServerProperties.PROP_WEB_HTTP_PORT,
            PROPS_MSG.rhq_server_startup_web_http_port());
        serverSettingWebHttpPort.setWidth(fieldWidth);
        serverSettingWebHttpPort.setMin(1);
        serverSettingWebHttpPort.setMax(65535);
        serverSettingWebHttpPort.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                updateServerProperty(ServerProperties.PROP_WEB_HTTP_PORT, String.valueOf(event.getValue()));
            }
        });

        serverSettingWebSecureHttpPort = new SpinnerItem(ServerProperties.PROP_WEB_HTTPS_PORT,
            PROPS_MSG.rhq_server_startup_web_http_port());
        serverSettingWebSecureHttpPort.setWidth(fieldWidth);
        serverSettingWebSecureHttpPort.setMin(1);
        serverSettingWebSecureHttpPort.setMax(65535);
        serverSettingWebSecureHttpPort.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                updateServerProperty(ServerProperties.PROP_WEB_HTTPS_PORT, String.valueOf(event.getValue()));
            }
        });

        registeredServersSelection = new SelectItem("registeredServersSelectItem",
            MSG.tab_simpleView_serverSettings_registeredServers());
        final LinkedHashMap<String, String> registeredServers = new LinkedHashMap<String, String>();
        registeredServers.put("*new*", MSG.tab_simpleView_serverSettings_registeredServers_newServer());
        registeredServers.put("blah", "blah");
        registeredServersSelection.setValueMap(registeredServers);
        registeredServersSelection.setDefaultToFirstOption(true);
        registeredServersSelection.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                String selectedServerName = String.valueOf(event.getValue());
                boolean newServer = "*new*".equals(selectedServerName);
                if (newServer) {
                    serverSettingServerName.setValue("");
                    serverSettingPublicAddress.setValue("");
                } else {
                    serverSettingServerName.setValue(selectedServerName);
                }
            }
        });

        serverSettingEmailSMTPHostname = new TextItem(ServerProperties.PROP_EMAIL_SMTP_HOST,
            PROPS_MSG.rhq_server_email_smtp_host());
        serverSettingEmailSMTPHostname.setWidth(fieldWidth);
        serverSettingEmailSMTPHostname.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                updateServerProperty(ServerProperties.PROP_EMAIL_SMTP_HOST, String.valueOf(event.getValue()));
            }
        });

        serverSettingEmailFromAddress = new TextItem(ServerProperties.PROP_EMAIL_FROM_ADDRESS,
            PROPS_MSG.rhq_server_email_from_address());
        serverSettingEmailFromAddress.setWidth(fieldWidth);
        serverSettingEmailFromAddress.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                updateServerProperty(ServerProperties.PROP_EMAIL_FROM_ADDRESS, String.valueOf(event.getValue()));
            }
        });

        serverSettingsForm.setItems(registeredServersSelection, serverSettingServerName, serverSettingPublicAddress,
            serverSettingWebHttpPort, serverSettingWebSecureHttpPort, serverSettingEmailSMTPHostname,
            serverSettingEmailFromAddress);

        ////////////////////////////////////////////////////////
        // The layout holding the forms in the simple view tab

        VLayout simpleForm = new VLayout();
        simpleForm.setLayoutMargin(5);
        simpleForm.setMembersMargin(5);
        simpleForm.setWidth100();
        simpleForm.setHeight100();
        simpleForm.setDefaultLayoutAlign(Alignment.CENTER);
        simpleForm.addMember(databaseForm);
        simpleForm.addMember(serverSettingsForm);

        return simpleForm;
    }
}
