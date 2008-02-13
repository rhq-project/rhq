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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

public class ViewMetricMetadataAction extends TilesAction {
    @Override
    public ActionForward execute(ComponentContext cc, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        //        MetricMetadataForm cform = (MetricMetadataForm)form;
        //        ServletContext ctx = getServlet().getServletContext();
        //        int sessionId = RequestUtils.getSessionId(request).intValue();
        //        MeasurementBoss mb = ContextUtils.getMeasurementBoss(ctx);
        //        AppdefEntityID aid = new AppdefEntityID(cform.getEid());
        //
        //        AppdefEntityTypeID atid = null;
        //        if (cform.getCtype() != null) {
        //            atid = new AppdefEntityTypeID(cform.getCtype());
        //        }
        //
        //        List mdss = mb.findMetricMetadata(sessionId, aid, atid, cform.getM());
        //        request.setAttribute(Constants.METRIC_SUMMARIES_ATTR, mdss);
        if (true) {
            throw new IllegalStateException("deprecated code");
        }

        return null;
    }
}