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

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;

/**
 * An Action that removes notifications for an alert definition.
 */
public abstract class RemoveNotificationsAction extends BaseAction {
    private Log log = LogFactory.getLog(RemoveNotificationsAction.class);

    /**
     * removes alert notifications
     */
    @Override
    @SuppressWarnings("deprecation")
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        RemoveNotificationsForm rnForm = (RemoveNotificationsForm) form;
        Subject subject = RequestUtils.getSubject(request);
        int recordsRemoved = handleRemove(subject, rnForm);

        Map<String, Integer> params = new HashMap<String, Integer>();
        params.put(Constants.ALERT_DEFINITION_PARAM, rnForm.getAd());
        if (rnForm.isAlertTemplate()) {
            params.put(Constants.RESOURCE_TYPE_ID_PARAM, rnForm.getType());
        } else {
            params.put(Constants.RESOURCE_ID_PARAM, rnForm.getId());
        }

        if (recordsRemoved > 0) {
            return returnSuccess(request, mapping, params);
        } else {
            return returnFailure(request, mapping, params);
        }
    }

    protected abstract int handleRemove(Subject subject, RemoveNotificationsForm rnForm) throws Exception;
}