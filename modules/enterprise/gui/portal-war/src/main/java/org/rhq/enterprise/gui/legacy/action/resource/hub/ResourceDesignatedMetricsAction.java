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
package org.rhq.enterprise.gui.legacy.action.resource.hub;

import java.util.HashSet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.rhq.enterprise.server.legacy.measurement.MeasurementConstants;

/**
 * Fetch the designated metrics for a resource
 */
public class ResourceDesignatedMetricsAction extends TilesAction {
    private static Log log = LogFactory.getLog(ResourceDesignatedMetricsAction.class.getName());

    private static HashSet CATEGORIES;

    static {
        CATEGORIES = new HashSet();
        CATEGORIES.add(MeasurementConstants.CAT_AVAILABILITY);
        CATEGORIES.add(MeasurementConstants.CAT_UTILIZATION);
        CATEGORIES.add(MeasurementConstants.CAT_THROUGHPUT);
    };

    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        //        String appdefKey =
        //            (String) context.getAttribute(Constants.ENTITY_ID_PARAM);
        //        AppdefEntityID entityId = new AppdefEntityID(appdefKey);
        //
        //        int sessionId = RequestUtils.getSessionId(request).intValue();
        //        ServletContext ctx = getServlet().getServletContext();
        //        MeasurementBoss boss = ContextUtils.getMeasurementBoss(ctx);
        //
        //        List designates;
        //        try {
        //            designates =
        //                boss.getDesignatedTemplates(sessionId, entityId, CATEGORIES);
        //
        //            if (designates.size() > 3) {
        //                // Only want one of each
        //                HashSet seen = new HashSet();
        ////                for (Iterator it = designates.iterator(); it.hasNext(); ) {
        ////                    MeasurementTempl mtv =
        ////                        (MeasurementTempl) it.next();
        ////
        ////                    String cat = mtv.getCategory().getName();
        ////                    if (seen.contains(cat))
        ////                        it.remove();
        ////                    else
        ////                        seen.add(cat);
        ////                }
        //            }
        //
        //            context.putAttribute(Constants.CTX_SUMMARIES, designates);
        //        } catch (MeasurementNotFoundException e) {
        //            log.debug("No designated metric for " + entityId + " found");
        //        }
        //
        //        // set the resource performance flag
        //        RtBoss rtBoss = ContextUtils.getRtBoss(ctx);
        //        boolean isPerformable = false;
        //        if (entityId.getType() !=
        //            AppdefEntityConstants.APPDEF_TYPE_APPLICATION) {
        //            isPerformable = rtBoss.isAnySupported(sessionId, entityId);
        //        }
        //        else {
        //            InventoryHelper helper =
        //                InventoryHelper.getHelper(entityId);
        //
        //            AppdefBoss appdefBoss = ContextUtils.getAppdefBoss(ctx);
        //
        //            if (log.isDebugEnabled())
        //                log.debug("finding resource [" + entityId + "]");
        //
        //            AppdefResourceValue resource =
        //                appdefBoss.findById(sessionId, entityId);
        //
        //            // check to see if any of the application's
        //            // services support performance
        //            List childTypes =
        //                helper.getChildResourceTypes(request, ctx, resource);
        //
        //            for (Iterator it = childTypes.iterator(); it.hasNext();) {
        //                AppdefResourceTypeValue type =
        //                    (AppdefResourceTypeValue) it.next();
        //                if (isPerformable = rtBoss.isAnySupported(sessionId, type))
        //                    break;
        //            }
        //        }
        //
        //        request.setAttribute(Constants.PERFORMANCE_SUPPORTED_ATTR,
        //                             new Boolean(isPerformable));
        if (true) {
            throw new IllegalStateException("deprecated code");
        }

        return null;
    }
}