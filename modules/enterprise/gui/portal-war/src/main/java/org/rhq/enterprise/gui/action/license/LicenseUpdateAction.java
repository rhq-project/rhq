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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.ContextUtils;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.license.CorruptLicenseException;
import org.rhq.enterprise.server.license.LicenseManager;
import org.rhq.enterprise.server.license.UnavailableLicenseException;
import org.rhq.enterprise.server.license.UpdateTrialLicenseException;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This action receives an updated license file through form submission and saves it out to the appropriate places in
 * the filesystem and then reloads the license manager system. License updates take force immediately.
 *
 * @author Greg Hinkle
 */
public class LicenseUpdateAction extends BaseAction {
    static Log log = LogFactory.getLog(LicenseUpdateAction.class.getName());

    // ---------------------------------------------------- Public Methods

    /**
     * Updates the installed license files on disk and invokes the LicenseManager's initialization routines to immediate
     * provide workable access to the application.
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        LicenseForm licenseForm = (LicenseForm) form;

        if (licenseForm.isCancelClicked()) {
            return returnCancelled(request, mapping);
        } else if (licenseForm.isResetClicked()) {
            return returnReset(request, mapping);
        } else if (licenseForm.isOkClicked()) {
            Subject subject = RequestUtils.getSubject(request);
            AuthorizationManagerLocal authzManager = LookupUtil.getAuthorizationManager();
            if (!authzManager.hasGlobalPermission(subject, Permission.MANAGE_SETTINGS)) {
                log.error("License file can only be updated by users who have permission to manage server settings");
                return returnFailure(request, mapping);
            }

            if ((licenseForm.licenseFile.getFileData() == null) || (licenseForm.licenseFile.getFileData().length == 0)) {
                return returnFailure(request, mapping);
            }

            log.debug("Updating license with uploaded file.");

            /*
             * Check to see if the license is valid, which means also checking whether it's valid with respect to any
             * license that may already be in the backing store.
             */
            InputStream is = new ByteArrayInputStream(licenseForm.licenseFile.getFileData());
            try {
                LicenseManager.checkLicense(is);
            } catch (CorruptLicenseException cle) {
                log.error("License file is corrupt or not in sync with previous records");
                return returnFailure(request, mapping);
            } catch (UpdateTrialLicenseException tlue) {
                log.error("Licenses can not be updated by trial licenses");
                return returnFailure(request, mapping);
            } catch (UnavailableLicenseException ule) {
                log.error("Backing store unavailable, allowing license to be written to disk unconditionally");
            } catch (Exception e) {
                log.error("Unable to update license file", e);
                return returnFailure(request, mapping);
            }

            log.debug("Uploaded license file validated");

            SystemManagerLocal systemManager = LookupUtil.getSystemManager();

            systemManager.updateLicense(licenseForm.licenseFile.getFileData());

            RequestUtils.setConfirmation(request, "admin.license.confirm.LicenseUpdated");

            ContextUtils.updateMonitoringEnabled(getServlet().getServletContext());
        }

        return returnSuccess(request, mapping);
    }
}