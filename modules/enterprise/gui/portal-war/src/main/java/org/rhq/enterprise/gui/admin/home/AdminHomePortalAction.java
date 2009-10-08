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
package org.rhq.enterprise.gui.admin.home;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.Portal;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.ActionUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;

/**
 * An <code>Action</code> that sets up the Admin Home portal
 */
public class AdminHomePortalAction extends BaseAction {
    private static final String TITLE_HOME = "user.admin.page.title";
    private static final String PORTLET_HOME = ".admin.home";

    protected static Log log = LogFactory.getLog(AdminHomePortalAction.class.getName());

    // ---------------------------------------------------- Public Methods

    /**
     * Set up the Admin Home portal.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(AdminHomePortalAction.class.getName());

        Portal portal = Portal.createPortal(TITLE_HOME, PORTLET_HOME);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        String returnPath = ActionUtils.findReturnPath(mapping, null);
        if (log.isTraceEnabled()) {
            log.trace("setting return path: " + returnPath);
        }

        SessionUtils.setReturnPath(request.getSession(), returnPath);

        return null;
    }
}