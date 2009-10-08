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

import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.license.License;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Created by IntelliJ IDEA. User: ghinkle Date: Aug 5, 2005 Time: 9:41:08 AM To change this template use File |
 * Settings | File Templates.
 */
public class LicenseController extends TilesAction {
    private static final Log log = LogFactory.getLog(org.rhq.enterprise.gui.action.license.LicenseController.class
        .getName());

    public static final String LICENSE = "license";

    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        ServletContext ctx = getServlet().getServletContext();

        HttpSession session = request.getSession();

        SystemManagerLocal systemManager = LookupUtil.getSystemManager();

        WebUser user = SessionUtils.getWebUser(session);
        request.setAttribute("userId", user.getId());

        License license = null;
        /*try
         *{*/
        license = systemManager.getLicense();

        // no license exists
        if (license == null) {
            RequestUtils.setError(request, "admin.license.LicenseNotLoaded");

            // license exists, but is expired
        } else if (license.getLicenseExpiration() < System.currentTimeMillis()) {
            RequestUtils.setError(request, "admin.license.LicenseExpired");
        }

        if (request.getParameter("update") != null) {
            RequestUtils.setConfirmation(request, "admin.license.confirm.LicenseUpdated");
        }

        request.setAttribute(LICENSE, license);

        // TODO GH: Fix the rest of this
        /*}
         * catch (UpdateTrialLicenseException tlue) {
         */
        /*
         * can't perform an update with trial licenses unless you're reinstalling, using the same one, and the trial
         * hasn't expired yet; thus, don't set the request attribute here
         */
        /*
         * RequestUtils.setError(request, "admin.license.error.TrialLicenseNotUpdated"); } catch
         * (CorruptLicenseException cle) { RequestUtils.setError(request, "admin.license.error.LicenseCorrupt"); } catch
         * (UnavailableLicenseException ule) { // the backing store was unavailable, let them use it, set the license
         * attrib request.setAttribute(LICENSE, license);} */

        return null;
    }
}