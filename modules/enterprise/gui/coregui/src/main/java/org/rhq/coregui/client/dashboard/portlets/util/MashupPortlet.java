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
package org.rhq.coregui.client.dashboard.portlets.util;

import com.smartgwt.client.types.ContentsType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.HTMLPane;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.dashboard.ConfigurablePortlet;
import org.rhq.coregui.client.dashboard.Portlet;
import org.rhq.coregui.client.dashboard.PortletViewFactory;
import org.rhq.coregui.client.dashboard.PortletWindow;

/**
 * @author Greg Hinkle
 */
public class MashupPortlet extends HTMLPane implements ConfigurablePortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "Mashup";
    // A default displayed, persisted name for the portlet    
    public static final String NAME = CoreGUI.getMessages().view_portlet_defaultName_mashup();

    protected Messages MSG = CoreGUI.getMessages();

    public MashupPortlet() {
        super();
        setContentsType(ContentsType.PAGE);
    }

    @Override
    public void transformHTML(String s) {
        super.transformHTML(s);
    }

    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {
        String url = storedPortlet.getConfiguration().getSimpleValue("address", null);
        if (url != null) {
            setContentsURL(url);
        } else {
            setContents("<br/><i>" + MSG.view_portlet_configure_needed() + "</i>");
        }
    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_help_mashup());
    }

    public ConfigurationDefinition getConfigurationDefinition() {
        ConfigurationDefinition definition = new ConfigurationDefinition(MSG.view_portlet_configure_definitionTitle(),
            MSG.view_portlet_configure_definitionDesc());

        definition.put(new PropertyDefinitionSimple("address", MSG.common_title_web_address(), true,
            PropertySimpleType.STRING));

        return definition;
    }

    public static final class Factory implements PortletViewFactory {
        public static final PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(EntityContext context) {

            return new MashupPortlet();
        }
    }
}
