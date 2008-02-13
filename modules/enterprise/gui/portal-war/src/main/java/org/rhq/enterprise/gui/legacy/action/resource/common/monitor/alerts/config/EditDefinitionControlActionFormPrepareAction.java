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
import java.util.List;
import java.util.Map;
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
import org.rhq.core.domain.event.alert.AlertDefinition;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.AlertDefUtil;
import org.rhq.enterprise.gui.legacy.beans.OptionItem;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;

/**
 * An Action that retrieves data from the BizApp to facilitate display of the <em>Edit Alert Definition Control
 * Action</em> form.
 */
public class EditDefinitionControlActionFormPrepareAction extends TilesAction {
    protected Log log = LogFactory.getLog(EditDefinitionControlActionFormPrepareAction.class);

    /**
     * Prepare the form for a new alert definition.
     */
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.debug("in edit definition control action form prepare ...");

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

        Subject subject = RequestUtils.getSubject(request);
        AlertDefinition alertDefinition = AlertDefUtil.getAlertDefinition(request);
        List<OptionItem> operationNames = AlertDefUtil.getControlActions(subject, id, isAlertTemplate);

        // drop-downs
        operationsForm.setControlActions(operationNames);
        if (alertDefinition.getOperationDefinition() != null) {
            Integer operationId = alertDefinition.getOperationDefinition().getId();

            operationsForm.setControlAction(String.valueOf(operationId));
        }

        // properties
        //        AlertDefinition alertDef = AlertDefUtil.getAlertDefinition(request, sessionID, eb);
        //        AlertDefUtil.ControlActionInfo cav = AlertDefUtil.getControlActionInfo(alertDef);
        //        caForm.setAd( alertDef.getId() );
        //        if (null != cav) {
        //            caForm.setId( cav.action.getId() );
        //            caForm.setControlAction(cav.controlAction);
        //        }

        return null;
    }
}

// EOF
