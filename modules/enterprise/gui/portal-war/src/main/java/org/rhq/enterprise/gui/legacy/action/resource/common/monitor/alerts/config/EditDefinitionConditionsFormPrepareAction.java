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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.AlertDefUtil;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;

/**
 * Prepare the alert definition form for editConditions.
 */
public class EditDefinitionConditionsFormPrepareAction extends DefinitionFormPrepareAction {
    protected Log log = LogFactory.getLog(EditDefinitionConditionsFormPrepareAction.class);

    @Override
    protected void setupConditions(HttpServletRequest request, DefinitionForm defForm) throws Exception {
        Integer alertDefId = new Integer(request.getParameter("ad"));
        log.trace("alertDefId=" + alertDefId);

        AlertDefinition alertDef = AlertDefUtil.getAlertDefinition(request);

        // Get the alert names
        Collection alertdefs = defForm.getAlertnames();
        for (Iterator it = alertdefs.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();

            //  Don't include this alert definition
            if (entry.getValue().equals(alertDef.getId())) {
                it.remove();
                break;
            }
        }

        Subject subject = RequestUtils.getSubject(request);
        defForm.importConditionsEnablement(alertDef, subject);
    }
}