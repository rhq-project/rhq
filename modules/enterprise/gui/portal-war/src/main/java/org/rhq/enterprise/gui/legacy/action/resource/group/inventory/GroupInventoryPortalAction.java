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

import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.Portal;
import org.rhq.enterprise.gui.legacy.action.resource.GroupController;
import org.rhq.enterprise.gui.legacy.exception.ParameterNotFoundException;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;

/**
 */
public class GroupInventoryPortalAction extends GroupController {
    /**
     * The request scope attribute under which actions store the full <code>List</code> of <code>EmptyValue</code>
     * objects. temporary list - mtk (will remove - implementing the groups.)
     */
    public static final String EMPTY_VALS_ATTR = "EmptyValues";

    protected Log log = LogFactory.getLog(GroupInventoryPortalAction.class);

    /**
     * @see org.apache.struts.actions.LookupDispatchAction#getKeyMethodMap()
     */
    private static Properties keyMethodMap = new Properties();

    static {
        keyMethodMap.setProperty(Constants.MODE_NEW, "newResource");
        keyMethodMap.setProperty(Constants.MODE_EDIT, "editResourceGeneral");
        keyMethodMap.setProperty(Constants.MODE_EDIT_TYPE, "editResourceTypeHost");
        keyMethodMap.setProperty(Constants.MODE_VIEW, "viewResource");
        keyMethodMap.setProperty(Constants.MODE_ADD_RESOURCES, "addResources");
    }

    protected Properties getKeyMethodMap() {
        return keyMethodMap;
    }

    public ActionForward newResource(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Portal portal = Portal.createPortal("resource.group.inventory.NewGroup", ".resource.group.inventory.NewGroup");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward editResourceGeneral(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResourceGroup(request);

        Portal portal = Portal.createPortal("resource.group.inventory.EditGeneralProperties",
            ".resource.group.inventory.EditGeneralProperties");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward editResourceTypeHost(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResourceGroup(request);

        Portal portal = Portal.createPortal("resource.group.inventory.EditTypeAndHostProperties",
            ".resource.group.inventory.EditTypeAndHostProperties");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward addResources(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResourceGroup(request);

        Portal portal = Portal.createPortal("resource.group.inventory.AddResources",
            ".resource.group.inventory.AddResources");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward viewResource(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResourceGroup(request);

        super.setNavMapLocation(request, mapping, Constants.INVENTORY_LOC_TYPE);

        // clean out the return path
        SessionUtils.resetReturnPath(request.getSession());

        // set the return path
        try {
            setReturnPath(request, mapping);
        } catch (ParameterNotFoundException pne) {
            if (log.isDebugEnabled()) {
                log.debug("returnPath error:", pne);
            }
        }

        Portal portal = Portal
            .createPortal("resource.group.inventory.ViewGroup", ".resource.group.inventory.ViewGroup");
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    protected void editConfig(HttpServletRequest request, Portal portal) throws Exception {
        setResourceGroup(request, true);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);
    }
}