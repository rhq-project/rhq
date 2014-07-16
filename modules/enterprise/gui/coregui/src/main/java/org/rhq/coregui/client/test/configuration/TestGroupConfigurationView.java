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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.coregui.client.components.configuration.GroupConfigurationEditor;
import org.rhq.coregui.client.components.configuration.GroupMemberConfiguration;
import org.rhq.coregui.client.components.configuration.PropertyValueChangeEvent;
import org.rhq.coregui.client.components.configuration.PropertyValueChangeListener;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.MessageCenter;

/**
 * @author Ian Springer
 */
public class TestGroupConfigurationView extends EnhancedVLayout implements PropertyValueChangeListener {

    private static final int GROUP_SIZE = 2;

    private ConfigurationEditor editor;
    private EnhancedIButton saveButton;
    private ToolStrip buttonBar;
    private ConfigurationDefinition configurationDefinition;
    private List<GroupMemberConfiguration> memberConfigurations;

    public TestGroupConfigurationView() {
        super();
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        setWidth100();
        setHeight100();

        this.configurationDefinition = TestConfigurationFactory.createConfigurationDefinition();
        this.memberConfigurations = new ArrayList<GroupMemberConfiguration>(GROUP_SIZE);
        for (int i = 0; i < GROUP_SIZE; i++) {
            Configuration configuration = TestConfigurationFactory.createConfiguration();
            GroupMemberConfiguration memberConfiguration = new GroupMemberConfiguration(i, "Member #" + i,
                configuration);
            this.memberConfigurations.add(memberConfiguration);
        }
        reloadConfiguration();
    }

    private ToolStrip createButtonBar() {
        buttonBar = new ToolStrip();
        buttonBar.setWidth100();
        buttonBar.addMember(new LayoutSpacer());

        this.saveButton = new EnhancedIButton(MSG.common_button_save());
        this.saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                save();
            }
        });
        this.saveButton.disable();
        buttonBar.addMember(this.saveButton);
        buttonBar.addSpacer(40);
        return buttonBar;
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

    protected GroupConfigurationEditor createConfigurationEditor() {
        GroupConfigurationEditor editor = new GroupConfigurationEditor(this.configurationDefinition,
            this.memberConfigurations);
        editor.setEditorTitle("Test Group Configuration");
        editor.setOverflow(Overflow.AUTO);
        editor.addPropertyValueChangeListener(this);
        return editor;
    }

    private void save() {
        CoreGUI.getMessageCenter().notify(
            new Message("Member configurations updated.", "Member configurations updated."));
        reloadConfiguration();
    }

}
