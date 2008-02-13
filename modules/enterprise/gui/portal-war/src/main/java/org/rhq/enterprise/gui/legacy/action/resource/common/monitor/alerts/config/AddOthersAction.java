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

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An action that adds other email addresses ( those that are not in CAM ) to an alert definition in the BizApp.
 */
public class AddOthersAction extends AddNotificationsAction {
    private Log log = LogFactory.getLog(AddOthersAction.class);

    @Override
    protected ActionForward process(ActionMapping mapping, AddNotificationsForm form, HttpServletRequest request,
        Map<String, Integer> params) throws Exception {
        checkSubmit(request, mapping, form, params);

        AddOthersForm addForm = (AddOthersForm) form;
        Subject subject = RequestUtils.getSubject(request);

        String[] emails = addForm.getEmailAddresses().split(",");

        AlertNotificationManagerLocal alertNotificationManager = LookupUtil.getAlertNotificationManager();
        alertNotificationManager.addEmailNotifications(subject, addForm.getAd(), emails, addForm.isAlertTemplate());

        RequestUtils.setConfirmation(request, "alerts.config.confirm.AddOthers");
        return returnSuccess(request, mapping, params);
    }
}