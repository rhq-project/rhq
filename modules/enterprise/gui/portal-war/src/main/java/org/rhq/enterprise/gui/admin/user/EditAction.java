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
package org.rhq.enterprise.gui.admin.user;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Edits the user's (aka {@link Subject}) information.
 */
public class EditAction extends BaseAction {
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(EditAction.class.getName());
        log.trace("modifying user properties action");

        EditForm userForm = (EditForm) form;
        ActionForward forward = checkSubmit(request, mapping, form, Constants.USER_PARAM, userForm.getId());

        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        Subject user = subjectManager.loadUserConfiguration(userForm.getId());

        if (forward != null) {
            request.setAttribute(Constants.USER_ATTR, user);
            return forward;
        }

        user.setFirstName(userForm.getFirstName());
        user.setLastName(userForm.getLastName());
        user.setDepartment(userForm.getDepartment());
        user.setName(userForm.getName());
        user.setEmailAddress(userForm.getEmailAddress());
        user.setPhoneNumber(userForm.getPhoneNumber());
        user.setSmsAddress(userForm.getSmsAddress());
        user.setFactive(userForm.getEnableLogin().equals("yes"));

        // a user can only edit his own configuration
        WebUser currentUser = RequestUtils.getWebUser(request);
        if (currentUser.getId().equals(userForm.getId())) {
            // update the in-memory preferences of the webuser so it takes effect for this session
            try {
                Integer pageRefreshPeriod = Integer.valueOf(userForm.getPageRefreshPeriod());
                WebUserPreferences preferences = currentUser.getWebPreferences();
                preferences.setPageRefreshPeriod(pageRefreshPeriod);
                preferences.persistPreferences();
            } catch (NumberFormatException e) {
                throw new RuntimeException(
                    "pageRefreshPeriod is not an integer, this should have been caught earlier by the form validation.");
            }
        }

        subjectManager.updateSubject(currentUser.getSubject(), user);

        return returnSuccess(request, mapping, Constants.USER_PARAM, userForm.getId());
    }
}