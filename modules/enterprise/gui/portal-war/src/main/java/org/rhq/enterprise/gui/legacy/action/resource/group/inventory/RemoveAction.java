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
/*
 * Created on Feb 14, 2003
 *
 */
package org.rhq.enterprise.gui.legacy.action.resource.group.inventory;

import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.legacy.HubConstants;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupNotFoundException;
import org.rhq.enterprise.server.resource.group.ResourceGroupUpdateException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 */
public class RemoveAction extends BaseAction {
    /**
     * Removes a application identified by the value of the request parameter <code>Constants.RESOURCE_PARAM</code> from
     * the BizApp.
     *
     * @return
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(RemoveAction.class);

        RemoveGroupResourcesForm removeForm = (RemoveGroupResourcesForm) form;

        Integer groupId = removeForm.getGroupId();
        String category = removeForm.getCategory();
        Subject subject = RequestUtils.getSubject(request);

        HashMap<String, Object> forwardParams = new HashMap<String, Object>(2);
        forwardParams.put(HubConstants.PARAM_GROUP_ID, groupId);
        forwardParams.put(HubConstants.PARAM_GROUP_CATEGORY, category);

        try {
            String[] resourceIdStrings = removeForm.getResources();

            if ((resourceIdStrings == null) || (resourceIdStrings.length == 0)) {
                return returnSuccess(request, mapping, forwardParams);
            }

            Integer[] resourceIds = new Integer[resourceIdStrings.length];
            for (int i = 0; i < resourceIds.length; i++) {
                resourceIds[i] = Integer.valueOf(resourceIdStrings[i]);
            }

            ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();
            groupManager.removeResourcesFromGroup(subject, groupId, resourceIds);

            return returnSuccess(request, mapping, forwardParams);
        } catch (ResourceGroupUpdateException rgue) {
            log.debug("group update failed:", rgue);
            RequestUtils.setError(request, "resource.group.inventory.error.GroupUpdateError", rgue.getMessage());
            return returnFailure(request, mapping, forwardParams);
        } catch (ResourceGroupNotFoundException raee) {
            log.debug("group update failed:", raee);
            RequestUtils.setError(request, "resource.group.inventory.error.GroupNotFound");
            return returnFailure(request, mapping, forwardParams);
        }
    }
}