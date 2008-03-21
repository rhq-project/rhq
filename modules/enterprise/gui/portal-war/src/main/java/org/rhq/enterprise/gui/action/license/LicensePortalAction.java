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
package org.rhq.enterprise.gui.action.license;

import java.util.HashMap;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.Portal;
import org.rhq.enterprise.gui.legacy.action.BaseDispatchAction;
import org.rhq.enterprise.gui.legacy.exception.ParameterNotFoundException;
import org.rhq.enterprise.gui.legacy.util.ActionUtils;
import org.rhq.enterprise.gui.legacy.util.BizappUtils;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.authz.PermissionException;

/**
 * Created by IntelliJ IDEA. User: ghinkle Date: Aug 8, 2005 Time: 10:34:55 AM To change this template use File |
 * Settings | File Templates.
 */
public class LicensePortalAction extends BaseDispatchAction {
    private static final String TITLE_VIEW = "admin.license.ViewLicenseTitle";
    private static final String PORTLET_VIEW = ".admin.license.View";

    private static final String TITLE_EDIT = "admin.license.EditLicenseTitle";
    private static final String PORTLET_EDIT = ".admin.license.Edit";

    protected static Log log = LogFactory.getLog(LicensePortalAction.class.getName());

    private static Properties keyMethodMap = new Properties();

    static {
        keyMethodMap.setProperty(Constants.MODE_VIEW, "viewLicense");
        keyMethodMap.setProperty(Constants.MODE_EDIT, "editLicense");
    }

    @Override
    protected Properties getKeyMethodMap() {
        return keyMethodMap;
    }

    public ActionForward viewLicense(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        setReturnPath(request, mapping, Constants.MODE_VIEW);

        Portal portal = Portal.createPortal(TITLE_VIEW, PORTLET_VIEW);
        portal.setWorkflowPortal(false);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward editLicense(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        if (!BizappUtils.hasPermission(request, Permission.MANAGE_SETTINGS)) {
            throw new PermissionException("User not authorized to update the license");
        }

        setReturnPath(request, mapping, Constants.MODE_VIEW);

        Portal portal = Portal.createPortal(TITLE_EDIT, PORTLET_EDIT);
        portal.setWorkflowPortal(false);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        if (request.getParameter("error") != null) {
            RequestUtils.setError(request, "admin.license.error.NotUpdated");
        }

        return null;
    }

    /**
     * Set the return path for the current action, including the mode and (if necessary) user id request parameters.
     *
     * @param request The request to get the session to store the return path into.
     * @param mapping The ActionMapping to get the return path from.
     * @param mode    The name of the current display mode.
     */
    protected void setReturnPath(HttpServletRequest request, ActionMapping mapping, String mode) throws Exception {
        HashMap params = new HashMap();
        params.put(Constants.MODE_PARAM, mode);
        try {
            params.put(Constants.USER_PARAM, RequestUtils.getUserId(request));
        } catch (ParameterNotFoundException e) {
            ; // not in a specific user's context
        }

        String returnPath = ActionUtils.findReturnPath(mapping, params);
        if (log.isTraceEnabled()) {
            log.trace("setting return path: " + returnPath);
        }

        SessionUtils.setReturnPath(request.getSession(), returnPath);
    }
}