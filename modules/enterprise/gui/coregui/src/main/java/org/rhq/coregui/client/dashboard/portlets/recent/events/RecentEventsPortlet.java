/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.coregui.client.dashboard.portlets.recent.events;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.coregui.client.dashboard.Portlet;
import org.rhq.coregui.client.dashboard.PortletViewFactory;

/**
 * @author Jirka Kremser
 */
public class RecentEventsPortlet extends AbstractRecentEventsPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "RecentEvents";
    // A default displayed, persisted name for the portlet    
    public static final String NAME = MSG.view_portlet_defaultName_recentEvents();

    public RecentEventsPortlet() {
        super(EntityContext.forSubsystemView());
    }

    public static final class Factory implements PortletViewFactory {
        public static final PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(EntityContext context) {

            return new RecentEventsPortlet();
        }
    }

}