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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.resource;

import com.google.gwt.user.client.History;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.alerts.AbstractRecentAlertsPortlet;

/**
 * @author Jay Shaughnessy
 * @author Simeon Pinder
 */
public class ResourceAlertsPortlet extends AbstractRecentAlertsPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "ResourceAlerts";
    // A default displayed, persisted name for the portlet
    public static final String NAME = MSG.view_portlet_defaultName_resource_alerts();

    private int resourceId;

    public ResourceAlertsPortlet(String locatorId, int resourceId) {

        super(locatorId, EntityContext.forResource(resourceId));

        this.resourceId = resourceId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        /* (non-Javadoc)
         * TODO:  This factory ASSUMES the user is currently navigated to a resource detail view, and generates a portlet
         *        for that resource.  It will fail in other scenarios.  This mechanism should be improved such that the
         *        factory method can take an EntityContext explicitly indicating, in this case, the resource. 
         * @see org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory#getInstance(java.lang.String)
         */
        public final Portlet getInstance(String locatorId) {

            String currentPage = History.getToken();
            String[] elements = currentPage.split("/");
            int resourceId = Integer.valueOf(elements[1]);

            return new ResourceAlertsPortlet(locatorId, resourceId);
        }
    }

}