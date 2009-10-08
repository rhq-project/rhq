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
package org.rhq.enterprise.gui.legacy.action;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.Portal;
import org.rhq.enterprise.gui.legacy.exception.ParameterNotFoundException;
import org.rhq.enterprise.gui.legacy.util.ActionUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;

/**
 * An abstract <strong>Action</strong> that dispatches to a subclass mapped method based on the value of a request
 * parameter.
 */
public abstract class BaseDispatchAction extends DispatchAction {
    private static Log log = LogFactory.getLog(BaseDispatchAction.class.getName());

    /**
     * Process the specified HTTP request, and create the corresponding HTTP response (or forward to another web
     * component that will create it). Return an <code>ActionForward</code> instance describing where and how control
     * should be forwarded, or <code>null</code> if the response has already been completed.
     *
     * @param     mapping  The ActionMapping used to select this instance
     * @param     request  The HTTP request we are processing
     * @param     response The HTTP response we are creating
     * @param     form     The optional ActionForm bean for this request (if any)
     *
     * @return    Describes where and how control should be forwarded.
     *
     * @exception Exception if an error occurs
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        // identify the request parameter containing the method name
        String parameter = mapping.getParameter();
        if (parameter == null) {
            throw new ServletException("no dispatch parameter configured");
        }

        // identify the string to look up
        String name = request.getParameter(parameter);
        if (name == null) {
            throw new ServletException("dispatch parameter [" + parameter + "] not found");
        }

        // look up the dispatch method
        String methodName = getKeyMethodMap().getProperty(name);
        if (methodName == null) {
            throw new ServletException("dispatch method for key [" + name + "] not found");
        }

        // execute the dispatch method
        ActionForward fwd = dispatchMethod(mapping, form, request, response, methodName);

        // save the return path in case the user clicks into a
        // workflow. be sure to include the mode parameter.
        try {
            Portal tmpPortal = (Portal) request.getAttribute(AttrConstants.PORTAL_KEY);
            if (tmpPortal.doWorkflow()) {
                Map<String, String> params = tmpPortal.getWorkflowParams();
                if (params == null) {
                    params = new HashMap<String, String>();
                    params.put(ParamConstants.MODE_PARAM, name);
                }

                setReturnPath(request, mapping, params);
            }
        } catch (ServletException e) {
            log.debug("Could not save return path: ", e);
        } catch (ParameterNotFoundException pne) {
            log.debug("Could not save return path: " + pne);
        }

        return fwd;
    }

    /**
     * Provides the mapping from resource key to method name
     *
     * @return Resource key / method name map
     */
    protected abstract Properties getKeyMethodMap();

    /**
     * Method to overload if the controller wants to be an origin for workflows. Child classes should customize this to
     * participate in workflows.
     *
     * @param request The request to get the session to store the returnPath into.
     * @param mapping The ActionMapping to get the input forward from.
     * @param params  A Map of request parameters to add to the return path.
     */
    protected void setReturnPath(HttpServletRequest request, ActionMapping mapping, Map params) throws Exception {
        String returnPath = ActionUtils.findReturnPath(mapping, params);
        if (log.isTraceEnabled()) {
            log.trace("setting return path: " + returnPath);
        }

        SessionUtils.setReturnPath(request.getSession(), returnPath);
    }

    protected void setReturnPath(HttpServletRequest request, ActionMapping mapping) throws Exception {
        setReturnPath(request, mapping, new HashMap());
    }
}