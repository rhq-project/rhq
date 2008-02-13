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
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.event.alert.AlertDefinition;
import org.rhq.core.domain.event.alert.notification.SnmpNotification;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.AlertDefUtil;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Prepare an {@link SnmpTrapForm} (SnmpActionProps.jsp).
 *
 * @author Ian Springer
 */
public class SnmpTrapFormPrepareAction extends BaseAction {
    private Log log = LogFactory.getLog(SnmpTrapFormPrepareAction.class);

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        SnmpTrapForm snmpTrapForm = (SnmpTrapForm) form;
        AlertDefinition alertDefinition = AlertDefUtil.getAlertDefinition(request);
        PageControl pageControl = WebUtility.getPageControl(request);
        SnmpNotification snmpNotification = getSnmpNotification(alertDefinition, pageControl);
        if (snmpNotification != null) {
            snmpTrapForm.setHost(snmpNotification.getHost());
            snmpTrapForm.setPort(snmpNotification.getPort());
            snmpTrapForm.setOid(snmpNotification.getOid());
        }

        return null;
    }

    @Nullable
    private SnmpNotification getSnmpNotification(AlertDefinition alertDefinition, PageControl pageControl) {
        SnmpNotification snmpNotification;
        AlertNotificationManagerLocal alertNotificationManager = LookupUtil.getAlertNotificationManager();
        PageList<SnmpNotification> snmpNotifications = alertNotificationManager.getSnmpNotifications(alertDefinition
            .getId(), pageControl);
        if (!snmpNotifications.isEmpty()) {
            if (snmpNotifications.size() > 1) {
                log.warn("More than one SNMP notification found for alert definition " + alertDefinition
                    + ", but the GUI currently only supports up to one SNMP notification per alert definition.");
            }

            snmpNotification = snmpNotifications.get(0);
        } else {
            snmpNotification = null;
        }

        return snmpNotification;
    }
}