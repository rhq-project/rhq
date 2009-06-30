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
package org.rhq.enterprise.gui.legacy.action.resource.hub;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.exception.DeleteException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupNotFoundException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Removes resources in ResourceHub
 */
public class RemoveGroupAction extends BaseAction {
    protected Log log = LogFactory.getLog(RemoveGroupAction.class);

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        GroupHubForm hubForm = (GroupHubForm) form;

        Subject subject = RequestUtils.getSubject(request);
        ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();

        String[] resources = hubForm.getResources();
        int remaining = resources.length;

        String invalidGroupIds = "";
        String invalidIntegers = "";
        String errorGroupIds = "";

        for (String item : resources) {
            try {
                groupManager.deleteResourceGroup(subject, new Integer(item));
                remaining--;
            } catch (NumberFormatException nfe) {
                invalidIntegers = add(invalidIntegers, item);
            } catch (DeleteException de) {
                if (de.getCause() instanceof ResourceGroupNotFoundException) {
                    invalidGroupIds = add(invalidGroupIds, item);
                } else {
                    errorGroupIds = add(errorGroupIds, item);
                }
            }
        }

        if (remaining == 0) {
            RequestUtils.setConfirmation(request, "resource.common.confirm.ResourceGroupsRemoved");
            return returnSuccess(request, mapping);
        } else {
            ActionErrors errors = new ActionErrors();
            if (invalidGroupIds.length() > 0) {
                errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("resource.common.error.InvalidGroupIds",
                    invalidGroupIds));
            }

            if (invalidIntegers.length() > 0) {
                errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("resource.common.error.InvalidIntegers",
                    invalidIntegers));
            }

            if (errorGroupIds.length() > 0) {
                errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("resource.common.error.ErrorGroupIds",
                    errorGroupIds));
            }

            RequestUtils.setErrors(request, errors);
            return returnFailure(request, mapping);
        }
    }

    private String add(String base, String addition) {
        if (base.length() != 0) {
            base += ", ";
        }

        return base + "'" + addition + "'";
    }
}