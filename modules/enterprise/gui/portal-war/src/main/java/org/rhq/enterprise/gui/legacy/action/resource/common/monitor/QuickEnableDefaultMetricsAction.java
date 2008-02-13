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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.RetCodeConstants;
import org.rhq.enterprise.gui.legacy.action.BaseAction;

/**
 * Just turn on default metrics and runtime AI, then forward to success page.
 */
public class QuickEnableDefaultMetricsAction extends BaseAction {
    private static Log log = LogFactory.getLog(QuickEnableDefaultMetricsAction.class.getName());

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        //        ServletContext ctx = getServlet().getServletContext();
        //        MeasurementBoss mBoss = ContextUtils.getMeasurementBoss(ctx);
        //        WebUser user = SessionUtils.getWebUser(request.getSession());
        //        int sessionId = user.getSessionId().intValue();
        //
        //        int type = RequestUtils.getIntParameter(request, "type").intValue();
        //        int id = RequestUtils.getIntParameter(request, "id").intValue();
        //        AppdefEntityID aid = new AppdefEntityID(type, id);
        //
        //        mBoss.enableDefaultMetricsAndRuntimeAI(sessionId, aid);
        if (true) {
            throw new IllegalStateException("deprecated code");
        }

        return mapping.findForward(RetCodeConstants.SUCCESS_URL);
    }
}