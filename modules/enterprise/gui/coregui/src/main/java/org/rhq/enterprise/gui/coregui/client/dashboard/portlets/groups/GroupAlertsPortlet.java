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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups;

import com.google.gwt.user.client.History;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.alerts.AbstractRecentAlertsPortlet;

/**
 * @author Jay Shaughnessy
 * @author Simeon Pinder
 */
public class GroupAlertsPortlet extends AbstractRecentAlertsPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "GroupAlerts";
    // A default displayed, persisted name for the portlet
    public static final String NAME = MSG.view_portlet_defaultName_group_alerts();

    private int groupId;

    public GroupAlertsPortlet(String locatorId, int groupId) {

        // Since the group id is only used for filtering I don't think it matters whether this is a
        // standard group, autogroup, or autocluster, but if so, we'll have to provide more specific
        // contexts and more specific context handling.
        super(locatorId, EntityContext.forGroup(groupId));

        this.groupId = groupId;
    }

    public int getResourceId() {
        return groupId;
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        /* (non-Javadoc)
         * TODO:  This factory ASSUMES the user is currently navigated to a group detail view, and generates a portlet
         *        for that group.  It will fail in other scenarios.  This mechanism should be improved such that the
         *        factory method can take an EntityContext explicitly indicating, in this case, the group. 
         * @see org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory#getInstance(java.lang.String)
         */
        public final Portlet getInstance(String locatorId) {

            String currentPage = History.getToken();
            int groupId = -1;
            String[] elements = currentPage.split("/");
            // process for groups and auto groups Ex. ResourceGroup/10111 or ResourceGroup/AutoCluster/10321
            try {
                groupId = Integer.valueOf(elements[1]);
            } catch (NumberFormatException nfe) {
                groupId = Integer.valueOf(elements[2]);
            }

            return new GroupAlertsPortlet(locatorId, groupId);
        }
    }

}