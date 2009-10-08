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
package org.rhq.enterprise.gui.legacy.action.resource.group.inventory;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.util.MessageResources;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.WorkflowPrepareAction;
import org.rhq.enterprise.gui.legacy.util.BizappUtils;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 */
public class NewGroupFormPrepareAction extends WorkflowPrepareAction {
    /**
     * Retrieve this data and store it in the <code>ServerForm</code>:
     */
    @Override
    public ActionForward workflow(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        //Log log = LogFactory.getLog(NewGroupFormPrepareAction.class);

        GroupForm newForm = (GroupForm) form;

        MessageResources res = getResources(request);

        Subject subject = RequestUtils.getSubject(request);

        ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
        HttpSession session = request.getSession();

        Integer[] resourceIds = (Integer[]) session.getAttribute(Constants.RESOURCE_IDS_ATTR);
        if (resourceIds != null) {
            newForm.setResourceIds(resourceIds);

            boolean commonResourceType = false;
            Integer resourceTypeId = (Integer) session.getAttribute(Constants.RESOURCE_TYPE_ATTR);

            if (resourceTypeId != null) {
                if (resourceTypeManager.ensureResourceType(subject, resourceTypeId, resourceIds) == false) {
                    ResourceType compatibleResourceType = resourceTypeManager.getResourceTypeById(subject,
                        resourceTypeId);

                    newForm.setResourceTypeName(compatibleResourceType.getName());
                    newForm.setResourceTypeId(resourceTypeId);
                    newForm.setGroupCategory(GroupCategory.COMPATIBLE);

                    return null;
                }
            } else {
                // TODO: do we need to set the name for mixed groups anymore?
                newForm.setResourceTypeName(res.getMessage("dash.home.DisplayCategory.group.plat.server.service"));
                newForm.setGroupCategory(GroupCategory.MIXED);

                return null;
            }
        }

        /*
         * We effectively want PageControl.PAGE_ALL here, so don't even bother having a method that takes one
         */
        // TODO: Why not turn this into a single select, and simplify the display by ordering the results?
        List<ResourceType> platformTypes = resourceTypeManager.getUtilizedResourceTypesByCategory(subject,
            ResourceCategory.PLATFORM, null);
        List<ResourceType> serverTypes = resourceTypeManager.getUtilizedResourceTypesByCategory(subject,
            ResourceCategory.SERVER, null);
        List<ResourceType> serviceTypes = resourceTypeManager.getUtilizedResourceTypesByCategory(subject,
            ResourceCategory.SERVICE, null);
        List groupTypes = BizappUtils.buildGroupTypes(request);

        newForm.setPlatformTypes(platformTypes);
        newForm.setServerTypes(serverTypes);
        newForm.setServiceTypes(serviceTypes);
        newForm.setGroupTypes(groupTypes);

        return null;
    }
}