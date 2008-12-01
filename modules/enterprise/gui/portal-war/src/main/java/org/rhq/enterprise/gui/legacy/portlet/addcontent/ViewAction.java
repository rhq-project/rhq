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
package org.rhq.enterprise.gui.legacy.portlet.addcontent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;

public class ViewAction extends TilesAction {
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        List portlets = (List) context.getAttribute("portlets");
        WebUser user = SessionUtils.getWebUser(request.getSession());
        WebUserPreferences preferences = user.getPreferences();

        ArrayList availablePortlets = new ArrayList();
        String userPortlets = new String();

        Boolean wide = new Boolean((String) context.getAttribute("wide"));

        if (wide.booleanValue()) {
            userPortlets = preferences.getPreference(Constants.USER_PORTLETS_SECOND);
        } else {
            userPortlets = preferences.getPreference(Constants.USER_PORTLETS_FIRST);
        }

        for (Iterator i = portlets.iterator(); i.hasNext();) {
            String portlet = (String) i.next();

            if (userPortlets.indexOf(portlet) == -1) {
                availablePortlets.add(portlet);
            }
        }

        context.putAttribute("availablePortlets", availablePortlets);
        return null;
    }
}