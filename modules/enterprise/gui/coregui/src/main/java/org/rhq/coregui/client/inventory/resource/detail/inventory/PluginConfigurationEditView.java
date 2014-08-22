/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.coregui.client.inventory.resource.detail.inventory;

import java.util.EnumSet;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.RefreshableView;
import org.rhq.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.coregui.client.components.configuration.PropertyValueChangeEvent;
import org.rhq.coregui.client.components.configuration.PropertyValueChangeListener;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.enhanced.EnhancedToolStrip;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.MessageCenter;

/**
 * A view for editing a Resource's plugin configuration (aka connection settings).
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class PluginConfigurationEditView extends EnhancedVLayout implements PropertyValueChangeListener,
    RefreshableView {

    private Resource resource;
    private ResourcePermission resourcePermission;
    private ConfigurationEditor editor;
    private EnhancedIButton saveButton;

    // flags to indicate if the config editor is refreshing its internal config/configDef objects
    private boolean refreshingConfig = false;
    private boolean refreshingConfigDef = false;

    public PluginConfigurationEditView(ResourceComposite resourceComposite) {
        super();

        this.resource = resourceComposite.getResource();
        this.resourcePermission = resourceComposite.getResourcePermission();
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        
        EnhancedToolStrip toolStrip = new EnhancedToolStrip();
        toolStrip.setBackgroundImage(null);
        toolStrip.setWidth100();
        toolStrip.setMembersMargin(3);
        toolStrip.setPadding(3);

        this.saveButton = new EnhancedIButton(MSG.common_button_save(), ButtonColor.BLUE);
        this.saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                save();
            }
        });
        this.saveButton.disable();
        toolStrip.addMember(this.saveButton);

        addMember(toolStrip);

        if (!this.resourcePermission.isInventory()) {
            Message message = new Message(MSG.view_connectionSettingsDetails_noPermission(), Message.Severity.Info,
                EnumSet.of(Message.Option.Transient));
            CoreGUI.getMessageCenter().notify(message);
        }
        refresh();
    }

    @Override
    public void refresh() {
        if (this.refreshingConfig || this.refreshingConfigDef) {
            return; // we are already in the process of refreshing, don't do it again
        }

        this.refreshingConfig = true;
        this.refreshingConfigDef = true;
        this.saveButton.disable();

        if (editor != null) {
            editor.destroy();
            removeMember(editor);
        }
        // TODO (ips): Load the config and config def ourselves, so we can remove that logic from the ConfigurationEditor,
        //       whose only purpose should be to render a config.
        editor = new ConfigurationEditor(resource.getId(), resource.getResourceType().getId(),
            ConfigurationEditor.ConfigType.plugin);
        editor.setOverflow(Overflow.AUTO);
        editor.addPropertyValueChangeListener(this);
        editor.setReadOnly(!this.resourcePermission.isInventory());
        editor.setLoadHandler(new ConfigurationEditor.LoadHandler() {
            @Override
            public void loadedConfigurationDefinition(ConfigurationDefinition configDef) {
                refreshingConfig = false; // finished loading the config
            }

            @Override
            public void loadedConfiguration(Configuration config) {
                refreshingConfigDef = false; // finished loading the config def
            }
        });

        addMember(editor, 0);
        // TODO (ips): If editor != null, use editor.reload() instead.
    }

    private void save() {
        Configuration updatedConfiguration = editor.getConfiguration();

        GWTServiceLookup.getConfigurationService().updatePluginConfiguration(resource.getId(), updatedConfiguration,
            new AsyncCallback<PluginConfigurationUpdate>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_connectionSettingsDetails_error_updateFailure(),
                        caught);
                }

                public void onSuccess(PluginConfigurationUpdate result) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.view_connectionSettingsDetails_messageConcise_updateSuccess(), MSG
                            .view_connectionSettingsDetails_messageDetailed_updateSuccess(resource.getName())));
                    refresh();
                }
            });
    }

    @Override
    public void propertyValueChanged(PropertyValueChangeEvent event) {
        MessageCenter messageCenter = CoreGUI.getMessageCenter();
        Message message;
        if (event.isInvalidPropertySetChanged()) {
            Map<String, String> invalidPropertyNames = event.getInvalidPropertyNames();
            if (invalidPropertyNames.isEmpty()) {
                this.saveButton.enable();
                message = new Message(MSG.view_connectionSettingsDetails_allPropertiesValid(), Message.Severity.Info,
                    EnumSet.of(Message.Option.Transient, Message.Option.Sticky));
            } else {
                this.saveButton.disable();
                message = new Message(MSG.view_connectionSettingsDetails_somePropertiesInvalid(invalidPropertyNames
                    .values().toString()), Message.Severity.Error, EnumSet.of(Message.Option.Transient,
                    Message.Option.Sticky));
            }
            messageCenter.notify(message);
        } else if (event.getInvalidPropertyNames().isEmpty()) {
            this.saveButton.enable();
        } else {
            this.saveButton.disable();
        }
    }

}