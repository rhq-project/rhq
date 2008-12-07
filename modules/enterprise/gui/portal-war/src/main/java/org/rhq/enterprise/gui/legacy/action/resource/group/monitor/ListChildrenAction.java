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
package org.rhq.enterprise.gui.legacy.action.resource.group.monitor;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.MetricRangePreferences;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.uibeans.CompGroupCompositeDisplaySummary;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplaySummary;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Action for the list of child resources. Creates input for group.monitor.visibility.ListChildResources.jsp
 *
 * @author Heiko W. Rupp
 */
public class ListChildrenAction extends TilesAction {
    private Log log = LogFactory.getLog(ListChildrenAction.class);

    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        int groupId = WebUtility.getOptionalIntRequestParameter(request, "groupId", -1);

        WebUser user = SessionUtils.getWebUser(request.getSession());
        WebUserPreferences preferences = user.getPreferences();
        Subject subject = user.getSubject();

        MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();
        long begin = rangePreferences.begin;
        long end = rangePreferences.end;

        ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        MeasurementDataManagerLocal metricsMgr = LookupUtil.getMeasurementDataManager();

        ResourceGroup group = groupManager.getResourceGroupById(subject, groupId, null);
        PageList<ResourceWithAvailability> resources = resourceManager
            .getImplicitResourceWithAvailabilityByResourceGroup(subject, group, new PageControl());

        Map<Integer, List<MetricDisplaySummary>> meDis = metricsMgr.getNarrowedMetricsDisplaySummaryForCompGroup(
            subject, group, begin, end);

        CompGroupCompositeDisplaySummary summaries = new CompGroupCompositeDisplaySummary(resources, meDis);
        context.putAttribute(AttrConstants.CTX_SUMMARIES, summaries);
        return null;
    }
}