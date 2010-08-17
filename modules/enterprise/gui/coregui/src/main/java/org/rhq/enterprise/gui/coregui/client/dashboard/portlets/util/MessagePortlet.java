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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.util;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.types.ContentsType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.HTMLPane;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.ConfigurablePortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;

/**
 * @author Greg Hinkle
 */
public class MessagePortlet extends HTMLPane implements ConfigurablePortlet {

    public static final String KEY = "Message";

    public MessagePortlet() {
        setContentsType(ContentsType.PAGE);
    }

    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {
        String contents = storedPortlet.getConfiguration().getSimpleValue("message",null);
        if (contents != null) {
            setContents(contents);
        } else {
            setContents("<i>Message not yet configured, click the settings button to setup this portlet.");
        }
    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow("This portlet can display an HTML message on the dashboard.");
    }

    public ConfigurationDefinition getConfigurationDefinition() {
        ConfigurationDefinition definition = new ConfigurationDefinition("MessagePortlet Configuration", "The configuration settings for the message portlet.");

        definition.put(new PropertyDefinitionSimple("message", "Message", true, PropertySimpleType.LONG_STRING));

        return definition;
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance() {
            return GWT.create(MessagePortlet.class);
        }
    }
}