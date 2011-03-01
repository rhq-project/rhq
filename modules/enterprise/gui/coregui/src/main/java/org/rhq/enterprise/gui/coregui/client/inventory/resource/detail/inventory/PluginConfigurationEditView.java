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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.inventory;

import java.util.EnumSet;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeEvent;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeListener;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.MessageCenter;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A view for editing a Resource's plugin configuration (aka connection settings).
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class PluginConfigurationEditView extends LocatableVLayout implements PropertyValueChangeListener,
    RefreshableView {

    private Resource resource;
    private ResourcePermission resourcePermission;
    private ConfigurationEditor editor;
    private LocatableIButton saveButton;

    // flags to indicate if the config editor is refreshing its internal config/configDef objects
    private boolean refreshingConfig = false;
    private boolean refreshingConfigDef = false;

    public PluginConfigurationEditView(String locatorId, ResourceComposite resourceComposite) {
        super(locatorId);

        this.resource = resourceComposite.getResource();
        this.resourcePermission = resourceComposite.getResourcePermission();
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        ToolStrip toolStrip = new ToolStrip();
        toolStrip.setWidth100();
        toolStrip.setExtraSpace(10);
        toolStrip.setMembersMargin(5);
        toolStrip.setLayoutMargin(5);

        this.saveButton = new LocatableIButton(this.extendLocatorId("Save"), MSG.common_button_save());
        this.saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                save();
            }
        });
        this.saveButton.disable();
        toolStrip.addMember(this.saveButton);

        addMember(toolStrip);
        refresh();

        if (!this.resourcePermission.isInventory()) {
            Message message = new Message(MSG.view_connectionSettingsDetails_noPermission(), Message.Severity.Info,
                EnumSet.of(Message.Option.Transient));
            CoreGUI.getMessageCenter().notify(message);
        }
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
        editor = new ConfigurationEditor(extendLocatorId("Editor"), resource.getId(), resource.getResourceType()
            .getId(), ConfigurationEditor.ConfigType.plugin);
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

        addMember(editor);
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
        if (event.isValidationStateChanged()) {
            Set<String> invalidPropertyNames = event.getInvalidPropertyNames();
            if (invalidPropertyNames.isEmpty()) {
                this.saveButton.enable();
                message = new Message(MSG.view_connectionSettingsDetails_allPropertiesValid(), Message.Severity.Info,
                    EnumSet.of(Message.Option.Transient, Message.Option.Sticky));
            } else {
                this.saveButton.disable();
                message = new Message(MSG.view_connectionSettingsDetails_somePropertiesInvalid(invalidPropertyNames
                    .toString()), Message.Severity.Error, EnumSet.of(Message.Option.Transient, Message.Option.Sticky));
            }
            messageCenter.notify(message);
        } else {
            this.saveButton.enable();
        }
    }

}