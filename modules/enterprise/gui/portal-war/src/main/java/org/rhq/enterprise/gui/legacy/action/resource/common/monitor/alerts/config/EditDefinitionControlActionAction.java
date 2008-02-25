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
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.AlertDefUtil;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertTemplateManagerLocal;
import org.rhq.enterprise.server.legacy.events.EventConstants;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Create a new alert definition.
 */
public class EditDefinitionControlActionAction extends BaseAction {
    private Log log = LogFactory.getLog(EditDefinitionControlActionAction.class);

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        log.trace("in edit alert definition controlAction ...");

        ControlActionForm operationsForm = (ControlActionForm) form;
        boolean isAlertTemplate = operationsForm.isAlertTemplate();
        log.trace("defForm.id=" + operationsForm.getId());

        Map<String, Integer> params = new HashMap<String, Integer>();
        params.put("ad", operationsForm.getAd());

        Integer id = null;
        if (isAlertTemplate) {
            params.put(Constants.RESOURCE_TYPE_ID_PARAM, operationsForm.getType());
            id = operationsForm.getType();
        } else {
            params.put(Constants.RESOURCE_ID_PARAM, operationsForm.getId());
            id = operationsForm.getId();
        }

        // early return from a cancel button
        ActionForward forward = checkSubmit(request, mapping, form, params);
        if (forward != null) {
            log.trace("returning " + forward);
            return forward;
        }

        String selectedOperation = operationsForm.getControlAction();

        OperationManagerLocal operationManager = LookupUtil.getOperationManager();
        AlertTemplateManagerLocal alertTemplateManager = LookupUtil.getAlertTemplateManager();
        AlertDefinitionManagerLocal alertDefinitionManager = LookupUtil.getAlertDefinitionManager();

        Subject subject = RequestUtils.getSubject(request);
        AlertDefinition alertDefinition = AlertDefUtil.getAlertDefinition(request);

        OperationDefinition operationDefinition = null;

        if (selectedOperation.equals(EventConstants.CONTROL_ACTION_NONE) == false) {
            Integer operationId = Integer.parseInt(selectedOperation);

            operationDefinition = operationManager.getOperationDefinition(subject, operationId);
        }

        alertDefinition.setOperationDefinition(operationDefinition);

        if (isAlertTemplate) {
            alertTemplateManager.updateAlertTemplate(subject, alertDefinition, operationsForm.isCascade());
        } else {
            alertDefinitionManager.updateAlertDefinition(subject, alertDefinition);
        }

        return returnSuccess(request, mapping, params);
    }
}