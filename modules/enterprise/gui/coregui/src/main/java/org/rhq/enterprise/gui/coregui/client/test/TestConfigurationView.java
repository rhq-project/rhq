/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.gui.coregui.client.test;

import java.util.EnumSet;

import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeEvent;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeListener;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.MessageCenter;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Ian Springer
 */
public class TestConfigurationView
    extends LocatableVLayout implements PropertyValueChangeListener {
    public static final String VIEW_ID = "TestConfig";

    private ConfigurationEditor editor;
    private LocatableIButton saveButton;
    private ConfigurationDefinition configurationDefinition;
    private Configuration configuration;

    public TestConfigurationView(String locatorId) {
        super(locatorId);
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        build();
    }

    public void build() {
        setWidth100();
        setHeight100();
        
        ToolStrip toolStrip = new ToolStrip();
        toolStrip.setWidth100();

        toolStrip.addMember(new LayoutSpacer());

        this.saveButton = new LocatableIButton(this.extendLocatorId("Save"), "Save");
        this.saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                save();
            }
        });
        this.saveButton.disable();
        toolStrip.addMember(this.saveButton);

        addMember(toolStrip);

        this.configurationDefinition = TestConfigurationFactory.createConfigurationDefinition();
        this.configuration = TestConfigurationFactory.createConfiguration();

        reloadConfiguration();
    }

    @Override
    public void propertyValueChanged(PropertyValueChangeEvent event) {
        MessageCenter messageCenter = CoreGUI.getMessageCenter();
        Message message;
        if (event.isValidationStateChanged()) {
            if (event.getInvalidPropertyNames().isEmpty()) {
                this.saveButton.enable();
                message = new Message("All properties now have valid values, so the configuration can now be saved.",
                    Message.Severity.Info, EnumSet.of(Message.Option.Transient));
            }
            else {
                this.saveButton.disable();
                message = new Message(
                    "One or more properties have invalid values. The values must be corrected before the configuration can be saved.",
                    Message.Severity.Error, EnumSet.of(Message.Option.Transient, Message.Option.Sticky));
            }
            messageCenter.notify(message);
        }
        else {
            this.saveButton.enable();
        }
    }

    private void reloadConfiguration() {
        this.saveButton.disable();
        if (editor != null) {
            editor.destroy();
            removeMember(editor);
        }

        editor = new ConfigurationEditor(extendLocatorId("Editor"), this.configurationDefinition, this.configuration);
        editor.setOverflow(Overflow.AUTO);
        editor.addPropertyValueChangeListener(this);
        addMember(editor);
    }

    private void save() {
        CoreGUI.getMessageCenter().notify(
            new Message("Configuration updated.", "Test configuration updated."));
        reloadConfiguration();
    }    
}
