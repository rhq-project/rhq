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

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.event.alert.AlertDefinition;
import org.rhq.core.domain.event.alert.notification.SnmpNotification;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.AlertDefUtil;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Process a submitted {@link SnmpTrapForm} (SnmpActionProps.jsp).
 *
 * @author Ian Springer
 */
public class SnmpTrapFormAction extends BaseAction {
    private Log log = LogFactory.getLog(SnmpTrapFormAction.class);

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        SnmpTrapForm snmpTrapForm = (SnmpTrapForm) form;
        AlertDefinition alertDefinition = AlertDefUtil.getAlertDefinition(request);
        PageControl pageControl = WebUtility.getPageControl(request);
        Subject subject = RequestUtils.getSubject(request);
        AlertNotificationManagerLocal alertNotificationManager = LookupUtil.getAlertNotificationManager();
        if (snmpTrapForm.isDeleteClicked()) {
            SnmpNotification snmpNotification = getExistingSnmpNotification(alertDefinition, pageControl);
            if (snmpNotification != null) {
                alertNotificationManager.removeNotifications(subject, alertDefinition.getId(),
                    new Integer[] { snmpNotification.getId() }, snmpTrapForm.isAlertTemplate());
            }
        } else // SET button clicked
        {
            SnmpNotification snmpNotification = new SnmpNotification(alertDefinition, snmpTrapForm.getHost(),
                snmpTrapForm.getPort(), snmpTrapForm.getOid());

            // NOTE: We currently only support associating a single SNMP notification with an alert definition,
            //       so the below manager method will delete any existing SNMP notifs and then add the new one.
            alertNotificationManager.setSnmpNotification(subject, alertDefinition.getId(), snmpNotification,
                snmpTrapForm.isAlertTemplate());
        }

        Map<String, Integer> requestParams = createRequestParamsMap(snmpTrapForm);
        return returnSuccess(request, mapping, requestParams);
    }

    @Nullable
    private SnmpNotification getExistingSnmpNotification(AlertDefinition alertDefinition, PageControl pageControl) {
        SnmpNotification snmpNotification;
        AlertNotificationManagerLocal alertNotificationManager = LookupUtil.getAlertNotificationManager();
        PageList<SnmpNotification> snmpNotifications = alertNotificationManager.getSnmpNotifications(alertDefinition
            .getId(), pageControl);
        if (!snmpNotifications.isEmpty()) {
            if (snmpNotifications.size() > 1) {
                log.error("More than one SNMP notification found for alert definition " + alertDefinition
                    + ", but the GUI currently only supports up to one SNMP notification per alert definition.");
            }

            snmpNotification = snmpNotifications.get(0);
        } else {
            snmpNotification = null;
        }

        return snmpNotification;
    }

    private Map<String, Integer> createRequestParamsMap(SnmpTrapForm snmpTrapForm) {
        Map<String, Integer> params = new HashMap<String, Integer>();
        params.put(Constants.ALERT_DEFINITION_PARAM, snmpTrapForm.getAd());
        params.put(Constants.RESOURCE_ID_PARAM, snmpTrapForm.getId());
        return params;
    }
}