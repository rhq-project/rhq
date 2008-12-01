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
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;

/**
 * An Action that retrieves a specific user from the BizApp to facilitate display of the <em>Edit User</em> form.
 */
public class EditUserFormPrepareAction extends TilesAction {
    // ---------------------------------------------------- Public Methods

    /**
     * Retrieve User data and store it in the specified request parameters.
     */
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(EditUserFormPrepareAction.class.getName());
        EditForm userForm = (EditForm) form;

        WebUser user = (WebUser) request.getAttribute(Constants.USER_ATTR);

        if (userForm.getFirstName() == null) {
            userForm.setFirstName(user.getFirstName());
        }

        if (userForm.getLastName() == null) {
            userForm.setLastName(user.getLastName());
        }

        if (userForm.getDepartment() == null) {
            userForm.setDepartment(user.getDepartment());
        }

        if (userForm.getName() == null) {
            userForm.setName(user.getName());
        }

        if (userForm.getEmailAddress() == null) {
            userForm.setEmailAddress(user.getEmailAddress());
        }

        if (userForm.getPhoneNumber() == null) {
            userForm.setPhoneNumber(user.getPhoneNumber());
        }

        if (userForm.getSmsAddress() == null) {
            userForm.setSmsAddress(user.getSmsaddress());
        }

        setupMyPreferences(request, user, userForm);

        if (user.getActive()) {
            userForm.setEnableLogin("yes");
        } else {
            userForm.setEnableLogin("no");
        }

        return null;
    }

    /**
     * Setup userForm to correctly hold preferences for this user, if the user who is logged in is editing their user
     *
     * @param request
     * @param userBeingEdited
     * @param userForm
     */
    private void setupMyPreferences(HttpServletRequest request, WebUser userBeingEdited, EditForm userForm) {
        HttpSession session = request.getSession();
        WebUser currentUser = SessionUtils.getWebUser(session);

        //Integer currentUserId = RequestUtils.getUserId(request);
        Integer currentUserId = currentUser.getId();
        Integer idOfUserBeingEdited = userBeingEdited.getId();

        // if the user being edited is the person who is logged in then setup these
        // preferences for display
        if ((currentUserId != null) && currentUserId.equals(idOfUserBeingEdited)) {
            userForm.setEditingCurrentUser(true);

            // if we've already got sthg set here, don't overwrite it
            if (userForm.getPageRefreshPeriod() == null) {
                int pageRefreshPeriod = currentUser.getPreferences().getPageRefreshPeriod();

                userForm.setPageRefreshPeriod(String.valueOf(pageRefreshPeriod));
            }
        }
    }
}