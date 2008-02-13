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

package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.config;

/**
 * This action prepares the portal for configuring the 
 * monitoring pages.
 * 
 * @author Heiko W. Rupp
 */
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.Portal;

public class ConfigPortalAction extends ResourceConfigPortalAction {

    private static final String CONFIG_METRICS_PORTAL = ".resource.platform.monitor.config.ConfigMetrics";
    private static final String CONFIG_METRICS_TITLE = "resource.platform.monitor.visibility.config.ConfigureVisibility.Title";

    /* (non javadoc)
     * @see org.rhq.enterprise.gui.legacy.action.BaseDispatchAction#getKeyMethodMap()
     */
    @Override
    protected Properties getKeyMethodMap() {
        Properties map = new Properties();
        map.setProperty(ParamConstants.MODE_CONFIGURE, "configMetrics");
        map.setProperty(ParamConstants.MODE_LIST, "configMetrics");
        return map;
    }

    /** mode=configure || mode=view */
    @Override
    public ActionForward configMetrics(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        setResource(request); // TODO resource or type (the later )???

        super.configMetrics(mapping, form, request, response);

        Portal portal = Portal.createPortal(CONFIG_METRICS_TITLE, CONFIG_METRICS_PORTAL);
        request.setAttribute(AttrConstants.PORTAL_KEY, portal);
        return null;

    }

}
