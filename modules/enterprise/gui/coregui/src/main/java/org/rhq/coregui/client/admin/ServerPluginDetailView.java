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

package org.rhq.coregui.client.admin;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.core.domain.plugin.ServerPluginControlDefinition;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.coregui.client.components.configuration.PropertyValueChangeEvent;
import org.rhq.coregui.client.components.configuration.PropertyValueChangeListener;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.PluginGWTServiceAsync;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * Shows details of a server plugin.
 * 
 * @author John Mazzitelli
 */
public class ServerPluginDetailView extends EnhancedVLayout {

    private final PluginGWTServiceAsync pluginManager = GWTServiceLookup.getPluginService();
    private final int pluginId;

    private final SectionStack sectionStack;
    private SectionStackSection detailsSection = null;
    private SectionStackSection helpSection = null;
    private SectionStackSection controlsSection = null;
    private SectionStackSection pluginConfigSection = null;
    private SectionStackSection scheduledJobsSection = null;
    private int initSectionCount = 0;

    public ServerPluginDetailView(int pluginId) {
        super();
        this.pluginId = pluginId;
        setHeight100();
        setWidth100();
        setOverflow(Overflow.AUTO);

        sectionStack = new SectionStack();
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        sectionStack.setWidth100();
        sectionStack.setHeight100();
        sectionStack.setMargin(5);
        sectionStack.setOverflow(Overflow.VISIBLE);
    }

    @Override
    protected void onInit() {
        super.onInit();

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
    }

    public boolean isInitialized() {
        return initSectionCount >= 5;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        // wait until we have all of the sections before we show them. We don't use InitializableView because,
        // it seems they are not supported (in the applicable renderView()) at this level.
        new Timer() {
            final long startTime = System.currentTimeMillis();

            public void run() {
                if (isInitialized()) {
                    if (null != detailsSection) {
                        sectionStack.addSection(detailsSection);
                    }
                    if (null != helpSection) {
                        sectionStack.addSection(helpSection);
                    }
                    if (null != controlsSection) {
                        sectionStack.addSection(controlsSection);
                    }
                    if (null != pluginConfigSection) {
                        sectionStack.addSection(pluginConfigSection);
                    }
                    if (null != scheduledJobsSection) {
                        sectionStack.addSection(scheduledJobsSection);
                    }

                    addMember(sectionStack);
                    markForRedraw();

                } else {
                    // don't wait forever, give up after 20s and show what we have
                    long elapsedMillis = System.currentTimeMillis() - startTime;
                    if (elapsedMillis > 20000) {
                        initSectionCount = 5;
                    }
                    schedule(100); // Reschedule the timer.
                }
            }
        }.run(); // fire the timer immediately
    }

    private void prepareControlsSection(final SectionStack stack, final ServerPlugin plugin) {
        PluginKey pluginKey = PluginKey.createServerPluginKey(plugin.getType(), plugin.getName());

        pluginManager.getServerPluginControlDefinitions(pluginKey,
            new AsyncCallback<ArrayList<ServerPluginControlDefinition>>() {
                public void onSuccess(ArrayList<ServerPluginControlDefinition> result) {
                    if (result != null && !result.isEmpty()) {
                        SectionStackSection section = new SectionStackSection(MSG.view_admin_plugins_serverControls());
                        section.setExpanded(false);
                        section.addItem(new ServerPluginControlView(plugin, result));

                        controlsSection = section;
                    }
                    ++initSectionCount;
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
                    EnhancedVLayout layout = new EnhancedVLayout();

                    ToolStrip buttons = new ToolStrip();
                    buttons.setWidth100();
                    buttons.setExtraSpace(10);
                    buttons.setMembersMargin(5);
                    buttons.setLayoutMargin(5);

                    final IButton saveButtonPC = new EnhancedIButton(MSG.common_button_save(), ButtonColor.BLUE);

                    final IButton resetButtonPC = new EnhancedIButton(MSG.common_button_reset(), ButtonColor.RED);

                    Configuration config = plugin.getPluginConfiguration();
                    final ConfigurationEditor editorPC = new ConfigurationEditor(def, config);
                    editorPC.setOverflow(Overflow.AUTO);
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

                    buttons.addMember(saveButtonPC);
                    buttons.addMember(resetButtonPC);
                    layout.addMember(buttons);
                    layout.addMember(editorPC);

                    SectionStackSection section = new SectionStackSection(MSG.view_admin_plugins_serverConfig());
                    section.setExpanded(false);
                    section.setItems(layout);

                    pluginConfigSection = section;
                }

                ++initSectionCount;
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
                    EnhancedVLayout layout = new EnhancedVLayout();

                    ToolStrip buttons = new ToolStrip();
                    buttons.setWidth100();
                    buttons.setExtraSpace(10);
                    buttons.setMembersMargin(5);
                    buttons.setLayoutMargin(5);

                    final IButton saveButtonSJ = new EnhancedIButton(MSG.common_button_save(), ButtonColor.BLUE);
                    buttons.addMember(saveButtonSJ);

                    final IButton resetButtonSJ = new EnhancedIButton(MSG.common_button_reset(), ButtonColor.RED);
                    buttons.addMember(resetButtonSJ);

                    Configuration config = plugin.getScheduledJobsConfiguration();
                    final ConfigurationEditor editorSJ = new ConfigurationEditor(def, config);
                    editorSJ.setOverflow(Overflow.AUTO);
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

                    layout.addMember(buttons);
                    layout.addMember(editorSJ);

                    SectionStackSection section = new SectionStackSection(MSG.view_admin_plugins_serverScheduleJobs());
                    section.setExpanded(false);
                    section.setItems(layout);

                    scheduledJobsSection = section;
                }

                ++initSectionCount;
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

            helpSection = section;
        }

        ++initSectionCount;
        return;
    }

    private void prepareDetailsSection(SectionStack stack, ServerPlugin plugin) {
        DynamicForm form = new DynamicForm();
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

        detailsSection = section;
        ++initSectionCount;

        return;
    }
}
