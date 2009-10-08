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

import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.Portal;
import org.rhq.enterprise.gui.legacy.action.resource.ResourceController;

/**
 * TODO: I think this can be deleted, since the Resource Inventory page is now JSF-based. (ips, 11/13/07)
 *
 * @author Ian Springer
 */
public class ResourceInventoryPortalAction extends ResourceController {
    private static final Properties KEY_METHOD_MAP = new Properties();

    static {
        KEY_METHOD_MAP.setProperty(ParamConstants.MODE_VIEW, "viewResource");
        KEY_METHOD_MAP.setProperty(ParamConstants.MODE_CHANGE_OWNER, "changeOwner");
    }

    public ActionForward viewResource(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        super.setResource(request);
        super.setNavMapLocation(request, mapping, Constants.INVENTORY_LOC_TYPE);
        Portal portal = Portal.createPortal("resource.platform.inventory.ViewPlatformTitle", // TODO
            ".resource.inventory.ViewResource");
        request.setAttribute(AttrConstants.PORTAL_KEY, portal);
        return null;
    }

    /**
     * Create a portal page to change the owner of a resource.
     *
     * @throws Exception
     */
    public ActionForward changeOwner(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        super.setResource(request);

        Portal portal = Portal.createPortal("resource.common.inventory.ChangeOwnerTitle",
            ".resource.inventory.ChangeOwner");
        request.setAttribute(AttrConstants.PORTAL_KEY, portal);
        return null;
    }

    protected void editConfig(HttpServletRequest request, Portal portal) throws Exception {
        setResource(request, true);
        portal.setDialog(true);
        request.setAttribute(AttrConstants.PORTAL_KEY, portal);
    }

    @Override
    protected Properties getKeyMethodMap() {
        return KEY_METHOD_MAP;
    }
}