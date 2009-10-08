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
package org.rhq.enterprise.gui.legacy.portlet.autodiscovery;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;

public class PrepareAction extends BaseAction {
    /**
     * @param     mapping  The ActionMapping used to select this instance
     * @param     form     The optional ActionForm bean for this request (if any)
     * @param     request  The HTTP request we are processing
     * @param     response The HTTP response we are creating
     *
     * @exception Exception if the application business logic throws an exception
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        PropertiesForm propsForm = (PropertiesForm) form;

        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);
        WebUserPreferences preferences = user.getWebPreferences();

        propsForm.setRange(preferences.getAutoDiscoveryRange());

        return null;
    }
}