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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Struts action to acknowledge one single alert from the
 * ViewAlertProperties.jsp page
 * @author Heiko W. Rupp
 */
public class AckAlertAction extends BaseAction {

    Log log = LogFactory.getLog(AckAlertAction.class);

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        Subject subject = RequestUtils.getSubject(request);
        AlertManagerLocal alertManager = LookupUtil.getAlertManager();

        Map params = new HashMap(3);
        // pass-through the alertId and resource id
        Integer alertId = new Integer(request.getParameter("a"));
        request.setAttribute("a", alertId);
        params.put("a", alertId);

        Integer resourceId = new Integer(request.getParameter("id"));
        request.setAttribute("id", resourceId);
        params.put("id", resourceId);

        String mode = request.getParameter("mode");
        request.setAttribute("mode", mode);
        params.put("mode", mode);

        alertManager.acknowledgeAlerts(subject, new int[] { alertId });

        log.debug("Acknowledged Alert with id " + alertId + " and user " + subject.getName());

        return returnSuccess(request, mapping, params);
    }

}
