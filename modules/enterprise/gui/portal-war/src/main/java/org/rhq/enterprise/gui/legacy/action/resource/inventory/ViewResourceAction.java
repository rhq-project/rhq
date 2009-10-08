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
package org.rhq.enterprise.gui.legacy.action.resource.inventory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An Action that retrieves data from the previously fetched resource to facilitate display of that resource.
 *
 * @author Ian Springer
 */
public class ViewResourceAction extends TilesAction {

    private final Log log = LogFactory.getLog(ViewResourceAction.class);

    /**
     * Retrieve this data and store it in request attributes:
     * <p/>
     * <ul> <li><code>Collection</code> of servers for the platform as <code>Constants.CHILD_RESOURCES_ATTR</code>
     * <li>number of servers for the platform as <code>Constants.NUM_CHILD_RESOURCES_ATTR</code> <li>map of server counts
     * by server type for the platform as <code>Constants.RESOURCE_TYPE_MAP_ATTR</code> </ul>
     */
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        Resource resource = (Resource) request.getAttribute(AttrConstants.RESOURCE_ATTR);

        if (resource == null) {
            RequestUtils.setError(request, Constants.ERR_PLATFORM_NOT_FOUND); // TODO
            return null;
        }

        log.info("Viewing resource " + resource + "...");

        WebUser webUser = SessionUtils.getWebUser(request.getSession());
        Subject subject = webUser.getSubject();

        try {
            ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

            PageList<Resource> childServers = resourceManager.findChildResourcesByCategoryAndInventoryStatus(subject,
                resource, ResourceCategory.SERVER, InventoryStatus.COMMITTED, PageControl.getUnlimitedInstance());
            request.setAttribute(Constants.CHILD_SERVERS_ATTR, childServers);

            PageList<Resource> childServices = resourceManager.findChildResourcesByCategoryAndInventoryStatus(subject,
                resource, ResourceCategory.SERVICE, InventoryStatus.COMMITTED, PageControl.getUnlimitedInstance());
            request.setAttribute(Constants.CHILD_SERVICES_ATTR, childServices);

            // TODO: Get list of all groups that contain this resource.
            // TODO: Construct a serverType -> serverList map.
            // TODO: Construct a serviceType -> serviceList map.

            //         RemoveServersForm rmServersForm = new RemoveServersForm();
            //         int fs = RequestUtils.getPageSize(request, "fs");
            //         rmServersForm.setFs(fs);
            //         int pss = RequestUtils.getPageSize(request, "pss");
            //         rmServersForm.setPss(pss);
            //         // TODO: Fully initialize the remove servers form.
            //         request.setAttribute(Constants.RESOURCE_REMOVE_SERVERS_FORM_ATTR, rmServersForm);
            //
            //         RemoveResourceGroupsForm rmGroupsForm = new RemoveResourceGroupsForm();
            //         // TODO: Fully initialize the remove resource groups form.
            //         int psg = RequestUtils.getPageSize(request, "psg");
            //         rmGroupsForm.setPsg(psg);
            //         request.setAttribute(Constants.RESOURCE_REMOVE_GROUPS_MEMBERS_FORM_ATTR, rmGroupsForm);

            // TODO: Stick config props into the request in a way the JSP can easily consume.

        } catch (PermissionException e) {
            RequestUtils.setError(request, "resource.platform.inventory.error.ViewServersPermission");
        }

        return null;
    }
}
