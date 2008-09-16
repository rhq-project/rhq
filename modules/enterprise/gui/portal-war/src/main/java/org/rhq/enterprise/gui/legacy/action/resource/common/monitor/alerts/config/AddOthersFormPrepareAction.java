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
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.enterprise.gui.legacy.exception.ParameterNotFoundException;

/**
 * An Action that retrieves data from the BizApp to facilitate display
 * of the form to add other emailAddresses( non-CAM) to an alert Definition
 */
public class AddOthersFormPrepareAction extends Action {

    // ---------------------------------------------------- Public Methods

    /**
     * Retrieve this data and store it in the specified request
     * parameters:
     * <p/>
     * <ul>
     * <li><code>OwnedRoleValue</code> object identified by
     * <code>Constants.ROLE_PARAM</code> request parameter in in
     * <code>Constants.ROLE_ATTR</code></li>
     * <li><code>List</code> of available <code>AuthzSubjectValue</code>
     * objects (those not already associated with the role) in
     * </ul>
     */
    @Override
    @SuppressWarnings("deprecation")
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        @SuppressWarnings("unused")
        Log log = LogFactory.getLog(AddOthersFormPrepareAction.class);

        AddOthersForm addForm = (AddOthersForm) form;
        Integer alertDefId = addForm.getAd();

        if (alertDefId == null) {
            throw new ParameterNotFoundException("alert definition id not found");
        }

        return null;
    }
}
