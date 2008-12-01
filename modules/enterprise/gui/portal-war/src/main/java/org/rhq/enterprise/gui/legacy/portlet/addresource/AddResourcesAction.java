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
package org.rhq.enterprise.gui.legacy.portlet.addresource;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.action.BaseValidatorForm;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;

/**
 * An Action that adds resources to a dashboard widget Heavily based on:
 * org.rhq.enterprise.gui.admin.role.AddUserFormPrepareAction
 */
public class AddResourcesAction extends BaseAction {
    /**
     * Add resources to the user specified in the given <code>AddResourcesForm</code>.
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(AddResourcesAction.class);
        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);
        WebUserPreferences preferences = user.getPreferences();
        AddResourcesForm addForm = (AddResourcesForm) form;

        ActionForward forward = checkSubmit(request, mapping, form, Constants.USER_PARAM, user.getId());
        if (forward != null) {
            BaseValidatorForm spiderForm = (BaseValidatorForm) form;

            if (spiderForm.isCancelClicked() || spiderForm.isResetClicked()) {
                log.trace("removing pending resources list");
                SessionUtils.removeList(session, Constants.PENDING_RESOURCES_SES_ATTR);
            } else if (spiderForm.isAddClicked()) {
                log.trace("adding to pending resources list");
                SessionUtils.addToList(session, Constants.PENDING_RESOURCES_SES_ATTR, addForm.getAvailableResources());
            } else if (spiderForm.isRemoveClicked()) {
                log.trace("removing from pending resources list");
                SessionUtils.removeFromList(session, Constants.PENDING_RESOURCES_SES_ATTR, addForm
                    .getPendingResources());
            }

            return forward;
        }

        log.trace("getting pending resources list");
        List<String> pendingResourceIds = SessionUtils.getListAsListStr(request.getSession(),
            Constants.PENDING_RESOURCES_SES_ATTR);

        StringBuffer resourcesAsString = new StringBuffer();
        for (String pendingId : pendingResourceIds) {
            resourcesAsString.append("|");
            resourcesAsString.append(pendingId);
        }

        SessionUtils.removeList(session, Constants.PENDING_RESOURCES_SES_ATTR);

        RequestUtils.setConfirmation(request, "admin.user.confirm.AddResource");

        preferences.setPreference(addForm.getKey(), resourcesAsString);
        LogFactory.getLog("user.preferences").trace(
            "Invoking setUserPrefs" + " in " + getClass().getSimpleName() + " for " + user.getId() + " at "
                + System.currentTimeMillis() + " user.prefs = " + user.getPreferences());
        preferences.persistPreferences();

        return returnSuccess(request, mapping);
    }
}