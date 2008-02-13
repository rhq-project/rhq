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
import javax.servlet.http.HttpSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An action that adds users to an alert definition in the BizApp.
 */
public class AddUsersAction extends AddNotificationsAction {
    private Log log = LogFactory.getLog(AddUsersAction.class);

    @Override
    protected ActionForward process(ActionMapping mapping, AddNotificationsForm form, HttpServletRequest request,
        Map<String, Integer> params) throws Exception {
        AddUsersForm addForm = (AddUsersForm) form;
        ActionForward forward = checkSubmit(request, mapping, form, params);
        HttpSession session = request.getSession();
        Subject subject = RequestUtils.getSubject(request);

        if (forward != null) {
            if (addForm.isCancelClicked() || addForm.isResetClicked()) {
                log.debug("removing pending user list");
                SessionUtils.removeList(session, Constants.PENDING_USERS_SES_ATTR);
            } else if (addForm.isAddClicked()) {
                log.debug("adding to pending user list");
                SessionUtils.addToList(session, Constants.PENDING_USERS_SES_ATTR, addForm.getAvailableUsers());
                log.debug("@@@@@@@@@@" + addForm.getAvailableUsers().toString());
                for (int i = 0; i < addForm.getAvailableUsers().length; i++) {
                    log.debug("Avalilable Users " + addForm.getAvailableUsers()[i]);
                }
            } else if (addForm.isRemoveClicked()) {
                log.debug("removing from pending user list");
                SessionUtils.removeFromList(session, Constants.PENDING_USERS_SES_ATTR, addForm.getPendingUsers());
            }

            return forward;
        }

        log.debug("getting pending user list");
        Integer[] pendingUserIds = SessionUtils.getList(session, Constants.PENDING_USERS_SES_ATTR);
        for (int i = 0; i < pendingUserIds.length; i++) {
            log.debug("adding user [" + pendingUserIds[i] + "] for alert definition [" + addForm.getAd() + "]");
        }

        AlertNotificationManagerLocal alertNotificationManager = LookupUtil.getAlertNotificationManager();
        alertNotificationManager.addSubjectNotifications(subject, addForm.getAd(), pendingUserIds, addForm
            .isAlertTemplate());

        log.debug("remove pending user list");
        SessionUtils.removeList(session, Constants.PENDING_USERS_SES_ATTR);

        RequestUtils.setConfirmation(request, "alerts.config.confirm.AddRoles");
        return returnSuccess(request, mapping, params);
    }
}