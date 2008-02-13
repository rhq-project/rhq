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

import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.util.MessageResources;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.KeyConstants;
import org.rhq.enterprise.gui.legacy.MessageConstants;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.util.DashboardUtils;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;

/**
 * An <code>TilesAction</code> that retrieves metric data to facilitate display of a current health page. Input is
 * either a single resource (param 'id'), a group (param 'groupId') or an autogroup (params 'type' and 'parent')
 */
public class CurrentHealthAction extends TilesAction {
    private static final String EWWW = "ewww";

    protected static Log log = LogFactory.getLog(CurrentHealthAction.class);

    private String defaultView = null;

    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        Integer groupId = null;
        Resource resource = (Resource) request.getAttribute(AttrConstants.RESOURCE_ATTR);
        groupId = (Integer) request.getAttribute(AttrConstants.GROUP_ID);
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
            RequestUtils.setError(request, MessageConstants.ERR_RESOURCE_NOT_FOUND);
            log.warn("Unknown ");
            key = EWWW;
        }

        setupViews(request, ivf, key);

        return null;
    }

    protected String getDefaultViewName(HttpServletRequest request) {
        if (defaultView != null) {
            return defaultView;
        }

        MessageResources res = getResources(request);
        return res.getMessage(KeyConstants.DEFAULT_INDICATOR_VIEW);
    }

    /**
     * Set up the {@link IndicatorViewsForm#getViews()} list of views for the current indicator charts page. The key
     * generated here needs to correspond to the one in
     * {@link IndicatorChartsAction#generateSessionKey(IndicatorViewsForm form, true)}
     */
    protected void setupViews(HttpServletRequest request, IndicatorViewsForm ivf, String key) {
        WebUser user = SessionUtils.getWebUser(request.getSession());

        String[] views;

        try {
            String viewsPref = user.getPreference(KeyConstants.INDICATOR_VIEWS + key);
            StringTokenizer st = new StringTokenizer(viewsPref, DashboardUtils.DASHBOARD_DELIMITER);

            views = new String[st.countTokens()];
            for (int i = 0; st.hasMoreTokens(); i++) {
                views[i] = st.nextToken();
            }
        } catch (IllegalArgumentException e) {
            views = new String[] { getDefaultViewName(request) };
        }

        ivf.setViews(views);

        String viewName = RequestUtils.getStringParameter(request, ParamConstants.PARAM_VIEW, views[0]);

        // Make sure that the view name is one of the views
        boolean validated = false;
        for (int i = 0; i < views.length; i++) {
            if (viewName.equals(views[i])) {
                validated = true;
                break;
            }
        }

        if (!validated) {
            viewName = views[0];
        }

        ivf.setView(viewName);
    }
}