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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration;

import java.util.EnumSet;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeEvent;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeListener;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.MessageCenter;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A view for editing a Resource's configuration.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ResourceConfigurationEditView extends LocatableVLayout implements PropertyValueChangeListener {
    private Resource resource;
    private ResourcePermission resourcePermission;
    private ConfigurationEditor editor;
    private IButton saveButton;

    public ResourceConfigurationEditView(String locatorId, ResourceComposite resourceComposite) {
        super(locatorId);

        this.resource = resourceComposite.getResource();
        this.resourcePermission = resourceComposite.getResourcePermission();
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        build();
    }

    public void build() {
        ToolStrip toolStrip = new ToolStrip();
        toolStrip.setWidth100();

        toolStrip.addMember(new LayoutSpacer());

        this.saveButton = new LocatableIButton(this.extendLocatorId("Save"), "Save");
        this.saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                save();
            }
        });
        toolStrip.addMember(saveButton);
        
        addMember(toolStrip);
        reloadConfiguration();

        if (!this.resourcePermission.isConfigureWrite()) {
            Message message = new Message("You do not have permission to edit this Resource's configuration.",
                Message.Severity.Info, EnumSet.of(Message.Option.Transient, Message.Option.Sticky));
            CoreGUI.getMessageCenter().notify(message);
        }
    }

    private void reloadConfiguration() {
        this.saveButton.disable();
        if (editor != null) {
            editor.destroy();
            removeMember(editor);
        }
        editor = new ConfigurationEditor(this.extendLocatorId("Editor"), resource.getId(),
            resource.getResourceType().getId());
        editor.setOverflow(Overflow.AUTO);
        editor.addPropertyValueChangeListener(this);
        editor.setReadOnly(!this.resourcePermission.isConfigureWrite());
        addMember(editor);
    }

    private void save() {
        Configuration updatedConfiguration = editor.getConfiguration();

        GWTServiceLookup.getConfigurationService().updateResourceConfiguration(resource.getId(), updatedConfiguration,
            new AsyncCallback<ResourceConfigurationUpdate>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to update configuration.", caught);
                }

                public void onSuccess(ResourceConfigurationUpdate result) {
                    CoreGUI.getMessageCenter().notify(
                        new Message("Configuration updated.",
                            "Configuration updated for Resource [" + resource.getName() + "].",
                            Message.Severity.Info));
                    reloadConfiguration();
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
                message = new Message("All configuration properties have valid values, so the configuration can now be saved.",
                    Message.Severity.Info, EnumSet.of(Message.Option.Transient, Message.Option.Sticky));
            } else {
                this.saveButton.disable();
                message = new Message("The following configuration properties have invalid values: "
                    + invalidPropertyNames + ". The values must be corrected before the configuration can be saved.",
                    Message.Severity.Error, EnumSet.of(Message.Option.Transient, Message.Option.Sticky));
            }
            messageCenter.notify(message);
        } else {
            this.saveButton.enable();
        }
    }
}
