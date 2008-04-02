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
package org.rhq.enterprise.gui.legacy.portlet.search;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.util.LabelValueBean;

import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.util.StopWatch;
import org.rhq.enterprise.gui.legacy.action.resource.hub.ResourceHubForm;

/**
 * An <code>TilesAction</code> that sets up for searching the Resource Hub portal.
 */
public class ViewAction extends TilesAction {
    // ---------------------------------------------------- Public Methods

    /**
     * Set up the Resource Hub portal.
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        StopWatch timer = new StopWatch();
        Log timingLog = LogFactory.getLog("DASHBOARD-TIMING");

        Log log = LogFactory.getLog(ViewAction.class.getName());
        ResourceHubForm hubForm = (ResourceHubForm) form;

        for (ResourceCategory category : ResourceCategory.values()) {
            hubForm.addFunction(new LabelValueBean(category.name(), category.name()));
        }

        timingLog.trace("SearchHubPrepare - timing [" + timer.toString() + "]");
        return null;
    }
}