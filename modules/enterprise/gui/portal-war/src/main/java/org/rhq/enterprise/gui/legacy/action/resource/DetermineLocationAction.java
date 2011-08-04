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
package org.rhq.enterprise.gui.legacy.action.resource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.util.WebUtility;

/**
 * An <code>Action</code> that forwards a request to view a resource or group on to the appropriate default action.
 *
 * @author Ian Springer
 */
public class DetermineLocationAction extends BaseAction {
    /**
     * Determines what default resource or group page to go to.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        String type = WebUtility
            .getOptionalRequestParameter(request, ParamConstants.RESOURCE_TYPE_ID_PARAM, "RESOURCE");
        String forwardConfigName;
        if (type.equals("RESOURCE")) {
            forwardConfigName = "resource";
            // Does this need to be broken out for P/S/S?
        } else if (type.equals("GROUP")) {
            String category = RequestUtils.getStringParameter(request, "category").toLowerCase();
            Integer id = RequestUtils.getGroupId(request);
            if (id != null) {
                if (category.equals("compatible")) {
                    forwardConfigName = "compatGroup";
                } else if (category.equals("mixed")) {
                    forwardConfigName = "adhocGroup";
                } else {
                    throw new RuntimeException("Unknown groupCategory '" + category
                        + "' was passed to DetermineLocationAction");
                }
            } else {
                // No group id means it's an autogroup.
                forwardConfigName = "autoGroup";
            }
        } else {
            throw new IllegalArgumentException("Unknown type '" + type + "' was passed to DetermineLocationAction");
        }

        //        if (!LookupUtil.getSystemManager().isMonitoringEnabled()) {
        //            forwardConfigName += "-inventory";
        //        }

        return mapping.findForward(forwardConfigName);
    }
}