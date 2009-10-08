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
package org.rhq.enterprise.gui.legacy.action.resource.group.monitor.config;

import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;

/**
 * An Action that retrieves data from the BizApp to facilitate display of the <em>Edit Availability</em> pages for a
 * compatible group's metrics.
 */
public class EditAvailabilityFormPrepareAction extends Action {
    // ---------------------------------------------------- Public Methods

    /**
     * Retrieve this data and store it in the specified request parameters:
     *
     * <ul>
     *   <li><code>AppdefEntityId</code> object identified by <code>Constants.RESOURCE_ID_PARAM</code> and <code>
     *     Constants.RESOURCE_TYPE_PARAM</code></li>
     *   <li><code>List</code> of available <code>Metrics</code> objects (those not already associated with the
     *     resource) in <code>Constants.AVAIL_METRICS_ATTR</code></li>
     *   <li><code>Integer</code> number of available metrics in <code>Constants.NUM_AVAIL_METRICS_ATTR</code></li>
     *   <li><code>List</code> of pending <code>Metrics</code> objects (those in queue to be associated with the
     *     resource) in <code>Constants.PENDING_METRICS_ATTR</code></li>
     *   <li><code>Integer</code> number of pending metrics in <code>Constants.NUM_PENDING_METRICS_ATTR</code></li>
     * </ul>
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(EditAvailabilityFormPrepareAction.class.getName());
        GroupMonitoringConfigForm addForm = (GroupMonitoringConfigForm) form;
        HashMap parms = new HashMap();
        Integer resourceId = addForm.getRid();
        Integer entityType = addForm.getResourceType();

        if (resourceId == null) {
            resourceId = RequestUtils.getResourceId(request);
        }

        if (entityType == null) {
            entityType = RequestUtils.getResourceTypeId(request);
        }

        //        AppdefResourceValue resource = RequestUtils.getHqResource(request);
        //        if (resource == null) {
        //            RequestUtils.setError(request, Constants.ERR_RESOURCE_NOT_FOUND);
        //            return null;
        //        }
        //
        //        AppdefEntityID appdefId = new AppdefEntityID(entityType.intValue(),
        //            resourceId.intValue());
        //        ServletContext ctx = getServlet().getServletContext();
        //        MeasurementBoss mBoss = ContextUtils.getMeasurementBoss(ctx);
        //        int sessionId = RequestUtils.getSessionId(request).intValue();
        //
        //        log.trace("Getting availability threshold metrics");

        // XXX actually set the availability and unavailability thresholds

        return null;
    }
}