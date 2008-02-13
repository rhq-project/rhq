/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.action.portlet;

import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.Portal;
import org.rhq.enterprise.gui.legacy.WebUser;

/**
 */
public class DisplayAutoDiscoveryAction extends TilesAction {
    private static final Log log = LogFactory.getLog(DisplayAutoDiscoveryAction.class.getName());

    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        ServletContext ctx = getServlet().getServletContext();

        HttpSession session = request.getSession();

        Portal portal = (Portal) session.getAttribute(Constants.USERS_SES_PORTAL);
        if (portal == null) {
            portal = new Portal();
            portal.setName("dashboard.template.title");
            portal.setColumns(1);

            //construct from user preferences.
            WebUser user = (WebUser) session.getAttribute(Constants.WEBUSER_SES_ATTR);
            //portal.addPortletsFromString( user.getPreference(Constants.USER_PORTLETS_FIRST), 1);
            //portal.addPortletsFromString(".dashContent.addContent.narrow", 1);

            // TODO [ccrouch]
            // should be able to pull this portal sutff out into another action
            // and have it put its portal in the request, so not screwing up the one in the session,
            // under the same key.
            // Should then be able to reuse DashboardLayout.jsp for rendering
            // NEed to test this out because some of the page submissions off of this portlet will
            // likely cause it to re-render the true Dashboard.
            // would be nice if there was a way to turn off the "wrench" icon,
            // also it would be nice if you could set the autodiscovery range to be = all.
            // have to check whether setting this on our "instance" also futz with the dashboard one.

            //portal.addPortletsFromString( user.getPreference(Constants.USER_PORTLETS_SECOND), 2);
            //portal.addPortletsFromString(".dashContent.addContent.wide", 2);
            portal.addPortletsFromString(".dashContent.autoDiscovery", 1);

            List portlets = portal.getPortlets();
            for (Iterator iter = portlets.iterator(); iter.hasNext();) {
                Object portlet = (Object) iter.next();
                log.trace("object is" + portlet.getClass().getName());
                log.trace(portlet);
            }

            session.setAttribute(Constants.USERS_SES_PORTAL, portal);
        }

        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    }
}