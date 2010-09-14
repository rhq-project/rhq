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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class PluginConfigurationEditView extends LocatableVLayout {

    private Resource resource;
    private ConfigurationEditor editor;

    public PluginConfigurationEditView(String locatorId, Resource resource) {
        super(locatorId);

        this.resource = resource;
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

        IButton saveButton = new LocatableIButton(this.extendLocatorId("Save"), "Save");
        saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                save();
            }
        });
        //        saveButton.disable();
        toolStrip.addMember(saveButton);

        editor = new ConfigurationEditor(this.getLocatorId(), resource.getId(), resource.getResourceType().getId(),
            ConfigurationEditor.ConfigType.plugin);
        editor.setOverflow(Overflow.AUTO);

        addMember(toolStrip);

        addMember(editor);
    }

    private void save() {
        Configuration updatedConfiguration = editor.getConfiguration();

        GWTServiceLookup.getConfigurationService().updatePluginConfiguration(resource.getId(), updatedConfiguration,
            new AsyncCallback<PluginConfigurationUpdate>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to update configuration", caught);
                }

                public void onSuccess(PluginConfigurationUpdate result) {
                    CoreGUI.getMessageCenter().notify(
                        new Message("Connection settings updated for resource [" + resource.getName() + "]",
                            Message.Severity.Info));

                }
            });

    }
}