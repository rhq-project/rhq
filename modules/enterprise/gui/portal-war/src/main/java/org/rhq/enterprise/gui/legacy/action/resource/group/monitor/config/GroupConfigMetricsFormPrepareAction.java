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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.config.ConfigMetricsFormPrepareAction;

/**
 * This populates the GroupConfigMetrics/Update metrics pages' request attributes.
 */
public class GroupConfigMetricsFormPrepareAction extends ConfigMetricsFormPrepareAction {
    /**
     * Retrieve different resource metrics and store them in various request attributes.
     */
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(GroupConfigMetricsFormPrepareAction.class.getName());
        log.trace("Preparing group resource metrics action.");

        ActionForward fwd = super.execute(context, mapping, form, request, response);

        if (fwd != null) {
            return null;
        }

        // XXX group specific prepare actions here

        log.debug("Successfully completed preparing Group Config Metrics");

        return null;
    }
}