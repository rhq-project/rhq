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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.util.MeasurementConverter;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.beans.AlertConditionBean;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.legacy.events.EventConstants;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * View an alert.
 */
public class ViewAlertAction extends TilesAction {
    private Log log = LogFactory.getLog(ViewAlertAction.class);

    /**
     * Retrieve this data and store it in request attributes.
     */
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        Subject subject = RequestUtils.getSubject(request);
        AlertManagerLocal alertManager = LookupUtil.getAlertManager();

        // pass-through the alertId
        Integer alertId = new Integer(request.getParameter("a"));
        log.trace("alertId=" + alertId);
        request.setAttribute("a", alertId);

        // properties
        Alert av = alertManager.getById(alertId);
        AlertDefinition adv = av.getAlertDefinition();
        request.setAttribute("alert", av);
        request.setAttribute(Constants.ALERT_DEFINITION_ATTR, adv);

        // conditions
        Set<AlertConditionLog> condLogs = av.getConditionLogs();
        Set<AlertCondition> conds = new LinkedHashSet<AlertCondition>(condLogs.size());

        for (AlertConditionLog condLog : condLogs) {
            conds.add(condLog.getCondition());
        }

        List<AlertConditionBean> alertCondBeans = AlertDefUtil.getAlertConditionBeanList(subject, request, conds,
            EventConstants.TYPE_ALERT_DEF_ID.equals(adv.getParentId()));

        Iterator<AlertCondition> condsIterator = conds.iterator();
        Iterator<AlertConditionLog> condLogsIterator = condLogs.iterator();

        for (AlertConditionBean alertCondBean : alertCondBeans) {
            AlertCondition cond = condsIterator.next();
            AlertConditionLog condLog = condLogsIterator.next();
            AlertConditionCategory category = cond.getCategory();

            if (category == AlertConditionCategory.CONTROL) {
                alertCondBean.setActualValue(RequestUtils.message(request, "alert.current.list.ControlActualValue"));
            } else if ((category == AlertConditionCategory.THRESHOLD) || (category == AlertConditionCategory.BASELINE)
                || (category == AlertConditionCategory.CHANGE)) {

                // Format threshold and value.
                MeasurementDefinition definition = condLog.getCondition().getMeasurementDefinition();
                String firedValue;

                try {
                    firedValue = MeasurementConverter.format(Double.valueOf(condLog.getValue()), definition.getUnits(),
                        true);
                } catch (Exception e) {
                    firedValue = "??";
                }

                alertCondBean.setActualValue(firedValue);
            } else if ((category == AlertConditionCategory.CONFIGURATION_PROPERTY)
                || (category == AlertConditionCategory.EVENT)) {
                // TODO: jmarques - add validation to make sure condition is a valid regex Pattern
                alertCondBean.setActualValue(condLog.getValue());
            } else {
                alertCondBean.setActualValue("??");
            }
        }

        request.setAttribute("alertDefConditions", alertCondBeans);

        request.setAttribute("controlEnabled", true); // always display control result, even if it's "none"
        if (av.getTriggeredOperation() != null) {
            request.setAttribute("controlAction", av.getTriggeredOperation());
        }

        // enablement
        AlertDefUtil.setAlertDampeningRequestAttributes(request, adv);

        return null;
    }
}