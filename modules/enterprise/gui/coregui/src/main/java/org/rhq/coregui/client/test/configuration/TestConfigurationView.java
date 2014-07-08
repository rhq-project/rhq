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
package org.rhq.coregui.client.test.configuration;

import java.util.EnumSet;
import java.util.Map;

import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.LayoutSpacer;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.coregui.client.components.configuration.PropertyValueChangeEvent;
import org.rhq.coregui.client.components.configuration.PropertyValueChangeListener;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedToolStrip;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.MessageCenter;

/**
 * @author Ian Springer
 */
public class TestConfigurationView extends EnhancedVLayout implements PropertyValueChangeListener {

    private ConfigurationEditor editor;
    private EnhancedIButton saveButton;
    private EnhancedToolStrip buttonBar;
    private ConfigurationDefinition configurationDefinition;
    private Configuration configuration;

    public TestConfigurationView() {
        super();
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        build();
    }

    public void build() {
        setWidth100();
        setHeight100();

        buttonBar = createButtonBar();

        this.configurationDefinition = TestConfigurationFactory.createConfigurationDefinition();
        this.configuration = TestConfigurationFactory.createConfiguration();

        reloadConfiguration();
    }

    private EnhancedToolStrip createButtonBar() {
        EnhancedToolStrip toolStrip = new EnhancedToolStrip();
        toolStrip.setWidth100();

        toolStrip.addMember(new LayoutSpacer());

        this.saveButton = new EnhancedIButton(MSG.common_button_save());
        this.saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                save();
            }
        });
        this.saveButton.disable();
        toolStrip.addMember(this.saveButton);
        toolStrip.addSpacer(40);
        return toolStrip;
    }

    @Override
    public void propertyValueChanged(PropertyValueChangeEvent event) {
        MessageCenter messageCenter = CoreGUI.getMessageCenter();
        Message message;
        if (event.isInvalidPropertySetChanged()) {
            Map<String, String> invalidPropertyNames = event.getInvalidPropertyNames();
            if (invalidPropertyNames.isEmpty()) {
                this.saveButton.enable();
                message = new Message("All properties now have valid values, so the configuration can now be saved.",
                    Message.Severity.Info, EnumSet.of(Message.Option.Transient, Message.Option.Sticky));
            } else {
                this.saveButton.disable();
                message = new Message("The following properties have invalid values: " + invalidPropertyNames.values()
                    + " - the values must be corrected before the configuration can be saved.", Message.Severity.Error,
                    EnumSet.of(Message.Option.Transient, Message.Option.Sticky));
            }
            messageCenter.notify(message);
        } else if (event.getInvalidPropertyNames().isEmpty()) {
            this.saveButton.enable();
        } else {
            this.saveButton.disable();
        }
    }

    private void reloadConfiguration() {
        this.saveButton.disable();
        if (editor != null) {
            editor.destroy();
            removeMember(editor);
            buttonBar.destroy();
            removeMember(buttonBar);
        }

        editor = createConfigurationEditor();
        addMember(editor);
        addMember(createButtonBar());
        markForRedraw();
    }

    protected ConfigurationEditor createConfigurationEditor() {
        ConfigurationEditor editor = new ConfigurationEditor(this.configurationDefinition, this.configuration);
        editor.setEditorTitle("Test Configuration");
        editor.setOverflow(Overflow.AUTO);
        editor.addPropertyValueChangeListener(this);
        return editor;
    }

    private void save() {
        StringBuilder str = new StringBuilder("=~pre~=");
        Configuration c = editor.getConfiguration();
        Map<String, Property> allProps = c.getAllProperties();
        for (Property prop : allProps.values()) {
            getPropertyStrings(prop, str, "");
        }
        str.append("=~/pre~=");
        CoreGUI.getMessageCenter().notify(new Message("Configuration updated.", str.toString()));
        reloadConfiguration();
    }

    private void getPropertyStrings(Property prop, StringBuilder str, String indent) {
        if (prop instanceof PropertySimple) {
            String value = ((PropertySimple) prop).getStringValue();
            str.append(indent + prop.getName() + "=" + ((value != null) ? value : "~~VALUE WAS NULL~~"));
            str.append("\n");
        } else if (prop instanceof PropertyMap) {
            str.append(indent + prop.getName() + " MAP:\n");
            PropertyMap propMap = (PropertyMap) prop;
            Map<String, Property> map = propMap.getMap();
            for (Property val : map.values()) {
                getPropertyStrings(val, str, indent + "   ");
            }
        } else if (prop instanceof PropertyList) {
            str.append(indent + prop.getName() + " LIST:\n");
            PropertyList propList = (PropertyList) prop;
            for (Property val : propList.getList()) {
                getPropertyStrings(val, str, indent + "   ");
            }
        } else {
            str.append("unknown prop type: " + prop);
        }
    }
}
