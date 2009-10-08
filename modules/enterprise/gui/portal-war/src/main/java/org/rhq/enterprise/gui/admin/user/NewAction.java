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

import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An <code>WorkflowAction</code> subclass that creates a user in the BizApp.
 */
public class NewAction extends BaseAction {
    // ---------------------------------------------------- Public Methods

    /**
     * Create the user with the attributes specified in the given <code>NewForm</code> and save it into the session
     * attribute <code>Constants.USER_ATTR</code>.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(NewAction.class.getName());

        Subject whoami = RequestUtils.getWebUser(request).getSubject();
        NewForm userForm = (NewForm) form;

        ActionForward forward = checkSubmit(request, mapping, form);
        if (forward != null) {
            return forward;
        }

        Subject user = new Subject();

        user.setName(userForm.getName());
        user.setFirstName(userForm.getFirstName());
        user.setLastName(userForm.getLastName());
        user.setDepartment(userForm.getDepartment());
        user.setEmailAddress(userForm.getEmailAddress());
        user.setSmsAddress(userForm.getSmsAddress());
        user.setPhoneNumber(userForm.getPhoneNumber());
        user.setFactive(!userForm.getEnableLogin().equals("no"));
        user.setFsystem(false);

        // add both a subject and a principal as normal
        log.debug("creating subject [" + user.getName() + "]");

        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        Subject newUser = subjectManager.createSubject(whoami, user);

        log.debug("creating principal [" + user.getName() + "]");
        subjectManager.createPrincipal(whoami, user.getName(), userForm.getNewPassword());

        HashMap parms = new HashMap(1);
        parms.put(Constants.USER_PARAM, newUser.getId());

        return returnOkAssign(request, mapping, parms, false);
    }
}