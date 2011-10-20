/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.admin;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.core.domain.plugin.ServerPluginControlDefinition;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeEvent;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeListener;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.PluginGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHStack;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableSectionStack;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVStack;

/**
 * Shows details of a server plugin.
 * 
 * @author John Mazzitelli
 */
public class ServerPluginDetailView extends LocatableVLayout {

    private final PluginGWTServiceAsync pluginManager = GWTServiceLookup.getPluginService();
    private final int pluginId;

    private int DETAILS_POSITION = 0;
    private int HELP_POSITION = 1;
    private int CONTROLS_POSITION = 2;
    private int PLUGINCONFIG_POSITION = 3;
    private int SCHEDULEDJOBS_POSITION = 4;

    public ServerPluginDetailView(String locatorId, int pluginId) {
        super(locatorId);
        this.pluginId = pluginId;
        setHeight100();
        setWidth100();
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        final LocatableSectionStack sectionStack;

        sectionStack = new LocatableSectionStack(extendLocatorId("stack"));
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        sectionStack.setWidth100();
        sectionStack.setHeight100();
        sectionStack.setMargin(5);
        sectionStack.setOverflow(Overflow.AUTO);

        pluginManager.getServerPlugin(this.pluginId, true, new AsyncCallback<ServerPlugin>() {
            public void onSuccess(ServerPlugin plugin) {
                prepareDetailsSection(sectionStack, plugin);
                prepareHelpSection(sectionStack, plugin);
                prepareControlsSection(sectionStack, plugin);
                preparePluginConfigurationSection(sectionStack, plugin);
                prepareScheduledJobsSection(sectionStack, plugin);
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_admin_plugins_loadFailure(), caught);
            }
        });

        addMember(sectionStack);
    }

    private void prepareControlsSection(final SectionStack stack, final ServerPlugin plugin) {
        PluginKey pluginKey = PluginKey.createServerPluginKey(plugin.getType(), plugin.getName());

        pluginManager.getServerPluginControlDefinitions(pluginKey,
            new AsyncCallback<ArrayList<ServerPluginControlDefinition>>() {
                public void onSuccess(ArrayList<ServerPluginControlDefinition> result) {
                    if (result != null && !result.isEmpty()) {
                        SectionStackSection section = new SectionStackSection(MSG.view_admin_plugins_serverControls());
                        section.setExpanded(false);
                        section.addItem(new ServerPluginControlView(extendLocatorId("controlView"), plugin, result));
                        stack.addSection(section, CONTROLS_POSITION);
                    }
                }

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_admin_plugins_loadFailure(), caught);
                }
            });
    }

    private void preparePluginConfigurationSection(final SectionStack stack, final ServerPlugin plugin) {
        final PluginKey pluginKey = PluginKey.createServerPluginKey(plugin.getType(), plugin.getName());

        pluginManager.getServerPluginConfigurationDefinition(pluginKey, new AsyncCallback<ConfigurationDefinition>() {
            public void onSuccess(ConfigurationDefinition def) {
                if (def != null) {
                    LocatableVStack layout = new LocatableVStack(extendLocatorId("pcEditorLayout"));
                    layout.setMargin(5);
                    layout.setMembersMargin(5);
                    layout.setAutoHeight();

                    LocatableHStack buttons = new LocatableHStack(extendLocatorId("pcButtonsLayout"));
                    buttons.setMembersMargin(5);
                    buttons.setAutoHeight();
                    layout.addMember(buttons);

                    final IButton saveButtonPC = new LocatableIButton(extendLocatorId("pcSave"), MSG
                        .common_button_save());
                    buttons.addMember(saveButtonPC);

                    final IButton resetButtonPC = new LocatableIButton(extendLocatorId("pcRest"), MSG
                        .common_button_reset());
                    buttons.addMember(resetButtonPC);

                    Configuration config = plugin.getPluginConfiguration();
                    final ConfigurationEditor editorPC = new ConfigurationEditor(extendLocatorId("pcEdit"), def, config);
                    editorPC.addPropertyValueChangeListener(new PropertyValueChangeListener() {
                        public void propertyValueChanged(PropertyValueChangeEvent event) {
                            if (event.isInvalidPropertySetChanged()) {
                                Map<String, String> invalidPropertyNames = event.getInvalidPropertyNames();
                                if (invalidPropertyNames.isEmpty()) {
                                    saveButtonPC.enable();
                                } else {
                                    saveButtonPC.disable();
                                }
                            }
                        }
                    });
                    layout.addMember(editorPC);

                    resetButtonPC.addClickHandler(new ClickHandler() {
                        public void onClick(ClickEvent event) {
                            editorPC.reset();
                        }
                    });

                    saveButtonPC.addClickHandler(new ClickHandler() {
                        public void onClick(ClickEvent event) {
                            if (!editorPC.validate()) {
                                Message msg = new Message(MSG.view_admin_plugins_serverConfig_badSettings(),
                                    Severity.Warning, EnumSet.of(Message.Option.Transient));
                                CoreGUI.getMessageCenter().notify(msg);
                                return;
                            }
                            pluginManager.updateServerPluginConfiguration(pluginKey, editorPC.getConfiguration(),
                                new AsyncCallback<Void>() {
                                    public void onSuccess(Void result) {
                                        Message m = new Message(MSG.view_admin_plugins_serverConfig_settingsSaved());
                                        CoreGUI.getMessageCenter().notify(m);
                                    }

                                    public void onFailure(Throwable caught) {
                                        CoreGUI.getErrorHandler().handleError(
                                            MSG.view_admin_plugins_serverConfig_saveFailed(), caught);
                                    }
                                });
                        }
                    });

                    SectionStackSection section = new SectionStackSection(MSG.view_admin_plugins_serverConfig());
                    section.setExpanded(false);
                    section.setItems(layout);
                    stack.addSection(section, PLUGINCONFIG_POSITION);
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_admin_plugins_loadFailure(), caught);
            }
        });
        return;
    }

    private void prepareScheduledJobsSection(final SectionStack stack, final ServerPlugin plugin) {
        final PluginKey pluginKey = PluginKey.createServerPluginKey(plugin.getType(), plugin.getName());

        pluginManager.getServerPluginScheduledJobsDefinition(pluginKey, new AsyncCallback<ConfigurationDefinition>() {
            public void onSuccess(ConfigurationDefinition def) {
                if (def != null) {
                    LocatableVLayout layout = new LocatableVLayout(extendLocatorId("sjEditorLayout"));
                    layout.setMargin(5);
                    layout.setMembersMargin(5);
                    layout.setAutoHeight();

                    LocatableHStack buttons = new LocatableHStack(extendLocatorId("sjButtonsLayout"));
                    buttons.setMembersMargin(5);
                    buttons.setAutoHeight();
                    layout.addMember(buttons);

                    final IButton saveButtonSJ = new LocatableIButton(extendLocatorId("sjSave"), MSG
                        .common_button_save());
                    buttons.addMember(saveButtonSJ);

                    final IButton resetButtonSJ = new LocatableIButton(extendLocatorId("sjRest"), MSG
                        .common_button_reset());
                    buttons.addMember(resetButtonSJ);

                    Configuration config = plugin.getScheduledJobsConfiguration();
                    final ConfigurationEditor editorSJ = new ConfigurationEditor(extendLocatorId("sjEdit"), def, config);
                    editorSJ.addPropertyValueChangeListener(new PropertyValueChangeListener() {
                        public void propertyValueChanged(PropertyValueChangeEvent event) {
                            if (event.isInvalidPropertySetChanged()) {
                                Map<String, String> invalidPropertyNames = event.getInvalidPropertyNames();
                                if (invalidPropertyNames.isEmpty()) {
                                    saveButtonSJ.enable();
                                } else {
                                    saveButtonSJ.disable();
                                }
                            }
                        }
                    });
                    layout.addMember(editorSJ);

                    resetButtonSJ.addClickHandler(new ClickHandler() {
                        public void onClick(ClickEvent event) {
                            editorSJ.reset();
                        }
                    });

                    saveButtonSJ.addClickHandler(new ClickHandler() {
                        public void onClick(ClickEvent event) {
                            if (!editorSJ.validate()) {
                                Message msg = new Message(MSG.view_admin_plugins_serverConfig_badSettings(),
                                    Severity.Warning, EnumSet.of(Message.Option.Transient));
                                CoreGUI.getMessageCenter().notify(msg);
                                return;
                            }
                            pluginManager.updateServerPluginScheduledJobs(pluginKey, editorSJ.getConfiguration(),
                                new AsyncCallback<Void>() {
                                    public void onSuccess(Void result) {
                                        Message m = new Message(MSG.view_admin_plugins_serverConfig_settingsSaved());
                                        CoreGUI.getMessageCenter().notify(m);
                                    }

                                    public void onFailure(Throwable caught) {
                                        CoreGUI.getErrorHandler().handleError(
                                            MSG.view_admin_plugins_serverConfig_saveFailed(), caught);
                                    }
                                });
                        }
                    });

                    SectionStackSection section = new SectionStackSection(MSG.view_admin_plugins_serverScheduleJobs());
                    section.setExpanded(false);
                    section.setItems(layout);
                    stack.addSection(section, SCHEDULEDJOBS_POSITION);
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_admin_plugins_loadFailure(), caught);
            }
        });
        return;
    }

    private void prepareHelpSection(SectionStack stack, ServerPlugin plugin) {
        if (plugin.getHelp() != null && plugin.getHelp().length() > 0) {
            SectionStackSection section = new SectionStackSection(MSG.common_title_help());
            section.setExpanded(true);
            Label help = new Label(plugin.getHelp());
            section.setItems(help);
            stack.addSection(section, HELP_POSITION);
        }
        return;
    }

    private void prepareDetailsSection(SectionStack stack, ServerPlugin plugin) {
        LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("detailsForm"));
        form.setMargin(10);
        form.setWidth100();
        form.setWrapItemTitles(false);
        form.setNumCols(4);

        StaticTextItem nameItem = new StaticTextItem("name", MSG.common_title_name());
        nameItem.setValue(plugin.getName());

        StaticTextItem displayNameItem = new StaticTextItem("displayName", MSG.common_title_display_name());
        displayNameItem.setValue(plugin.getDisplayName());

        StaticTextItem versionItem = new StaticTextItem("version", MSG.common_title_version());
        versionItem.setValue(plugin.getVersion());

        StaticTextItem md5Item = new StaticTextItem("MD5", "MD5");
        md5Item.setValue(plugin.getMD5());

        StaticTextItem pathItem = new StaticTextItem("path", MSG.common_title_path());
        pathItem.setValue(plugin.getPath());

        StaticTextItem ampsItem = new StaticTextItem("ampsVersion", "AMPS " + MSG.common_title_version());
        ampsItem.setValue(plugin.getAmpsVersion());

        StaticTextItem descItem = new StaticTextItem("desc", MSG.common_title_description());
        descItem.setValue(plugin.getDescription());

        StaticTextItem mtimeItem = new StaticTextItem("mtime", MSG.common_title_lastUpdated());
        mtimeItem.setValue(TimestampCellFormatter.format(Long.valueOf(plugin.getMtime()),
            TimestampCellFormatter.DATE_TIME_FORMAT_MEDIUM));

        StaticTextItem kindItem = new StaticTextItem("kind", MSG.common_title_kind());
        switch (plugin.getDeployment()) {
        case AGENT:
            kindItem.setValue(MSG.view_admin_plugins_agent());
            break;
        case SERVER:
            kindItem.setValue(MSG.view_admin_plugins_server());
            break;
        }

        CanvasItem enabledItem = new CanvasItem("enabled", MSG.common_title_enabled());
        Img img = new Img(ImageManager.getAvailabilityIcon(plugin.isEnabled()), 16, 16);
        enabledItem.setCanvas(img);

        StaticTextItem typeItem = new StaticTextItem("type", MSG.common_title_type());
        typeItem.setValue(plugin.getType());

        form.setItems(displayNameItem, nameItem, versionItem, ampsItem, md5Item, kindItem, descItem, pathItem,
            mtimeItem, enabledItem, typeItem);

        SectionStackSection section = new SectionStackSection(MSG.common_title_details());
        section.setExpanded(true);
        section.setItems(form);
        stack.addSection(section, DETAILS_POSITION);

        return;
    }
}
