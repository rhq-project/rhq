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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.MetricViewsPreferences;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;

/**
 * An <code>TilesAction</code> that retrieves metric data to facilitate display of a current health page. Input is
 * either a single resource (param 'id'), a group (param 'groupId') or an autogroup (params 'type' and 'parent')
 */
public class CurrentHealthAction extends TilesAction {

    protected static Log log = LogFactory.getLog(CurrentHealthAction.class);

    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        Resource resource = (Resource) request.getAttribute(AttrConstants.RESOURCE_ATTR);
        Integer groupId = (Integer) request.getAttribute(AttrConstants.GROUP_ID);
        int parent = WebUtility.getOptionalIntRequestParameter(request, "parent", -1);
        int type = WebUtility.getOptionalIntRequestParameter(request, "type", -1);

        IndicatorViewsForm ivf = (IndicatorViewsForm) form;
        String key;
        if (resource != null) {
            key = String.valueOf((resource.getId()));
        } else if (groupId != null) {
            key = "cg=" + String.valueOf(groupId);
        } else if ((type > 0) && (parent > 0)) {
            ivf.setCtype(type);
            ivf.setParent(parent);
            key = "ag=" + parent + ":" + type;
        } else {
            throw new IllegalStateException("Unknown or unsupported IndicatorViewsForm mode '" + ivf + "'");
        }

        setupViews(request, ivf, key);

        return null;
    }

    /**
     * Set up the {@link IndicatorViewsForm#getViews()} list of views for the current indicator charts page. The key
     * generated here needs to correspond to the one in
     * {@link IndicatorChartsAction#generateSessionKey(IndicatorViewsForm form, true)}
     */
    protected void setupViews(HttpServletRequest request, IndicatorViewsForm ivf, String key) {
        WebUser user = SessionUtils.getWebUser(request.getSession());
        WebUserPreferences preferences = user.getPreferences();

        MetricViewsPreferences metricViews = preferences.getMetricViews(key);
        ivf.setViews(metricViews.views.toArray(new String[metricViews.views.size()]));

        String viewName = RequestUtils.getStringParameter(request, ParamConstants.PARAM_VIEW, metricViews.views.get(0));
        ivf.setView(viewName);
    }
}