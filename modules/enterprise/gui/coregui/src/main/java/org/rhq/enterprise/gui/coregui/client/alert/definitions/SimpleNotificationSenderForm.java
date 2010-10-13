/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.alert.definitions;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Label;

import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

/**
 * This notification form will be used for most alert senders since most alert senders
 * only need to be given a simple set of configuration properties where the user
 * provides values via the normal configuration editor.
 *
 * @author John Mazzitelli
 */
public class SimpleNotificationSenderForm extends AbstractNotificationSenderForm {

    private ConfigurationEditor configEditor;

    public SimpleNotificationSenderForm(String locatorId, AlertNotification notif, String sender) {
        super(locatorId, notif, sender);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        GWTServiceLookup.getAlertDefinitionService().getConfigurationDefinitionForSender(getSender(),
            new AsyncCallback<ConfigurationDefinition>() {
                @Override
                public void onSuccess(ConfigurationDefinition configDef) {
                    configEditor = new ConfigurationEditor(extendLocatorId("configEditor"), configDef,
                        getConfiguration());
                    configEditor.setHeight(400);
                    configEditor.setWidth(600);
                    addMember(configEditor);
                    markForRedraw();
                }

                @Override
                public void onFailure(Throwable caught) {
                    String errMsg = "Cannot get alert sender configuration definition";
                    CoreGUI.getErrorHandler().handleError(errMsg, caught);
                    Label label = new Label(errMsg);
                    label.setIcon("[SKIN]/Dialog/error.png");
                    addMember(label);
                    markForRedraw();
                }
            });
    }

    @Override
    public boolean validate() {
        if (configEditor != null) {
            return configEditor.validate();
        }
        return true;
    }

}
