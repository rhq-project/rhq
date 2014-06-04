/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.coregui.client.dashboard;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.LayoutPolicy;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedToolStrip;

/**
 * This is a window for displaying portlet settings. The window contains a form which in turn will contain the
 * widgets for a portlet's settings. Those widgets will have to be passed to the window since the window is intended
 * to support all portlets. The layout and sizing strategy for this window is to be as large as the widgets contained
 * within it. And the form will stretch and resize as the window is resized. This flexible layout strategy is used
 * since there are different settings (with different widgets) for different portlets which will in turn result in
 * different sizes for the parent window.
 *
 * @author John Sanda
 */
public class PortletSettingsWindow extends Window {

    private Messages MSG = CoreGUI.getMessages();

    private PortletWindow parentWindow;
    private DashboardPortlet storedPortlet;
    private Portlet view;
    private EnhancedToolStrip toolStrip;

    public PortletSettingsWindow(PortletWindow parentWindow, DashboardPortlet storedPortlet,
        Portlet view) {
        super();
        this.parentWindow = parentWindow;
        this.storedPortlet = storedPortlet;
        this.view = view;
        setTitle(storedPortlet.getName() + " " + MSG.common_title_settings());
        setOverflow(Overflow.VISIBLE);
        setMinWidth(400);
        setMinHeight(400);
        setAutoSize(true);
        setAutoCenter(true);
        setCanDragResize(true);
        setCanDragReposition(true);

    }

    @Override
    protected void onInit() {
        super.onInit();

        VLayout layout = new VLayout();
        layout.setWidth100();
        layout.setHeight100();
        layout.setPadding(15);
        layout.setLayoutMargin(20);
        layout.setVPolicy(LayoutPolicy.FILL);

        if (view instanceof CustomSettingsPortlet) {
            final DynamicForm form = ((CustomSettingsPortlet) view).getCustomSettingsForm();
            layout.addMember(form);

            VLayout spacer = new VLayout();
            spacer.setHeight(10);
            layout.addMember(spacer);

            IButton cancel = new EnhancedIButton(MSG.common_button_cancel());
            cancel.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    PortletSettingsWindow.this.destroy();
                }
            });
            IButton save = new EnhancedIButton(MSG.common_button_save());
            save.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    if (form.validate()) {
                        // the form submit handler code is responsible for performing the configure and refresh as
                        // needed. This ensures that the updated configuration is used.                        
                        form.submit();

                        // the portlet does not have a handle on its container, so saving the new settings is down here
                        parentWindow.save();

                        // nuke the settings window now that its form field values have been processed 
                        PortletSettingsWindow.this.destroy();
                    }
                }
            });
            toolStrip = new EnhancedToolStrip();
            toolStrip.setPadding(5);
            toolStrip.setWidth100();
            toolStrip.setMembersMargin(15);
            toolStrip.addMember(cancel);
            toolStrip.addMember(save);
            toolStrip.setLayoutAlign(Alignment.CENTER);
        } else if (view instanceof ConfigurablePortlet) {
            ConfigurationDefinition definition = ((ConfigurablePortlet) view).getConfigurationDefinition();
            Configuration configuration = storedPortlet.getConfiguration();

            final ConfigurationEditor editor = new ConfigurationEditor(definition, configuration);
            editor.setWidth(400);
            editor.setHeight(400);
            layout.addMember(editor);

            IButton cancel = new EnhancedIButton(MSG.common_button_cancel());
            cancel.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    PortletSettingsWindow.this.destroy();
                }
            });
            IButton save = new EnhancedIButton(MSG.common_button_save());
            save.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    if (editor.validate()) {
                        // set the portlet config to the new config
                        Configuration configuration = editor.getConfiguration();
                        storedPortlet.setConfiguration(configuration);

                        // nuke the settings window now that its config data is stored
                        PortletSettingsWindow.this.destroy();

                        // configure the actual portlet and redraw
                        view.configure(parentWindow, storedPortlet);
                        parentWindow.markForRedraw();

                        // persist the updated config
                        parentWindow.save();
                    }
                }
            });
            
            toolStrip = new EnhancedToolStrip();
            toolStrip.setPadding(5);
            toolStrip.setWidth100();
            toolStrip.setMembersMargin(15);
            toolStrip.addMember(cancel);
            toolStrip.addMember(save);
            toolStrip.setLayoutAlign(Alignment.CENTER);
        } else {
            layout.addMember(new Label(MSG.view_portlet_configure_notNeeded()));
        }
        addItem(layout);
        if (null != toolStrip) addItem(toolStrip);
    }

}
