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
package org.rhq.enterprise.gui.admin.config;

import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.legacy.common.shared.HQConstants;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class EditConfigAction extends BaseAction {
    Log log = LogFactory.getLog(EditConfigAction.class.getName());

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        ActionForward forward = checkSubmit(request, mapping, form);
        if (forward != null) {
            return forward;
        }

        Subject whoami = RequestUtils.getSubject(request);
        SystemConfigForm cForm = (SystemConfigForm) form;
        ServletContext ctx = getServlet().getServletContext();
        SystemManagerLocal systemManager = LookupUtil.getSystemManager();

        if (cForm.isOkClicked()) {
            try {
                log.trace("Getting config");
                Properties props = cForm.saveConfigProperties(systemManager.getSystemConfiguration());

                log.trace("Setting config");
                systemManager.setSystemConfiguration(whoami, props);

                log.trace("Restarting config service");

                systemManager.reconfigureSystem(whoami);

                if (cForm.getLdapEnabled() != null) {
                    // cache in servlet context
                    ctx.setAttribute(Constants.JAAS_PROVIDER_CTX_ATTR, HQConstants.LDAPJAASProvider);
                } else {
                    ctx.setAttribute(Constants.JAAS_PROVIDER_CTX_ATTR, HQConstants.JDBCJAASProvider);
                }
            } catch (Exception e) {
                String throwableMsgs = ThrowableUtil.getAllMessages(e, true);
                log.error("Failed to store server settings. Cause: " + throwableMsgs);
                RequestUtils.setErrorObject(request, "admin.config.confirm.saveSettingsFailure", throwableMsgs);
                return returnFailure(request, mapping);
            }
        }

        RequestUtils.setConfirmation(request, "admin.config.confirm.saveSettings");
        return returnSuccess(request, mapping);
    }
}