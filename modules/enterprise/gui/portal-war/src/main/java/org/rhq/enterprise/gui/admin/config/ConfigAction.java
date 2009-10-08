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
 * Created on May 16, 2003
 *
 */
package org.rhq.enterprise.gui.admin.config;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.Portal;
import org.rhq.enterprise.gui.legacy.action.BaseDispatchAction;

/**
 * Action that is triggered when the RHQ server configuration is to be edited.
 */
public class ConfigAction extends BaseDispatchAction {
    @Override
    protected Properties getKeyMethodMap() {
        Properties map = new Properties();
        map.setProperty(Constants.MODE_EDIT, "editConfig");
        return map;
    }

    public ActionForward editConfig(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        Portal portal = Portal.createPortal("admin.settings.EditServerConfig.Title", ".admin.config.EditConfig");
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }
}