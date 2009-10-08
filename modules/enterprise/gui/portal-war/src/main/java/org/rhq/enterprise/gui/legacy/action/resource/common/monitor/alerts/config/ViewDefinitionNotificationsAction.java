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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.AlertDefUtil;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.util.WebUtility;

/**
 * View an alert definition -- notified roles.
 */
public abstract class ViewDefinitionNotificationsAction<T extends AlertNotification> extends TilesAction {
    private Log log = LogFactory.getLog(ViewDefinitionNotificationsAction.class);

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        PageControl pc = WebUtility.getPageControl(request);

        log.debug("Viewing notifications for an alert definition ...");

        AlertDefinition alertDefinition = AlertDefUtil.getAlertDefinition(request);
        Subject subject = RequestUtils.getSubject(request);
        PageList<T> notifyList = getPageList(subject, alertDefinition, pc);

        request.setAttribute("notifyList", notifyList);
        request.setAttribute(Constants.LIST_SIZE_ATTR, notifyList.getTotalSize());

        return null;
    }

    protected abstract PageList<T> getPageList(Subject subject, AlertDefinition alertDefinition, PageControl pc);
}