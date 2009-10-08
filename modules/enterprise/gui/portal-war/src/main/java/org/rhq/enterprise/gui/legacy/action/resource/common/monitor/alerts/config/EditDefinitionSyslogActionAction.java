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

import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;

/**
 * Create a new alert definition.
 */
public class EditDefinitionSyslogActionAction extends BaseAction {
    private Log log = LogFactory.getLog(EditDefinitionSyslogActionAction.class.getName());

    // ---------------------------------------------------- Public Methods

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        SyslogActionForm saForm = (SyslogActionForm) form;

        ServletContext ctx = getServlet().getServletContext();
        int sessionID = RequestUtils.getSessionId(request).intValue();
        Integer rid = saForm.getRid();
        //        Integer type = saForm.getType();
        //        EventsBoss eb = ContextUtils.getEventsBoss(ctx);

        Map params = new HashMap();
        params.put(Constants.RESOURCE_PARAM, rid);
        //        params.put(Constants.RESOURCE_TYPE_ID_PARAM, type);
        params.put("ad", saForm.getAd());

        ActionForward forward = checkSubmit(request, mapping, form, params);
        if (forward != null) {
            log.trace("returning " + forward);
            return forward;
        }

        //        AlertDefinition alertDef = eb.getAlertDefinition( sessionID, saForm.getAd() );
        //        Action action = AlertDefUtil.getSyslogActionValue(alertDef);
        //        if ( saForm.getShouldBeRemoved() ) {
        //            if (null != action) {
        //                alertDef.removeAction(action);
        //                eb.updateAlertDefinition(sessionID, alertDef);
        //            }
        //        } else {
        //            SyslogActionConfig sa = new SyslogActionConfig();
        //            sa.setMeta( saForm.getMetaProject() );
        //            sa.setProduct( saForm.getProject() );
        //            sa.setVersion( saForm.getVersion() );
        //            ConfigResponse configResponse = sa.getConfigResponse();
        //            if (null == action) {
        //                eb.createAction( sessionID, saForm.getAd(),
        //                                 sa.getImplementor(), configResponse );
        //            } else {
        //                action.setConfig( configResponse.encode() );
        //                eb.updateAction(sessionID, action);
        //            }
        //        }

        return returnSuccess(request, mapping, params);
    }
}

// EOF
