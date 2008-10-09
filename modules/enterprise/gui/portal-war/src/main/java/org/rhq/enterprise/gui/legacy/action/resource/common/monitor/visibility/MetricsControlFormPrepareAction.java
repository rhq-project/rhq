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

import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.util.MonitorUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;

/**
 * A base class for <code>Action</code>s that prepare pages containing the metrics control form.
 */
public class MetricsControlFormPrepareAction extends TilesAction {
    protected static Log log = LogFactory.getLog(MetricsControlFormPrepareAction.class.getName());

    // ---------------------------------------------------- Public Methods

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        MetricsControlForm controlForm = (MetricsControlForm) form;
        int groupId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.GROUP_ID_PARAM, -1);
        int parent = WebUtility.getOptionalIntRequestParameter(request, "parent", -1);
        int type = WebUtility.getOptionalIntRequestParameter(request, "type", -1);
        if (parent > -1 && type > 1) {
            // autogroup
            controlForm.setParent(parent);
            controlForm.setCtype(type);
            controlForm.setCategory("Autogroup");

        } else if (groupId > -1) {
            ResourceType resourceType = (ResourceType) request.getAttribute(AttrConstants.RESOURCE_TYPE_ATTR);
            if (resourceType != null) {
                controlForm.setCategory(GroupCategory.COMPATIBLE.getName());
                controlForm.setGroupId(groupId);
            }

        } else {
            Resource resource = (Resource) request.getAttribute(AttrConstants.RESOURCE_ATTR);
            if (resource != null) {

                controlForm.setId(resource.getId());
                controlForm.setCategory(resource.getResourceType().getCategory().name());
            }
        }

        prepareForm(request, controlForm);
        return null;
    }

    // ---------------------------------------------------- Protected Methods

    protected void prepareForm(HttpServletRequest request, MetricsControlForm form, MetricRange range)
        throws IllegalArgumentException {
        WebUser user = SessionUtils.getWebUser(request.getSession());

        // set metric range defaults
        Map pref = user.getMetricRangePreference(true);
        form.setReadOnly((Boolean) pref.get(MonitorUtils.RO));
        form.setRn((Integer) pref.get(MonitorUtils.LASTN));
        form.setRu((Integer) pref.get(MonitorUtils.UNIT));

        Long begin;
        Long end;

        if (range != null) {
            begin = range.getBegin();
            end = range.getEnd();
        } else {
            begin = (Long) pref.get(MonitorUtils.BEGIN);
            end = (Long) pref.get(MonitorUtils.END);
        }

        form.setRb(begin);
        form.setRe(end);

        form.populateStartDate(new Date(begin), request.getLocale());
        form.populateEndDate(new Date(end), request.getLocale());
    }

    protected void prepareForm(HttpServletRequest request, MetricsControlForm form) throws IllegalArgumentException {
        prepareForm(request, form, null);
    }
}