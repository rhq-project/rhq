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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Side;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.SpinnerItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.IconClickEvent;
import com.smartgwt.client.widgets.form.fields.events.IconClickHandler;
import com.smartgwt.client.widgets.form.validator.IntegerRangeValidator;
import com.smartgwt.client.widgets.form.validator.RequiredIfFunction;
import com.smartgwt.client.widgets.form.validator.RequiredIfValidator;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.events.EditCompleteEvent;
import com.smartgwt.client.widgets.grid.events.EditCompleteHandler;
import com.smartgwt.client.widgets.grid.events.EditorExitEvent;
import com.smartgwt.client.widgets.grid.events.EditorExitHandler;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.enterprise.gui.installer.client.gwt.InstallerGWTServiceAsync;
import org.rhq.enterprise.gui.installer.client.shared.ServerDetails;
import org.rhq.enterprise.gui.installer.client.shared.ServerProperties;

/**
 * The GWT {@link EntryPoint entry point} to the RHQ Installer GUI.
 *
 * @author John Mazzitelli
 */
public class Installer implements EntryPoint {

    // This must come first to ensure proper I18N class loading for dev mode
    private static final Messages MSG = GWT.create(Messages.class);
    private static final ServerPropertiesMessages PROPS_MSG = GWT.create(ServerPropertiesMessages.class);

    private static final String INSTALLED_APP_URL = "/"; // if already installed, this is the URL context where it can be accessed
    private static final String NEW_SERVER_TO_REGISTER = "*new*";

    private final InstallerGWTServiceAsync installerService = InstallerGWTServiceAsync.Util.getInstance();
    private final ServerPropertyRecordList serverProperties = new ServerPropertyRecordList();
    private final HashMap<String, String> originalProperties = new HashMap<String, String>();
    private final LinkedHashMap<String, String> registeredServerNames = new LinkedHashMap<String, String>();

    private VLayout mainCanvas;

    private ListGrid advancedPropertyItemGrid;
    private Button mainInstallButton;

    private TextItem dbConnectionUrl;
    private TextItem dbUsername;
    private PasswordItem dbPassword;
    private SelectItem dbExistingSchemaOption;
    private ButtonItem testConnectionButton;
    private SelectItem dbType;

    private DynamicForm serverSettingsForm;
    private TextItem serverSettingServerName;
    private TextItem serverSettingPublicAddress;
    private SpinnerItem serverSettingWebHttpPort;
    private SpinnerItem serverSettingWebSecureHttpPort;
    private SelectItem registeredServersSelection;
    private TextItem serverSettingEmailSMTPHostname;
    private TextItem serverSettingEmailFromAddress;

    public void onModuleLoad() {
        mainCanvas = new VLayout();
        mainCanvas.setWidth100();
        mainCanvas.setHeight100();
        mainCanvas.setLayoutMargin(10);
        mainCanvas.setMembersMargin(5);
        mainCanvas.setDefaultLayoutAlign(Alignment.CENTER);
        mainCanvas.draw();

        installerService.getInstallationResults(new AsyncCallback<String>() {
            public void onSuccess(String result) {
                if (result == null) {
                    prepareInstallerView(); // we need the user to complete the installation
                } else if (result.length() == 0) {
                    showInstalledDialog(); // nothing needs to be done, we are already installed
                } else {
                    showInstallationErrorDialog(result);
                }
            }

            public void onFailure(Throwable caught) {
                SC.warn(MSG.message_install_doNotKnowIfInstalled(caught.toString()), new BooleanCallback() {
                    public void execute(Boolean value) {
                        prepareInstallerView();
                    }
                });
            }
        });

        // Remove loading image in case we don't completely cover it
        Element loadingPanel = DOM.getElementById("Loading-Panel");
        loadingPanel.removeFromParent();

        return;
    }

    private void showInstallationErrorDialog(String error) {
        SC.warn(MSG.message_install_failed() + "<br/>" + error);
    }

    private void showInstalledDialog() {
        String loginMsg = MSG.message_install_loginHere(INSTALLED_APP_URL);
        SC.say(loginMsg, new BooleanCallback() {
            public void execute(Boolean value) {
                Window.open(INSTALLED_APP_URL, "_self", null);
            }
        });
    }

    private void prepareInstallerView() {
        Canvas header = createHeader();
        mainInstallButton = createMainInstallButton();
        Canvas tabSet = createTabSet();

        mainCanvas.addMember(header);
        mainCanvas.addMember(mainInstallButton);
        mainCanvas.addMember(tabSet);
        mainCanvas.draw();

        // get the server properties from the server
        loadServerProperties();
    }

    private void updateServerProperty(String name, Object value) {
        serverProperties.putServerProperty(name, value == null ? "" : value.toString());
        refreshAdvancedView();
    }

    private void loadServerProperties() {
        // load the initial server properties
        installerService.getServerProperties(new AsyncCallback<HashMap<String, String>>() {
            public void onSuccess(HashMap<String, String> result) {
                if (result.size() == 0) {
                    SC.say("Initial server properties are missing.");
                }
                serverProperties.replaceServerProperties(result);

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
        Map<String, String> props = serverProperties.getMap();

        // DB SETTINGS
        dbType.setValue(props.get(ServerProperties.PROP_DATABASE_TYPE));
        dbConnectionUrl.setValue(props.get(ServerProperties.PROP_DATABASE_CONNECTION_URL));
        dbUsername.setValue(props.get(ServerProperties.PROP_DATABASE_USERNAME));
        // do not prefill the database password - force the user to know it and type it in for security purposes

        // SERVER SETTINGS
        serverSettingServerName.setValue(props.get(ServerProperties.PROP_HIGH_AVAILABILITY_NAME));
        serverSettingWebHttpPort.setValue(props.get(ServerProperties.PROP_WEB_HTTP_PORT));
        serverSettingWebSecureHttpPort.setValue(props.get(ServerProperties.PROP_WEB_HTTPS_PORT));
        serverSettingEmailSMTPHostname.setValue(props.get(ServerProperties.PROP_EMAIL_SMTP_HOST));
        serverSettingEmailFromAddress.setValue(props.get(ServerProperties.PROP_EMAIL_FROM_ADDRESS));

        forceAnotherTestConnection();
    }

    private void refreshAdvancedView() {
        advancedPropertyItemGrid.markForRedraw();
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
                if (!serverSettingsForm.validate()) {
                    SC.say(MSG.message_formDidNotValidate());
                    return;
                }
                String highAvailabilityName = serverSettingServerName.getValueAsString();
                String publicEndpoint = serverSettingPublicAddress.getValueAsString();
                int httpPort = Integer.parseInt(serverSettingWebHttpPort.getValueAsString());
                int httpsPort = Integer.parseInt(serverSettingWebSecureHttpPort.getValueAsString());
                String existingSchemaOption = dbExistingSchemaOption.getValueAsString();

                SC.say(MSG.message_install_started());
                mainCanvas.disable();

                HashMap<String, String> propMap = serverProperties.getMap();
                ServerDetails svrDetails = new ServerDetails(highAvailabilityName, publicEndpoint, httpPort, httpsPort);
                installerService.install(propMap, svrDetails, existingSchemaOption, new AsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        mainCanvas.enable();
                        showInstalledDialog();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        mainCanvas.enable();
                        showInstallationErrorDialog(caught.toString());
                    }
                });
            }
        });

        return installButton;
    }

    private TabSet createTabSet() {
        final TabSet topTabSet = new TabSet();
        topTabSet.setTabBarPosition(Side.TOP);
        topTabSet.setTabBarAlign(Side.LEFT);
        topTabSet.setWidth("90%");

        final Tab welcomeTab = new Tab(MSG.tab_welcome());
        Label welcomeLabel = new Label(MSG.tab_welcome_content());
        welcomeTab.setPane(welcomeLabel);

        final Tab simpleViewTab = new Tab(MSG.tab_simpleView());
        Canvas simpleForm = createSimpleForm();
        simpleViewTab.setPane(simpleForm);

        final Tab advancedViewTab = new Tab(MSG.tab_advancedView());
        Canvas advancedView = createAdvancedView();
        advancedViewTab.setPane(advancedView);

        topTabSet.addTab(welcomeTab);
        topTabSet.addTab(simpleViewTab);
        topTabSet.addTab(advancedViewTab);

        return topTabSet;
    }

    private Canvas createAdvancedView() {
        VLayout layout = new VLayout();

        ToolStrip strip = new ToolStrip();
        strip.setWidth100();

        IButton resetButton = new IButton(MSG.button_reset());
        resetButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                serverProperties.replaceServerProperties(originalProperties);
                refreshSimpleView();
                refreshAdvancedView();
                forceAnotherTestConnection();
            }
        });
        strip.addMember(resetButton);
        layout.addMember(strip);

        advancedPropertyItemGrid = new ListGrid();
        advancedPropertyItemGrid.setWidth100();
        advancedPropertyItemGrid.setHeight100();
        advancedPropertyItemGrid.setData(serverProperties);

        ListGridField nameField = new ListGridField(ServerPropertyRecordList.PROPERTY_NAME, MSG.property_name_label());
        nameField.setCanEdit(false);

        ListGridField valueField = new ListGridField(ServerPropertyRecordList.PROPERTY_VALUE,
            MSG.property_value_label());
        valueField.setCanEdit(true);

        advancedPropertyItemGrid.setFields(nameField, valueField);
        advancedPropertyItemGrid.setSortField(ServerPropertyRecordList.PROPERTY_NAME);
        advancedPropertyItemGrid.addEditorExitHandler(new EditorExitHandler() {
            public void onEditorExit(EditorExitEvent event) {
                String changedProperty = event.getRecord().getAttribute(ServerPropertyRecordList.PROPERTY_NAME);
                if (ServerProperties.BOOLEAN_PROPERTIES.contains(changedProperty)) {
                    String newValue = event.getNewValue().toString();
                    if (!(newValue.equals("true") || newValue.equals("false"))) {
                        event.cancel();
                        advancedPropertyItemGrid.setFieldError(event.getRowNum(),
                            ServerPropertyRecordList.PROPERTY_VALUE, MSG.message_notValidBoolean());
                    }
                } else if (ServerProperties.INTEGER_PROPERTIES.contains(changedProperty)) {
                    String newValue = event.getNewValue().toString();
                    try {
                        Integer.parseInt(newValue);
                    } catch (NumberFormatException e) {
                        event.cancel();
                        advancedPropertyItemGrid.setFieldError(event.getRowNum(),
                            ServerPropertyRecordList.PROPERTY_VALUE, MSG.message_notValidInteger());
                    }
                }
            }
        });
        advancedPropertyItemGrid.addEditCompleteHandler(new EditCompleteHandler() {
            public void onEditComplete(EditCompleteEvent event) {
                String newValue = event.getNewValues().values().iterator().next().toString();
                String changedProperty = event.getOldRecord().getAttribute(ServerPropertyRecordList.PROPERTY_NAME);
                serverProperties.getMap().put(changedProperty, newValue); // so this is reflected in the internal map
                refreshSimpleView();
            }
        });

        layout.addMember(advancedPropertyItemGrid);

        return layout;
    }

    private Canvas createSimpleForm() {

        final int fieldWidth = 300;

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
                forceAnotherTestConnection();
            }
        });

        dbUsername = new TextItem(ServerProperties.PROP_DATABASE_USERNAME,
            PROPS_MSG.rhq_server_database_user_name());
        dbUsername.setWidth(fieldWidth);
        dbUsername.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                updateServerProperty(ServerProperties.PROP_DATABASE_USERNAME, String.valueOf(event.getValue()));
                forceAnotherTestConnection();
            }
        });

        dbPassword = new PasswordItem(ServerProperties.PROP_DATABASE_PASSWORD,
            PROPS_MSG.rhq_server_database_password());
        dbPassword.setWidth(fieldWidth);
        dbPassword.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                updateServerProperty(ServerProperties.PROP_DATABASE_PASSWORD, String.valueOf(event.getValue()));
                forceAnotherTestConnection();
            }
        });

        dbExistingSchemaOption = new SelectItem("existingSchemaOption", MSG.schema_update_question());
        // keys must match ServerInstallUtil.ExistingSchemaOption enum names
        final LinkedHashMap<String, String> schemaOpt = new LinkedHashMap<String, String>();
        schemaOpt.put("KEEP", MSG.schema_update_keep());
        schemaOpt.put("OVERWRITE", MSG.schema_update_overwrite());
        schemaOpt.put("SKIP", MSG.schema_update_skip());
        dbExistingSchemaOption.setValueMap(schemaOpt);
        dbExistingSchemaOption.setDefaultToFirstOption(true);
        dbExistingSchemaOption.setVisible(false);
        dbExistingSchemaOption.setWidth(fieldWidth);
        dbExistingSchemaOption.setWrapTitle(true);
        dbExistingSchemaOption.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                if (registeredServersSelection != null) {
                    String selected = event.getValue().toString();
                    if ("OVERWRITE".equals(selected)) {
                        registeredServersSelection.setValue((String) null);
                        registeredServersSelection.disable();
                    } else {
                        registeredServersSelection.enable();
                    }
                }
            }
        });

        testConnectionButton = new ButtonItem("testConnectionButton", MSG.button_testConnection());
        testConnectionButton.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
                final Conn conn = new Conn();
                installerService.testConnection(conn.url(), conn.username(), conn.password(),
                    new AsyncCallback<String>() {
                        public void onSuccess(String result) {
                            if (result != null) {
                                forceAnotherTestConnection();
                                testConnectionButton.setIcon("[SKIN]/actions/exclamation.png");
                                SC.say("Could not connect to the database: " + result);
                            } else {
                                connectedToDatabase();
                            }
                        }

                        public void onFailure(Throwable caught) {
                            forceAnotherTestConnection();
                            testConnectionButton.setIcon("[SKIN]/actions/exclamation.png");
                            SC.say("Failed to test connection: " + caught.toString());
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
                forceAnotherTestConnection();

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

                updateServerProperty(ServerProperties.PROP_DATABASE_CONNECTION_URL, newURL);
                updateServerProperty(ServerProperties.PROP_DATABASE_HIBERNATE_DIALECT, dialect);
                updateServerProperty(ServerProperties.PROP_QUARTZ_DRIVER_DELEGATE_CLASS, quartzDriverDelegateClass);
                updateServerProperty(ServerProperties.PROP_QUARTZ_SELECT_WITH_LOCK_SQL, quartzSelectWithLockSQL);
                updateServerProperty(ServerProperties.PROP_QUARTZ_LOCK_HANDLER_CLASS, quartzLockHandlerClass);
                updateServerProperty(ServerProperties.PROP_DATABASE_TYPE, newDBType);
            }
        });

        // use this to move the button over to the second column - it looks better this way
        SpacerItem buttonSpacer = new SpacerItem();
        buttonSpacer.setEndRow(false);
        testConnectionButton.setStartRow(false);

        // add contextual help for users
        addContextualHelp(dbType, MSG.help_dbType());
        addContextualHelp(dbConnectionUrl, MSG.help_dbConnectionUrl());
        addContextualHelp(dbUsername, MSG.help_dbUsername());
        addContextualHelp(dbPassword, MSG.help_dbPassword());
        addContextualHelp(testConnectionButton, MSG.help_testConnectionButton());
        addContextualHelp(dbExistingSchemaOption, MSG.help_dbExistingSchemaOption());

        databaseForm.setFields(dbType, dbConnectionUrl, dbUsername, dbPassword, buttonSpacer, testConnectionButton,
            dbExistingSchemaOption);

        ////////////////////////////////////////////////////////
        // The Server Settings form

        RequiredIfValidator notEmptyValidator = new RequiredIfValidator();
        notEmptyValidator.setExpression(new RequiredIfFunction() {
            public boolean execute(FormItem formItem, Object value) {
                return true;
            }
        });

        IntegerRangeValidator portValidator = new IntegerRangeValidator();
        portValidator.setMin(1);
        portValidator.setMax(65535);

        serverSettingsForm = new DynamicForm();
        serverSettingsForm.setPadding(5);
        serverSettingsForm.setCellPadding(5);
        serverSettingsForm.setAutoWidth();
        serverSettingsForm.setIsGroup(true);
        serverSettingsForm.setValidateOnChange(true);
        serverSettingsForm.setWrapItemTitles(false);
        serverSettingsForm.setGroupTitle(MSG.tab_simpleView_serverSettings());

        serverSettingServerName = new TextItem(ServerProperties.PROP_HIGH_AVAILABILITY_NAME,
            PROPS_MSG.rhq_server_high_availability_name());
        serverSettingServerName.setWidth(fieldWidth);
        serverSettingServerName.setValidators(notEmptyValidator);
        serverSettingServerName.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                updateServerProperty(ServerProperties.PROP_HIGH_AVAILABILITY_NAME, String.valueOf(event.getValue()));
                if (registeredServersSelection != null) {
                    registeredServersSelection.setValue((String) null); // flip it back to *new* since the server name changed
                }
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
        serverSettingWebHttpPort.setDefaultValue(ServerDetails.DEFAULT_ENDPOINT_PORT);
        serverSettingWebHttpPort.setValidators(portValidator);
        serverSettingWebHttpPort.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                updateServerProperty(ServerProperties.PROP_WEB_HTTP_PORT, String.valueOf(event.getValue()));
            }
        });

        serverSettingWebSecureHttpPort = new SpinnerItem(ServerProperties.PROP_WEB_HTTPS_PORT,
            PROPS_MSG.rhq_server_startup_web_https_port());
        serverSettingWebSecureHttpPort.setWidth(fieldWidth);
        serverSettingWebSecureHttpPort.setMin(1);
        serverSettingWebSecureHttpPort.setMax(65535);
        serverSettingWebSecureHttpPort.setDefaultValue(ServerDetails.DEFAULT_ENDPOINT_SECURE_PORT);
        serverSettingWebSecureHttpPort.setValidators(portValidator);
        serverSettingWebSecureHttpPort.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                updateServerProperty(ServerProperties.PROP_WEB_HTTPS_PORT, String.valueOf(event.getValue()));
            }
        });

        registeredServersSelection = new SelectItem("registeredServersSelectItem",
            MSG.tab_simpleView_serverSettings_registeredServers());
        registeredServersSelection.setWidth(fieldWidth);
        registeredServerNames.put(NEW_SERVER_TO_REGISTER,
            MSG.tab_simpleView_serverSettings_registeredServers_newServer());
        registeredServersSelection.setValueMap(registeredServerNames);
        registeredServersSelection.setDefaultToFirstOption(true);
        registeredServersSelection.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                final String selectedServerName = String.valueOf(event.getValue());
                final boolean newServer = NEW_SERVER_TO_REGISTER.equals(selectedServerName);
                if (newServer) {
                    serverSettingServerName.setValue("");
                    serverSettingPublicAddress.setValue("");
                    updateServerProperty(ServerProperties.PROP_HIGH_AVAILABILITY_NAME, "");
                    serverSettingsForm.validate();
                } else {
                    final Conn conn = new Conn();
                    installerService.getServerDetails(conn.url(), conn.username(), conn.password(), selectedServerName,
                        new AsyncCallback<ServerDetails>() {
                            public void onSuccess(ServerDetails details) {
                                serverSettingServerName.setValue(details.getName());
                                serverSettingPublicAddress.setValue(details.getEndpointAddress());
                                serverSettingWebHttpPort.setValue(details.getEndpointPortString());
                                serverSettingWebSecureHttpPort.setValue(details.getEndpointSecurePortString());
                                updateServerProperty(ServerProperties.PROP_HIGH_AVAILABILITY_NAME, details.getName());
                                updateServerProperty(ServerProperties.PROP_WEB_HTTP_PORT,
                                    details.getEndpointPortString());
                                updateServerProperty(ServerProperties.PROP_WEB_HTTPS_PORT,
                                    details.getEndpointSecurePortString());
                                serverSettingsForm.validate();
                            }

                            public void onFailure(Throwable caught) {
                                SC.say("Failed to get details on selected server [" + selectedServerName + "]");
                            }
                        });
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

        // add contextual help for users
        addContextualHelp(registeredServersSelection, MSG.help_registeredServers());
        addContextualHelp(serverSettingServerName, MSG.help_ssServerName());
        addContextualHelp(serverSettingPublicAddress, MSG.help_ssPublicAddress());
        addContextualHelp(serverSettingWebHttpPort, MSG.help_ssHttpPort());
        addContextualHelp(serverSettingWebSecureHttpPort, MSG.help_ssHttpsPort());
        addContextualHelp(serverSettingEmailSMTPHostname, MSG.help_ssEmailHostname());
        addContextualHelp(serverSettingEmailFromAddress, MSG.help_ssEmailFromAddress());

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

    private void connectedToDatabase() {
        final Conn conn = new Conn();
        testConnectionButton.setIcon("[SKIN]/actions/ok.png");
        installerService.isDatabaseSchemaExist(conn.url(), conn.username(), conn.password(),
            new AsyncCallback<Boolean>() {
                public void onSuccess(Boolean schemaExists) {
                    if (schemaExists) {
                        dbExistingSchemaOption.show();
                        registeredServersSelection.enable();

                        installerService.getServerNames(conn.url(), conn.username(), conn.password(),
                            new AsyncCallback<ArrayList<String>>() {
                                public void onSuccess(ArrayList<String> servers) {
                                    registeredServerNames.clear();
                                    registeredServerNames.put(NEW_SERVER_TO_REGISTER,
                                        MSG.tab_simpleView_serverSettings_registeredServers_newServer());
                                    for (String server : servers) {
                                        registeredServerNames.put(server, server);
                                    }
                                    registeredServersSelection.setValueMap(registeredServerNames);
                                }

                                public void onFailure(Throwable caught) {
                                    SC.say("Cannot get the registered server names");
                                }
                            });
                    } else {
                        dbExistingSchemaOption.hide();
                        registeredServersSelection.setValue((String) null);
                        registeredServersSelection.disable();
                    }
                    mainInstallButton.enable();
                }

                public void onFailure(Throwable caught) {
                    SC.say("Cannot determine the status of the database schema: " + caught);
                }
            });
    }

    private void addContextualHelp(final FormItem item, final String helpText) {
        final FormItemIcon helpIcon = new FormItemIcon();
        helpIcon.setSrc("[SKIN]/actions/help.png");
        helpIcon.setNeverDisable(true);
        item.setIcons(helpIcon);

        item.addIconClickHandler(new IconClickHandler() {
            public void onIconClick(IconClickEvent event) {
                if (event.getIcon().equals(helpIcon)) {
                    SC.say(helpText);
                }
            }
        });
    }

    /**
     * Call this when the user changed something (like connection URL or password) that renders
     * the old connection test invalid. This will ensure the user is forced to re-test the connection.
     */
    private void forceAnotherTestConnection() {
        mainInstallButton.disable();
        testConnectionButton.setIcon(null);
        dbExistingSchemaOption.hide();

        if (registeredServerNames.size() > 1) {
            registeredServerNames.clear();
            registeredServerNames.put(NEW_SERVER_TO_REGISTER, MSG.tab_simpleView_serverSettings_registeredServers_newServer());
        }
        registeredServersSelection.setValue((String) null);
        registeredServersSelection.setValueMap(registeredServerNames);
    }

    // for convienence, so we can get the conn url, user, pass in one object
    private class Conn {
        public String url() {
            return Installer.this.dbConnectionUrl.getValueAsString();
        }

        public String username() {
            return Installer.this.dbUsername.getValueAsString();
        }

        public String password() {
            return Installer.this.dbPassword.getValueAsString();
        }
    }
}
