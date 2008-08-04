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

import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.AlertDefUtil;
import org.rhq.enterprise.gui.legacy.beans.AlertConditionBean;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.legacy.events.EventConstants;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * View an alert definition.
 */
public class ViewDefinitionAction extends TilesAction {
    private Log log = LogFactory.getLog(ViewDefinitionAction.class);

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Subject subject = RequestUtils.getSubject(request);

        // properties, also sets Request or RequestType request attribute
        AlertDefinition alertDef = AlertDefUtil.getAlertDefinition(request);

        // conditions
        Set<AlertCondition> conds = alertDef.getConditions();

        boolean isAlertTemplate = EventConstants.TYPE_ALERT_DEF_ID.equals(alertDef.getParentId());
        List<AlertConditionBean> alertDefConditions = AlertDefUtil.getAlertConditionBeanList(subject, request, conds,
            isAlertTemplate);
        request.setAttribute("conditionExpression", alertDef.getConditionExpression().name());
        request.setAttribute("alertDefConditions", alertDefConditions);

        request.setAttribute("controlEnabled", true); // TODO: disable if this resource doesn't have any operations facet

        int recoveryAlertDefId = alertDef.getRecoveryId();
        if (recoveryAlertDefId != 0) {
            String recoveryAlertName = getRecoveryAlertName(recoveryAlertDefId, subject);
            request.setAttribute("recoveryAlertName", recoveryAlertName);
        }

        if (alertDef.getOperationDefinition() != null) {
            request.setAttribute("controlAction", alertDef.getOperationDefinition().getDisplayName());
        }

        // enablement
        AlertDefUtil.setAlertDampeningRequestAttributes(request, alertDef);

        return null;
    }

    private String getRecoveryAlertName(int alertDefinitionId, Subject user) {
        try {
            AlertDefinition alertDefinition = LookupUtil.getAlertDefinitionManager().getAlertDefinitionById(user,
                alertDefinitionId);
            return alertDefinition.getName();
        } catch (Exception ex) {
            return null;
        }
    }
}