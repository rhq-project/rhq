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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;
import org.rhq.core.domain.auth.Subject;

/**
 * View an alert definition -- notified roles.
 */
public class ViewDefinitionControlActionAction extends TilesAction {
    private Log log = LogFactory.getLog(ViewDefinitionControlActionAction.class.getName());

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Subject subject = org.rhq.enterprise.gui.legacy.util.RequestUtils.getSubject(request);

        //        ServletContext ctx = getServlet().getServletContext();
        //        int sessionID = RequestUtils.getSessionId(request);
        //        ControlBoss cb = ContextUtils.getControlBoss(ctx);
        //        EventsBoss eb = ContextUtils.getEventsBoss(ctx);
        //        boolean controlEnabled = false;
        //        try {
        //            try {
        //                AppdefEntityTypeID atid = RequestUtils.getEntityTypeId(request);
        //                controlEnabled = cb.isControlSupported(sessionID, atid);
        //            } catch (ParameterNotFoundException e) {
        //                AppdefEntityID adeId = RequestUtils.getEntityId(request);
        //                controlEnabled = cb.isControlEnabled(sessionID, adeId);
        //            }
        //        } catch (Exception e) {
        //            // if we can't get our resource, pretend control is disabled
        //            log.warn("Couldn't get resource.");
        //        }
        //        request.setAttribute(Constants.CONTROL_ENABLED,
        //          controlEnabled);
        //        log.debug(Constants.CONTROL_ENABLED + "=" + controlEnabled);
        //
        //        if (controlEnabled) {
        //            AlertDefinition alertDef =
        //                AlertDefUtil.getAlertDefinition(request, sessionID, eb);
        //            AlertDefUtil.ControlActionInfo cav =
        //                AlertDefUtil.getControlActionInfo(alertDef);
        //            if (null != cav) {
        //                request.setAttribute("controlAction", cav.controlAction);
        //            }
        //        }
        //
        return null;
    }
}

// EOF
