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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.core.clientapi.util.StringUtil;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.Portal;
import org.rhq.enterprise.gui.legacy.Portlet;
import org.rhq.enterprise.gui.legacy.action.resource.ResourceController;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.AlertDefUtil;
import org.rhq.enterprise.gui.legacy.exception.ParameterNotFoundException;
import org.rhq.enterprise.gui.legacy.util.ActionUtils;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A dispatcher for the alerts portal.
 */
public class PortalAction extends ResourceController {
    protected static Log log = LogFactory.getLog(PortalAction.class);

    private static Properties keyMethodMap = new Properties();

    static {
        keyMethodMap.setProperty(Constants.MODE_LIST, "listDefinitions");
        keyMethodMap.setProperty(Constants.MODE_VIEW, "listDefinitions");
        keyMethodMap.setProperty(Constants.MODE_NEW, "newDefinition");

        keyMethodMap.setProperty("editProperties", "editDefinitionProperties");
        keyMethodMap.setProperty("editConditions", "editDefinitionConditions");
        keyMethodMap.setProperty("editControlAction", "editDefinitionControlAction");
        keyMethodMap.setProperty("editSyslogAction", "editDefinitionSyslogAction");

        keyMethodMap.setProperty("viewOthers", "viewDefinitionOthers");
        keyMethodMap.setProperty("viewRoles", "viewDefinitionRoles");
        keyMethodMap.setProperty("viewUsers", "viewDefinitionUsers");
        keyMethodMap.setProperty("viewSnmp", "viewDefinitionSNMP");

        keyMethodMap.setProperty("addOthers", "addOthersDefinitions");
        keyMethodMap.setProperty("addRoles", "addRolesDefinitions");
        keyMethodMap.setProperty("addUsers", "addUsersDefinitions");
    }

    @Override
    protected Properties getKeyMethodMap() {
        return keyMethodMap;
    }

    /**
     * We override this in case the resource has been deleted ... simply ignore that fact.
     */
    @Override
    protected void setResource(HttpServletRequest request) throws Exception {
        try {
            super.setResource(request);
        } catch (ParameterNotFoundException e) {
            log.warn("No resource found.");
        }
    }

    private void setTitle(HttpServletRequest request, Portal portal, String titleName) throws Exception {
        Resource resource = RequestUtils.getResource(request);
        ResourceType type;
        if (resource == null) {
            // template alert definition
            type = RequestUtils.getResourceType(request);
        } else {
            // resource alert definition
            type = resource.getResourceType();
        }

        ResourceCategory category = type.getCategory();

        titleName = StringUtil.replace(titleName, "platform", category.toString().toLowerCase());

        portal.setName(titleName);

        // if there's an alert definition available, set our second
        // title parameter to its name
        try {
            AlertDefinition alertDef = AlertDefUtil.getAlertDefinition(request);
            request.setAttribute(Constants.TITLE_PARAM2_ATTR, alertDef.getName());
        } catch (ParameterNotFoundException e) {
            // it's okay
            log.trace("couldn't find alert definition: " + e.getMessage());
        }
    }

    private void setSNMPEnabled(HttpServletRequest request) throws RemoteException {
        Properties props = LookupUtil.getSystemManager().getSystemConfiguration();

        // See if the property exists
        if (props.containsKey(RHQConstants.SNMPVersion)) {
            String ver = props.getProperty(RHQConstants.SNMPVersion);
            request.setAttribute("snmpEnabled", ver.length() > 0);
        }
    }

    public ActionForward newDefinition(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);
        Portal portal = Portal.createPortal();
        setTitle(request, portal, "alert.config.platform.edit.NewAlertDef.Title");
        portal.addPortlet(new Portlet(".events.config.new"), 1);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward editDefinitionProperties(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);
        Portal portal = Portal.createPortal();
        setTitle(request, portal, "alert.config.platform.edit.page.Title");
        portal.addPortlet(new Portlet(".events.config.edit.properties"), 1);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward editDefinitionConditions(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);
        Portal portal = Portal.createPortal();
        setTitle(request, portal, "alert.config.platform.edit.condition.Title");
        portal.addPortlet(new Portlet(".events.config.edit.conditions"), 1);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward editDefinitionControlAction(ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        setResource(request);
        Portal portal = Portal.createPortal();
        setTitle(request, portal, "alerts.config.platform.EditControlAction.Title");
        portal.addPortlet(new Portlet(".events.config.edit.controlaction"), 1);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward editDefinitionSyslogAction(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);
        Portal portal = Portal.createPortal();
        setTitle(request, portal, "alerts.config.platform.EditSyslogAction.Title");
        portal.addPortlet(new Portlet(".events.config.edit.syslogaction"), 1);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward viewDefinitionOthers(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);
        Portal portal = Portal.createPortal();
        setTitle(request, portal, "alert.config.platform.props.ViewDef.email.Title");
        portal.addPortlet(new Portlet(".events.config.view.others"), 1);

        // JW - this shouldn't be a dialog ... portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        setSNMPEnabled(request);
        return null;
    }

    public ActionForward viewDefinitionRoles(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);
        Portal portal = Portal.createPortal();
        setTitle(request, portal, "alert.config.platform.props.ViewDef.roles.Title");
        portal.addPortlet(new Portlet(".events.config.view.roles"), 1);

        // JW - this shouldn't be a dialog ... portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        setSNMPEnabled(request);
        return null;
    }

    public ActionForward viewDefinitionUsers(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);
        Portal portal = Portal.createPortal();
        setTitle(request, portal, "alert.config.platform.props.ViewDef.users.Title");
        portal.addPortlet(new Portlet(".events.config.view.users"), 1);

        // JW - this shouldn't be a dialog ... portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        setSNMPEnabled(request);
        return null;
    }

    public ActionForward viewDefinitionSNMP(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);
        Portal portal = Portal.createPortal();
        setTitle(request, portal, "alert.config.platform.props.ViewDef.users.Title");
        portal.addPortlet(new Portlet(".events.config.view.snmp"), 1);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        setSNMPEnabled(request);
        return null;
    }

    public ActionForward monitorConfigureAlerts(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);
        Portal portal = Portal.createPortal();
        portal.addPortlet(new Portlet(".events.config.list"), 1);
        portal.setDialog(false);

        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward listDefinitions(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);

        super.setNavMapLocation(request, mapping, Constants.ALERT_CONFIG_LOC);

        // clean out the return path
        SessionUtils.resetReturnPath(request.getSession());

        // set the return path
        try {
            setReturnPath(request, mapping);
        } catch (ParameterNotFoundException pne) {
            if (log.isDebugEnabled()) {
                log.debug("", pne);
            }
        }

        Portal portal = Portal.createPortal();
        setTitle(request, portal, "alerts.config.platform.DefinitionList.Title");
        portal.setDialog(false);

        try {
            RequestUtils.getStringParameter(request, Constants.APPDEF_RES_TYPE_ID);
            portal.addPortlet(new Portlet(".admin.alerts.List"), 1);
        } catch (ParameterNotFoundException e) {
            portal.addPortlet(new Portlet(".events.config.list"), 1);
        }

        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward addUsersDefinitions(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);
        Portal portal = Portal.createPortal();
        setTitle(request, portal, "alerts.config.platform.AssignUsersToAlertDefinition.Title");
        portal.addPortlet(new Portlet(".events.config.addusers"), 1);
        portal.setDialog(false);

        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    }

    public ActionForward addRolesDefinitions(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);
        Portal portal = Portal.createPortal();
        setTitle(request, portal, "alerts.config.platform.AssignRolesToAlertDefinition.Title");
        portal.addPortlet(new Portlet(".events.config.addroles"), 1);
        portal.setDialog(false);

        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    }

    public ActionForward addOthersDefinitions(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);
        Portal portal = Portal.createPortal();
        setTitle(request, portal, "alerts.config.platform.AssignOthersToAlertDefinition.Title");
        portal.addPortlet(new Portlet(".events.config.addothers"), 1);
        portal.setDialog(false);

        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    }

    /**
     * This sets the return path for a ResourceAction by appending the type and resource id to the forward url.
     *
     * @param     request The current controller's request.
     * @param     mapping The current controller's mapping that contains the input.
     *
     * @exception ParameterNotFoundException if the type or id are not found
     * @exception ServletException           If there is not input defined for this form
     */
    @Override
    protected void setReturnPath(HttpServletRequest request, ActionMapping mapping) throws Exception {
        HashMap parms = new HashMap();

        int resourceId = RequestUtils.getResourceId(request);
        parms.put(Constants.RESOURCE_ID_PARAM, resourceId);

        try {
            parms.put(Constants.ALERT_DEFINITION_PARAM, RequestUtils.getIntParameter(request,
                Constants.ALERT_DEFINITION_PARAM));
            parms.put(Constants.CHILD_RESOURCE_TYPE_ID_PARAM, WebUtility.getChildResourceTypeId(request));
        } catch (ParameterNotFoundException pnfe) {
            // that's ok!
            log.trace("couldn't find parameter: " + pnfe.getMessage());
        }

        // sets the returnPath to match the mode we're in.
        String mode = request.getParameter(Constants.MODE_PARAM);
        parms.put(Constants.MODE_PARAM, mode);

        String returnPath = ActionUtils.findReturnPath(mapping, parms);
        SessionUtils.setReturnPath(request.getSession(), returnPath);
    }
}