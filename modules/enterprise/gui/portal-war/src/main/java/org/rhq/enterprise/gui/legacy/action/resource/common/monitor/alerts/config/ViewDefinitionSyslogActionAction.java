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

import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;
import org.rhq.core.domain.event.alert.AlertDefinition;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.AlertDefUtil;
import org.rhq.enterprise.server.legacy.common.shared.HQConstants;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * View an alert definition -- syslog action.
 */
public class ViewDefinitionSyslogActionAction extends TilesAction {
    private Log log = LogFactory.getLog(ViewDefinitionSyslogActionAction.class.getName());

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Properties config = LookupUtil.getSystemManager().getSystemConfiguration();

        String enabledStr = config.getProperty(HQConstants.SyslogActionsEnabled);
        Boolean syslogActionsEnabled = Boolean.valueOf(enabledStr);
        request.setAttribute(HQConstants.SyslogActionsEnabled, syslogActionsEnabled);

        if (syslogActionsEnabled) {
            AlertDefinition alertDef = AlertDefUtil.getAlertDefinition(request);
            SyslogActionForm saForm = new SyslogActionForm();
            AlertDefUtil.prepareSyslogActionForm(alertDef, saForm);
            request.setAttribute("syslogActionForm", saForm);
        }

        return null;
    }
}