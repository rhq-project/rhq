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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.exception.UndefinedForwardException;
import org.rhq.enterprise.gui.legacy.util.ActionUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;

/**
 * An <code>Action</code> subclass that provides convenience methods for recognizing form submission types (cancel,
 * reset, ok, etc) and deciding where to return after the action has completed.
 */
public class BaseAction extends Action {
    public static final boolean YES_RETURN_PATH = true;
    public static final boolean NO_RETURN_PATH = false;

    private final Log log = LogFactory.getLog(BaseAction.class.getName());

    //-------------------------------------public methods

    /**
     * Dosen't do a damn thing. its here as a dummy method used by list that don't submit a form.
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        return null;
    }

    /**
     * Return an <code>ActionForward</code> if the form has been cancelled or reset; otherwise return <code>null</code>
     * so that the subclass can continue to execute.
     */
    public ActionForward checkSubmit(HttpServletRequest request, ActionMapping mapping, ActionForm form, Map params,
        boolean doReturnPath) throws Exception {
        BaseValidatorForm spiderForm = (BaseValidatorForm) form;

        if (spiderForm.isCancelClicked()) {
            return returnCancelled(request, mapping, params, doReturnPath);
        }

        if (spiderForm.isResetClicked()) {
            spiderForm.reset(mapping, request);
            return returnReset(request, mapping, params);
        }

        if (spiderForm.isCreateClicked()) {
            return returnNew(request, mapping, params);
        }

        if (spiderForm.isAddClicked()) {
            return returnAdd(request, mapping, params);
        }

        if (spiderForm.isRemoveClicked()) {
            return returnRemove(request, mapping, params);
        }

        if (spiderForm.isInstallClicked()) {
            return returnInstall(request, mapping, params);
        }

        if (spiderForm.isManualUninstallClicked()) {
            return returnManualUninstall(request, mapping, params);
        }

        return null;
    }

    public ActionForward checkSubmit(HttpServletRequest request, ActionMapping mapping, ActionForm form, Map params)
        throws Exception {
        return checkSubmit(request, mapping, form, params, NO_RETURN_PATH);
    }

    public ActionForward checkSubmit(HttpServletRequest request, ActionMapping mapping, ActionForm form,
        boolean doReturnPath) throws Exception {
        return checkSubmit(request, mapping, form, (Map) null, doReturnPath);
    }

    public ActionForward checkSubmit(HttpServletRequest request, ActionMapping mapping, ActionForm form)
        throws Exception {
        return checkSubmit(request, mapping, form, (Map) null, NO_RETURN_PATH);
    }

    public ActionForward checkSubmit(HttpServletRequest request, ActionMapping mapping, ActionForm form, String param,
        Object value, boolean doReturnPath) throws Exception {
        HashMap params = new HashMap(1);
        params.put(param, value);
        return checkSubmit(request, mapping, form, params, doReturnPath);
    }

    public ActionForward checkSubmit(HttpServletRequest request, ActionMapping mapping, ActionForm form, String param,
        Object value) throws Exception {
        return checkSubmit(request, mapping, form, param, value, NO_RETURN_PATH);
    }

    /**
     * Return an <code>ActionForward</code> representing the <em>add</em> form gesture, setting the return path to the
     * current URL.
     */
    public ActionForward returnAdd(HttpServletRequest request, ActionMapping mapping, Map params) throws Exception {
        return constructForward(request, mapping, Constants.ADD_URL, params, NO_RETURN_PATH);
    }

    public ActionForward returnAdd(HttpServletRequest request, ActionMapping mapping) throws Exception {
        return returnAdd(request, mapping, null);
    }

    public ActionForward returnAdd(HttpServletRequest request, ActionMapping mapping, String param, Object value)
        throws Exception {
        HashMap params = new HashMap(1);
        params.put(param, value);
        return returnAdd(request, mapping, params);
    }

    /**
     * Return an <code>ActionForward</code> representing the <em>cancel</em> form gesture.
     */
    public ActionForward returnCancelled(HttpServletRequest request, ActionMapping mapping, Map params,
        boolean doReturnPath) throws Exception {
        return constructForward(request, mapping, Constants.CANCEL_URL, params, doReturnPath);
    }

    public ActionForward returnCancelled(HttpServletRequest request, ActionMapping mapping, Map params)
        throws Exception {
        return returnCancelled(request, mapping, params, YES_RETURN_PATH);
    }

    public ActionForward returnCancelled(HttpServletRequest request, ActionMapping mapping) throws Exception {
        return returnCancelled(request, mapping, null);
    }

    public ActionForward returnCancelled(HttpServletRequest request, ActionMapping mapping, String param, Object value)
        throws Exception {
        HashMap params = new HashMap(1);
        params.put(param, value);
        return returnCancelled(request, mapping, params);
    }

    /**
     * Return an <code>ActionForward</code> representing the <em>failure</em> action state.
     */
    public ActionForward returnFailure(HttpServletRequest request, ActionMapping mapping, Map params,
        boolean doReturnPath) throws Exception {
        return constructForward(request, mapping, Constants.FAILURE_URL, params, doReturnPath);
    }

    public ActionForward returnFailure(HttpServletRequest request, ActionMapping mapping, Map params) throws Exception {
        return returnFailure(request, mapping, params, NO_RETURN_PATH);
    }

    public ActionForward returnFailure(HttpServletRequest request, ActionMapping mapping, boolean doReturnPath)
        throws Exception {
        return returnFailure(request, mapping, (Map) null, doReturnPath);
    }

    public ActionForward returnFailure(HttpServletRequest request, ActionMapping mapping) throws Exception {
        return returnFailure(request, mapping, NO_RETURN_PATH);
    }

    public ActionForward returnFailure(HttpServletRequest request, ActionMapping mapping, String param, Object value)
        throws Exception {
        HashMap params = new HashMap(1);
        params.put(param, value);
        return constructForward(request, mapping, Constants.FAILURE_URL, params, NO_RETURN_PATH);
    }

    /**
     * Return an <code>ActionForward</code> representing the <em>new</em> form gesture, setting the return path to the
     * current URL.
     */
    public ActionForward returnNew(HttpServletRequest request, ActionMapping mapping, Map params) throws Exception {
        return constructForward(request, mapping, Constants.SUCCESS_URL, params, NO_RETURN_PATH);
    }

    public ActionForward returnNew(HttpServletRequest request, ActionMapping mapping) throws Exception {
        return returnNew(request, mapping, (Map) null, NO_RETURN_PATH);
    }

    public ActionForward returnNew(HttpServletRequest request, ActionMapping mapping, String param, Object value)
        throws Exception {
        HashMap params = new HashMap();
        params.put(param, value);
        return constructForward(request, mapping, Constants.SUCCESS_URL, params, NO_RETURN_PATH);
    }

    /**
     * Return an <code>ActionForward</code> representing the <em>success</em> action state.
     */
    public ActionForward returnNew(HttpServletRequest request, ActionMapping mapping, Map params, boolean doReturnPath)
        throws Exception {
        return constructForward(request, mapping, Constants.SUCCESS_URL, params, doReturnPath);
    }

    public ActionForward returnNew(HttpServletRequest request, ActionMapping mapping, String param, Object value,
        boolean doReturnPath) throws Exception {
        HashMap params = new HashMap(1);
        params.put(param, value);
        return returnNew(request, mapping, params, doReturnPath);
    }

    /**
     * Return an <code>ActionForward</code> representing the <em>remove</em> form gesture, setting the return path to the
     * current URL.
     */
    public ActionForward returnRemove(HttpServletRequest request, ActionMapping mapping, Map params) throws Exception {
        return constructForward(request, mapping, Constants.REMOVE_URL, params, NO_RETURN_PATH);
    }

    public ActionForward returnRemove(HttpServletRequest request, ActionMapping mapping) throws Exception {
        return returnRemove(request, mapping, null);
    }

    public ActionForward returnRemove(HttpServletRequest request, ActionMapping mapping, String param, Object value)
        throws Exception {
        HashMap params = new HashMap(1);
        params.put(param, value);
        return returnRemove(request, mapping, params);
    }

    /**
     * Return an <code>ActionForward</code> representing the <em>install</em> form gesture, setting the return path to
     * the current URL.
     */
    public ActionForward returnInstall(HttpServletRequest request, ActionMapping mapping, Map params) throws Exception {
        return constructForward(request, mapping, Constants.INSTALL_URL, params, NO_RETURN_PATH);
    }

    public ActionForward returnInstall(HttpServletRequest request, ActionMapping mapping) throws Exception {
        return returnInstall(request, mapping, null);
    }

    public ActionForward returnInstall(HttpServletRequest request, ActionMapping mapping, String param, Object value)
        throws Exception {
        HashMap params = new HashMap(1);
        params.put(param, value);
        return returnInstall(request, mapping, params);
    }

    /**
     * Return an <code>ActionForward</code> representing the <em>install</em> form gesture, setting the return path to
     * the current URL.
     */
    public ActionForward returnManualUninstall(HttpServletRequest request, ActionMapping mapping, Map params)
        throws Exception {
        return constructForward(request, mapping, Constants.MANUAL_UNINSTALL_URL, params, NO_RETURN_PATH);
    }

    public ActionForward returnManualUninstall(HttpServletRequest request, ActionMapping mapping) throws Exception {
        return returnManualUninstall(request, mapping, null);
    }

    public ActionForward returnManualUninstall(HttpServletRequest request, ActionMapping mapping, String param,
        Object value) throws Exception {
        HashMap params = new HashMap(1);
        params.put(param, value);
        return returnManualUninstall(request, mapping, params);
    }

    /**
     * Return an <code>ActionForward</code> representing the <em>reset</em> form gesture.
     */
    public ActionForward returnReset(HttpServletRequest request, ActionMapping mapping, Map params, boolean doReturnPath)
        throws Exception {
        return constructForward(request, mapping, Constants.RESET_URL, params, doReturnPath);
    }

    public ActionForward returnReset(HttpServletRequest request, ActionMapping mapping, Map params) throws Exception {
        return returnReset(request, mapping, params, NO_RETURN_PATH);
    }

    public ActionForward returnReset(HttpServletRequest request, ActionMapping mapping) throws Exception {
        return returnReset(request, mapping, null);
    }

    public ActionForward returnReset(HttpServletRequest request, ActionMapping mapping, String param, Object value)
        throws Exception {
        HashMap params = new HashMap(1);
        params.put(param, value);
        return returnReset(request, mapping, params);
    }

    /**
     * Return an <code>ActionForward</code> representing the <em>okassign</em> action state.
     */
    public ActionForward returnOkAssign(HttpServletRequest request, ActionMapping mapping, Map params) throws Exception {
        return returnOkAssign(request, mapping, params, YES_RETURN_PATH);
    }

    /**
     * Return an <code>ActionForward</code> representing the <em>okassign</em> action state.
     */
    public ActionForward returnOkAssign(HttpServletRequest request, ActionMapping mapping, Map params,
        boolean doReturnPath) throws Exception {
        return constructForward(request, mapping, Constants.OK_ASSIGN_URL, params, doReturnPath);
    }

    /**
     * Return an <code>ActionForward</code> representing the <em>success</em> action state.
     */
    public ActionForward returnSuccess(HttpServletRequest request, ActionMapping mapping, Map params,
        boolean doReturnPath) throws Exception {
        if (doReturnPath) {
            doReturnPath = !SessionUtils.getReturnPathIgnoredForOk(request.getSession());
        }

        try {
            return constructForward(request, mapping, Constants.SUCCESS_URL, params, doReturnPath);
        } catch (UndefinedForwardException e) {
            // if there's no success forward defined, struts will send
            // us back to the same place we were before
            return null;
        }
    }

    public ActionForward returnSuccess(HttpServletRequest request, ActionMapping mapping, Map params) throws Exception {
        return returnSuccess(request, mapping, params, YES_RETURN_PATH);
    }

    public ActionForward returnSuccess(HttpServletRequest request, ActionMapping mapping) throws Exception {
        return returnSuccess(request, mapping, null);
    }

    public ActionForward returnSuccess(HttpServletRequest request, ActionMapping mapping, String param, Object value,
        boolean doReturnPath) throws Exception {
        HashMap params = new HashMap(1);
        params.put(param, value);
        return returnSuccess(request, mapping, params, doReturnPath);
    }

    public ActionForward returnSuccess(HttpServletRequest request, ActionMapping mapping, String param, Object value)
        throws Exception {
        return returnSuccess(request, mapping, param, value, YES_RETURN_PATH);
    }

    //-------------------------------------protected methods

    /**
     * Return an <code>ActionForward</code> corresponding to the given form gesture or action state. Utilize the session
     * return path if it is set. Optionally set a request parameter to the path.
     */
    protected ActionForward constructForward(HttpServletRequest request, ActionMapping mapping, String forwardName,
        Map params, boolean doReturnPath) throws Exception {
        ActionForward forward = null;
        ActionForward mappedForward = mapping.findForward(forwardName);
        HttpSession session = request.getSession();

        if (mapping instanceof BaseActionMapping) {
            BaseActionMapping smap = (BaseActionMapping) mapping;
            String workflow = smap.getWorkflow();
            String returnPath = null;
            if (doReturnPath && (workflow != null) && !"".equals(workflow)) {
                returnPath = SessionUtils.popWorkflow(session, workflow);
            }

            if (log.isTraceEnabled()) {
                log.trace("forwardName=" + forwardName);
                log.trace("returnPath=" + returnPath);
            }

            if (returnPath != null) {
                boolean redirect = (mappedForward != null) && mappedForward.getRedirect();
                forward = new ActionForward(forwardName, returnPath, redirect);
            }
        }

        if (forward == null) {
            // no return path, use originally requested forward
            forward = mappedForward;
        }

        if (forward == null) {
            // requested forward not defined
            throw new UndefinedForwardException(forwardName);
        }

        if (params != null) {
            forward = ActionUtils.changeForwardPath(forward, params);
        }

        return forward;
    }

    protected ActionForward constructForward(HttpServletRequest request, ActionMapping mapping, String forwardName,
        String param, Object value, boolean doReturnPath) throws Exception {
        HashMap params = new HashMap(1);
        params.put(param, value);
        return constructForward(request, mapping, forwardName, params, doReturnPath);
    }

    protected ActionForward constructForward(HttpServletRequest request, ActionMapping mapping, String forwardName,
        boolean doReturnPath) throws Exception {
        return constructForward(request, mapping, forwardName, null, doReturnPath);
    }

    protected ActionForward constructForward(HttpServletRequest request, ActionMapping mapping, String forwardName)
        throws Exception {
        return constructForward(request, mapping, forwardName, null, NO_RETURN_PATH);
    }
}

// EOF
