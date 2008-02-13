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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An Action that removes an alert
 */
public class RemoveAction extends BaseAction {
    /**
     * removes alerts
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(RemoveAction.class);
        log.debug("entering removeAlertsAction");

        RemoveForm nwForm = (RemoveForm) form;

        Integer resourceId = nwForm.getId();

        Map params = new HashMap();
        params.put(Constants.RESOURCE_ID_PARAM, resourceId);

        ActionForward forward = checkSubmit(request, mapping, form, params);

        // if the remove button was clicked, we are coming from
        // the alerts list page and just want to continue
        // processing ...
        if ((forward != null) && !forward.getName().equals(Constants.REMOVE_URL)) {
            log.trace("returning " + forward);

            // if there is no resourceId -- go to dashboard on cancel
            if (forward.getName().equals(Constants.CANCEL_URL) && (resourceId == null)) {
                return returnNoResource(request, mapping);
            }

            return forward;
        }

        Integer[] alertIds = nwForm.getAlerts();
        if (log.isDebugEnabled()) {
            log.debug("removing: " + Arrays.asList(alertIds));
        }

        if ((alertIds == null) || (alertIds.length == 0)) {
            return returnSuccess(request, mapping, params);
        }

        AlertManagerLocal alertManager = LookupUtil.getAlertManager();
        alertManager.deleteAlerts(alertIds);

        log.debug("!!!!!!!!!!!!!!!! removing alerts!!!!!!!!!!!!");

        if (resourceId == null) {
            return returnNoResource(request, mapping);
        } else {
            return returnSuccess(request, mapping, params);
        }
    }

    protected ActionForward returnNoResource(HttpServletRequest request, ActionMapping mapping) throws Exception {
        return constructForward(request, mapping, "noresource");
    }
}