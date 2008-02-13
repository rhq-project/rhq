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

import java.util.ArrayList;
import java.util.List;
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
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Removes resources from inventory.
 */
public class RemoveResourceAction extends BaseAction {
    protected Log log = LogFactory.getLog(RemoveResourceAction.class);

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Subject subject = RequestUtils.getSubject(request);
        ResourceHubForm hubForm = (ResourceHubForm) form;
        String[] resourceIdStrings = hubForm.getResources();

        String invalidResourceIds = "";
        String invalidIntegerStrings = "";
        List<Integer> doomedResourceIds = new ArrayList<Integer>();

        // convert all the ID strings to integers and add them to our doomed array
        for (String resourceIdString : resourceIdStrings) {
            try {
                doomedResourceIds.add(Integer.valueOf(resourceIdString));
            } catch (NumberFormatException nfe) {
                invalidIntegerStrings = add(invalidIntegerStrings, resourceIdString);
            }
        }

        // ask the server to delete the doomed resources
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        List<Integer> deletedResourceIds = resourceManager.deleteResources(subject, doomedResourceIds
            .toArray(new Integer[0]));

        // a doomed resource is one that we asked to be deleted.
        // a deleted resource is one that was actually deleted.
        // if a doomed resource ID is not found in the list of deleted resources, it was probably a non-existing resource
        for (Integer doomedResourceId : doomedResourceIds) {
            if (!deletedResourceIds.contains(doomedResourceId)) {
                invalidResourceIds = add(invalidResourceIds, doomedResourceId.toString());
            }
        }

        if ((invalidResourceIds.length() == 0) && (invalidIntegerStrings.length() == 0)) {
            RequestUtils.setConfirmation(request, "resource.common.confirm.ResourcesRemoved");
        } else {
            ActionErrors errors = new ActionErrors();
            if (invalidResourceIds.length() > 0) {
                errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("resource.common.error.InvalidResourceIds",
                    invalidResourceIds));
            }

            if (invalidIntegerStrings.length() > 0) {
                errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("resource.common.error.InvalidIntegers",
                    invalidIntegerStrings));
            }

            RequestUtils.setErrors(request, errors);
            return returnFailure(request, mapping);
        }

        return returnSuccess(request, mapping);
    }

    private String add(String base, String addition) {
        if (base.length() != 0) {
            base += ", ";
        }

        return base + "'" + addition + "'";
    }
}