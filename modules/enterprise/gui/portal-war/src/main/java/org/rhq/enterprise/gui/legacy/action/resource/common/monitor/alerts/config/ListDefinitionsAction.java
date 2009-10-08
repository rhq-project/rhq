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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * List all alert definitions for this entity.
 */
public class ListDefinitionsAction extends TilesAction {
    private Log log = LogFactory.getLog(ListDefinitionsAction.class.getName());

    /**
     * Retrieve this data and store it in request attributes.
     */
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.trace("in ListDefinitionAction");
        Subject subject = RequestUtils.getSubject(request);
        PageControl pc = WebUtility.getPageControl(request);

        AlertDefinitionManagerLocal alertManager = LookupUtil.getAlertDefinitionManager();
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

        int resourceId = RequestUtils.getResourceId(request);

        PageList<AlertDefinition> alertDefinitions = alertManager.findAlertDefinitions(subject, resourceId, pc);

        // TODO GH: Deal with when its by type... i guess for the template setup

        /*PageList uiBeans = new PageList();
         * for (Object alertDefObj : alertDefs) { AlertDefinition alertDef = (AlertDefinition) alertDefObj;
         * AlertDefinitionBean bean = new AlertDefinitionBean(alertDef.getId(),   alertDef.getCtime(),
         * alertDef.getName(), alertDef.getDescription(),   alertDef.getEnabled(), alertDef.getParentId());
         * bean.setAppdefEntityID(appEntId); uiBeans.add(bean);}*/

        context.putAttribute(Constants.HQ_RESOURCE_ATTR, resourceManager.getResourceById(subject, resourceId));
        request.setAttribute(Constants.ALERT_DEFS_ATTR, alertDefinitions);

        /*
         * context.putAttribute(Constants.RESOURCE_OWNER_ATTR, request.getAttribute(Constants.RESOURCE_OWNER_ATTR));
         * context.putAttribute(Constants.RESOURCE_MODIFIER_ATTR,
         * request.getAttribute(Constants.RESOURCE_MODIFIER_ATTR)); request.setAttribute(Constants.LIST_SIZE_ATTR,
         * alertDefs.getTotalSize());
         */
        return null;
    }
}

// EOF
