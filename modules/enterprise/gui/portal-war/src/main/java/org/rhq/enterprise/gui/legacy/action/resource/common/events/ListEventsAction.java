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

package org.rhq.enterprise.gui.legacy.action.resource.common.events;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.util.WebUtility;

/**
 * @author Heiko W. Rupp
 * @author Jay Shaughnessy
 *
 */
public class ListEventsAction extends BaseAction {

    Log log = LogFactory.getLog(ListEventsAction.class);

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        EventsForm eForm = (EventsForm) form;

        // Get the filters set on the form. If set these settings take precedence
        String severityFilter = eForm.getSevFilter();
        String sourceFilter = eForm.getSourceFilter();
        String searchString = eForm.getSearchString();

        // If the form does not provide filter values then check for filters passed as parameters. 
        // Pagination bypasses the form settings so if navigating
        // from pagination we maintain the filter information only via request parameter.
        if (null == severityFilter) {
            severityFilter = WebUtility.getOptionalRequestParameter(request, "pSeverity", null);
        }
        if (null == sourceFilter) {
            sourceFilter = WebUtility.getOptionalRequestParameter(request, "pSource", null);
        }
        if (null == searchString) {
            searchString = WebUtility.getOptionalRequestParameter(request, "pSearch", null);
        }

        Map<String, String> returnRequestParams = new HashMap<String, String>();

        // Ensure the filters are provided by supplying them as return request params 
        if (null != severityFilter) {
            returnRequestParams.put("pSeverity", severityFilter);
        }
        if (null != sourceFilter) {
            returnRequestParams.put("pSource", sourceFilter);
        }
        if (null != searchString) {
            returnRequestParams.put("pSearch", searchString);
        }

        // Navigate back to self (EventsFormPrepareAction) which will actually populate the events list

        return returnSuccess(request, mapping, returnRequestParams);
    }

}
