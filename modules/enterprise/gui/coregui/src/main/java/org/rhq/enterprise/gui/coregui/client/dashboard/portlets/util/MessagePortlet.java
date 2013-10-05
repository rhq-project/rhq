/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
import org.rhq.core.domain.common.ProductInfo;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.admin.users.UsersDataSource;
import org.rhq.coregui.client.dashboard.ConfigurablePortlet;
import org.rhq.coregui.client.dashboard.Portlet;
import org.rhq.coregui.client.dashboard.PortletViewFactory;
import org.rhq.coregui.client.dashboard.PortletWindow;
import org.rhq.coregui.client.util.StringUtility;

/**
 * @author Greg Hinkle
 */
public class MessagePortlet extends HTMLPane implements ConfigurablePortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "Message";
    // A default displayed, persisted name for the portlet    
    public static final String NAME = CoreGUI.getMessages().view_portlet_defaultName_message();

    protected Messages MSG = CoreGUI.getMessages();

    private static final String MESSAGE_PROPERTY = "message";
    //    private static final String DEFAULT_MESSAGE = MSG.view_dashboardsManager_message_title_details();
    private static String DEFAULT_MESSAGE;
    {
        ProductInfo productInfo = CoreGUI.get().getProductInfo();
        String link1 = LinkManager.getAutodiscoveryQueueLink();
        String link2 = LinkManager.getAllResourcesLink();
        String link3 = LinkManager.getHelpLink();
        DEFAULT_MESSAGE = MSG.view_dashboardsManager_message_title_details(productInfo.getShortName(), link1, link2,
            link3);
    }

    public MessagePortlet() {
        super();
        setContentsType(ContentsType.PAGE);
    }

    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {
        String contents = storedPortlet.getConfiguration().getSimpleValue(MESSAGE_PROPERTY, null);
        if (contents != null) {
            if (UserSessionManager.getSessionSubject().getId() != UsersDataSource.ID_RHQADMIN
                && !contents.equals(DEFAULT_MESSAGE)) {
                // To avoid XSS attacks, don't allow non-superusers to enter HTML.
                // TODO (ips, 04/06/11): Sanitize this, rather than escaping it, once we upgrade to GWT 2.1 or later.
                contents = StringUtility.escapeHtml(contents);
            }
            setContents(contents);
        } else {
            setContents(DEFAULT_MESSAGE);
        }
    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_help_message());
    }

    public ConfigurationDefinition getConfigurationDefinition() {
        ConfigurationDefinition definition = new ConfigurationDefinition(MSG.view_portlet_configure_definitionTitle(),
            MSG.view_portlet_configure_definitionDesc());

        definition.put(new PropertyDefinitionSimple(MESSAGE_PROPERTY, MSG.view_portlet_message_title(), true,
            PropertySimpleType.LONG_STRING));

        return definition;
    }

    public static final class Factory implements PortletViewFactory {
        public static final PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(EntityContext context) {
            return new MessagePortlet();
        }
    }

}