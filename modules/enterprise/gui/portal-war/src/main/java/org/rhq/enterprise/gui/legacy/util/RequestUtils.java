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
package org.rhq.enterprise.gui.legacy.util;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.util.MessageResources;

import org.rhq.core.clientapi.util.ArrayUtil;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.HubConstants;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.exception.ParameterNotFoundException;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Utilities class that provides many convenience methods for logging, request parameter processing, etc.
 *
 * @deprecated replaced by {@link org.rhq.enterprise.gui.util.WebUtility}
 */
@Deprecated
public class RequestUtils {
    private static Log log = LogFactory.getLog(RequestUtils.class.getName());

    /**
     * Verify if a parameter exists in the request
     *
     * @param  name The name of the parameter in the request
     *
     * @return Boolean if the parameter exists in the request
     */
    public static boolean parameterExists(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value != null;
    }

    /**
     * Fetches the String value of the parameter from the request
     *
     * @param  name The name of the parameter in the request
     *
     * @return The value of the parameter passed into the request
     *
     * @throws ParameterNotFoundException If the parameter name is not found in the request
     */
    public static String getStringParameter(HttpServletRequest request, String name) throws ParameterNotFoundException {
        String[] values = request.getParameterValues(name);
        if (values == null) {
            throw new ParameterNotFoundException(name + " not found");
        } else if (values[0].length() == 0) {
            throw new ParameterNotFoundException(name + " was empty");
        } else {
            return values[0]; // Just return the first value
        }
    }

    /**
     * Fetchs the String value of the parameter from the request
     *
     * @param  name The name of the parameter in the request
     * @param  def  The default value to return if the parameter is not found
     *
     * @return The value of the parameter passed into the request
     */
    public static String getStringParameter(HttpServletRequest request, String name, String def) {
        try {
            return getStringParameter(request, name);
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Fetchs the int value of the parameter from the request
     *
     * @param  name The name of the parameter in the request
     *
     * @return The value of the parameter passed into the request
     *
     * @throws ParameterNotFoundException If the parameter name is not found in the request
     * @throws NumberFormatException      If the parameter value is not a valid integer
     */
    public static Integer getIntParameter(HttpServletRequest request, String name) throws ParameterNotFoundException,
        NumberFormatException {
        String intParam = getStringParameter(request, name);
        Integer tmpInt = Integer.valueOf(intParam);
        return tmpInt;
    }

    /**
     * Fetchs the int value of the parameter from the request
     *
     * @param  name The name of the parameter in the request
     * @param  def  The default value to return if the parameter is not found
     *
     * @return The value of the parameter passed into the request
     */
    public static Integer getIntParameter(HttpServletRequest request, String name, Integer def) {
        try {
            return getIntParameter(request, name);
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Return the <code>WebUser</code> representing the person currently interacting with the product.
     *
     * @throws ServletException if the session cannot be accessed
     */
    public static WebUser getWebUser(HttpServletRequest request) throws ServletException {
        //do not create a session one should already exist.
        HttpSession session = request.getSession(false);
        if (session == null) {
            // should throw SessionNotFoundException
            throw new ServletException("web session does not exist!");
        }

        return SessionUtils.getWebUser(request.getSession());
    }

    /**
     * Return the <code>Subject</code> representing the person currently interacting with the product.
     *
     * @throws ServletException if the session cannot be accessed
     * @deprecated Use {@link WebUtility#getSubject(HttpServletRequest)} instead
     */
    @Deprecated
    public static Subject getSubject(HttpServletRequest request) throws ServletException {
        return getWebUser(request).getSubject();
    }

    /**
     * Extract the BizApp session id as an <code>Integer</code> from the web session.
     *
     * @throws ServletException if the session cannot be accessed
     */
    public static Integer getSessionId(HttpServletRequest request) throws ServletException {
        return getWebUser(request).getSessionId();
    }

    /**
     * Extract the subcontroller mode from the <code>Constants.MODE_PARAM</code> parameter of the HTTP request.
     */
    public static String getMode(HttpServletRequest request) {
        String mode = request.getParameter(Constants.MODE_PARAM);
        if ((mode == null) || mode.equals("")) {
            mode = Constants.MODE_LIST;
        }

        return mode;
    }

    /**
     * Retrieve the <code>Integer</code> value of the <strong>ROLE_PARAM</strong> parameter from the HTTP request.
     *
     * @throws ParameterNotFoundException if the parameter was not specified
     */
    public static Integer getRoleId(HttpServletRequest request) throws ParameterNotFoundException {
        String roleId = request.getParameter(Constants.ROLE_PARAM);
        if ((roleId == null) || roleId.equals("")) {
            throw new ParameterNotFoundException("role id not found");
        }

        return new Integer(roleId);
    }

    /**
     * Retrieve the <code>Resource</code> value of the <strong>RESOURCE_ATTR</strong> parameter from the HTTP request.
     * If the resource is not cached in the request, set a user error and return <code>null</code>.
     */
    public static Resource getResource(HttpServletRequest request) {
        Resource resource = (Resource) request.getAttribute(Constants.RESOURCE_ATTR);

        // This has been commented out because there are still lots of
        // calls being made to getResource() when we're in an
        // auto-group.  Furthermore, platform auto-groups have no
        // parent resource, so we will always get this "error", even
        // though it's not an error.
        /*
         * if (resource == null) { setError(request, Constants.ERR_RESOURCE_NOT_FOUND); }
         */
        return resource;
    }

    /**
     * Set the <code>Resource</code> value in the <strong>RESOURCE_ATTR</strong> parameter of the HTTP request.
     */
    public static void setResource(HttpServletRequest request, Resource resource) {
        request.setAttribute(Constants.RESOURCE_ATTR, resource);
    }

    /**
     * Set the <code>Resource</code> value in the <strong>RESOURCE_ATTR</strong> parameter of the HTTP request.
     */
    public static void setResourceType(HttpServletRequest request, ResourceType resourceType) {
        request.setAttribute(Constants.RESOURCE_TYPE_ATTR, resourceType);
    }

    /**
     * Retrieve the <code>Integer</code> value of the <strong>RESOURCE_PARAM</strong> parameter from the HTTP request.
     *
     * @throws ParameterNotFoundException if the parameter was not specified
     */
    public static Integer getGroupId(HttpServletRequest request) throws ParameterNotFoundException {
        String result = request.getParameter(HubConstants.PARAM_GROUP_ID);
        return (result == null) ? null : Integer.valueOf(result);
    }

    public static GroupCategory getGroupCategory(HttpServletRequest request) throws ParameterNotFoundException {
        String category = request.getParameter(HubConstants.PARAM_GROUP_CATEGORY);
        if ((category == null) || category.trim().equals("")) {
            return null;
        }

        return GroupCategory.valueOf(category.toUpperCase());
    }

    public static void setGroupCategory(HttpServletRequest request, GroupCategory category) {
        request.setAttribute(HubConstants.PARAM_GROUP_CATEGORY, category.name());
    }

    public static ResourceGroup getResourceGroup(HttpServletRequest request) {
        return (ResourceGroup) request.getAttribute(HubConstants.PARAM_GROUP);
    }

    public static void setResourceGroup(HttpServletRequest request, ResourceGroup resourceGroup) {
        request.setAttribute(HubConstants.PARAM_GROUP, resourceGroup);
    }

    public static Resource getRes(HttpServletRequest request) {
        return (Resource) request.getAttribute("resource");
    }

    public static void setRes(HttpServletRequest request, Resource resource) {
        request.setAttribute("resource", resource);
    }

    public static void setErrorWithNullCheck(HttpServletRequest request, Exception e, String nullMsg, String regularMsg) {
        try {
            if (e.getMessage().equals("null")) {
                RequestUtils.setError(request, nullMsg);
            } else {
                RequestUtils.setErrorObject(request, regularMsg, e.getMessage());
            }
        } catch (Exception npe) {
            RequestUtils.setError(request, nullMsg);
        }
    }

    /**
     * Retrieve the <code>Integer</code> value of the <strong>RESOURCE_ID_PARAM</strong> parameter from the HTTP
     * request.
     *
     * @throws ParameterNotFoundException if the parameter was not specified
     */
    public static Integer getResourceId(HttpServletRequest request) throws ParameterNotFoundException {
        String resourceId = request.getParameter(ParamConstants.RESOURCE_ID_PARAM);

        // TODO (ips): Return -1 here instead if id param is not specified, since I'm going to use 0 to indicate Availability.
        return ((resourceId == null) || resourceId.equals("")) ? 0 : Integer.valueOf(resourceId);
    }

    /**
     * Retrieve the <code>Integer</code> values of the <strong>RESOURCE_ID_PARAM</strong> parameter from the HTTP
     * request.
     *
     * @throws ParameterNotFoundException if the parameter was not specified
     */
    public static Integer[] getResourceIds(HttpServletRequest request) throws ParameterNotFoundException {
        String[] resourceIdStrings = request.getParameterValues(ParamConstants.RESOURCE_ID_PARAM);
        Integer[] resourceIds = ArrayUtil.stringToInteger(resourceIdStrings);
        return resourceIds;
    }

    /**
     * Retrieve the <code>Integer</code> value of the <strong>SCHEDULE_PARAM</strong> parameter from the HTTP request.
     *
     * @throws ParameterNotFoundException if the parameter was not specified
     */
    public static Integer getScheduleId(HttpServletRequest request) throws ParameterNotFoundException {
        String scheduleId = request.getParameter(Constants.SCHEDULE_PARAM);
        if ((scheduleId == null) || scheduleId.equals("")) {
            throw new ParameterNotFoundException("schedule id not found");
        }

        return new Integer(scheduleId);
    }

    /**
     * Retrieve the <code>Integer</code> value of the <strong>RESOURCE_TYPE_ID_PARAM</strong> parameter from the HTTP
     * request.
     *
     * @throws ParameterNotFoundException if the parameter was not specified
     */
    public static Integer getResourceTypeId(HttpServletRequest request) throws ParameterNotFoundException {
        String resourceTypeId = request.getParameter(Constants.RESOURCE_TYPE_ID_PARAM);
        if ((resourceTypeId == null) || resourceTypeId.equals("")) {
            throw new ParameterNotFoundException("resource type id not found");
        }

        return new Integer(resourceTypeId);
    }

    public static ResourceType getResourceType(HttpServletRequest request) {
        try {
            Integer resourceTypeId = getResourceTypeId(request);
            Subject subject = RequestUtils.getSubject(request);
            ResourceTypeManagerLocal typeManager = LookupUtil.getResourceTypeManager();

            ResourceType resourceType = typeManager.getResourceTypeById(subject, resourceTypeId);

            setResourceType(request, resourceType);

            return resourceType;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Retrieve the <code>Integer</code> value of the <strong>AUTOGROUP_TYPE_ID_PARAM</strong> parameter from the HTTP
     * request.
     *
     * @throws ParameterNotFoundException if the parameter was not specified
     */
    public static Integer getAutogroupResourceTypeId(HttpServletRequest request) throws ParameterNotFoundException {
        String autogrouptypeId = request.getParameter(Constants.AUTOGROUP_TYPE_ID_PARAM);
        if ((autogrouptypeId == null) || autogrouptypeId.equals("")) {
            throw new ParameterNotFoundException("autogroup resource type id not found");
        }

        return new Integer(autogrouptypeId);
    }

    /**
     * Retrieve the <code>Integer</code> value of the <strong>USER_PARAM</strong> parameter from the HTTP request.
     *
     * @throws ParameterNotFoundException if the parameter was not specified
     */
    public static Integer getUserId(HttpServletRequest request) throws ParameterNotFoundException {
        String userId = request.getParameter(Constants.USER_PARAM);
        if ((userId == null) || userId.equals("")) {
            throw new ParameterNotFoundException("user id not found");
        }

        return new Integer(userId);
    }

    /**
     * Retrieve the <code>Integer</code> value of the <strong>METRIC_BASELINE_PARAM</strong> parameter from the HTTP
     * request.
     *
     * @throws ParameterNotFoundException if the parameter was not specified
     */
    public static Integer getMetricId(HttpServletRequest request) throws ParameterNotFoundException {
        String metricId = request.getParameter(Constants.METRIC_ID_PARAM);
        if ((metricId == null) || metricId.equals("")) {
            throw new ParameterNotFoundException("metric baseline param not found");
        }

        return new Integer(metricId);
    }

    /**
     * Retrieve the <code>String</code> value of the <strong>URL_ATTR</strong> parameter from the HTTP request.
     *
     * @throws ParameterNotFoundException if the parameter was not specified
     */
    public static String getUrl(HttpServletRequest request) {
        String url = request.getParameter(Constants.URL_PARAM);
        if ((url == null) || url.equals("")) {
            throw new ParameterNotFoundException(Constants.URL_PARAM);
        }

        return url;
    }

    /**
     * Retrieve the <code>int</code> value of the <code>Constants.PAGENUM_PARAM</code> request parameter, or <code>
     * Constants.PAGENUM_DEFAULT</code> if the parameter was not found or not specified as an integer.
     */
    public static int getPageNum(HttpServletRequest request, String param) {
        Integer pn = null;
        String val = request.getParameter(param);
        if ((val != null) && (val.length() > 0)) {
            try {
                pn = new Integer(val);
            } catch (Exception e) {
            }
        }

        if (pn == null) {
            pn = Constants.PAGENUM_DEFAULT;
        }

        return pn;
    }

    /**
     * Retrieve the <code>int</code> value of the <code>Constants.PAGESIZE_PARAM</code> request parameter, or <code>
     * Constants.PAGESIZE_DEFAULT</code> if the parameter was not found or not specified as an integer.
     */
    public static int getPageSize(HttpServletRequest request, String param) {
        Integer ps = null;
        String val = request.getParameter(param);
        if ((val != null) && (val.length() > 0)) {
            try {
                ps = new Integer(val);
            } catch (Exception e) {
            }
        }

        if ((ps == null) || (ps == 0)) {
            ps = Constants.PAGESIZE_DEFAULT;
        }

        return ps;
    }

    /**
     * Set a confirmation message upon completion of a user action.
     *
     * @param key the message resource key
     */
    public static void setConfirmation(HttpServletRequest request, String key) {
        ActionMessage msg = new ActionMessage(key);
        ActionMessages msgs = new ActionMessages();
        msgs.add(ActionMessages.GLOBAL_MESSAGE, msg);
        request.setAttribute(Globals.MESSAGE_KEY, msgs);
    }

    /**
     * Set a confirmation message with a replacement value upon completion of a user action.
     *
     * @param key    the message resource key
     * @param value0 the replacement value
     */
    public static void setConfirmation(HttpServletRequest request, String key, Object value0) {
        ActionMessage msg = new ActionMessage(key, value0);
        ActionMessages msgs = new ActionMessages();
        msgs.add(ActionMessages.GLOBAL_MESSAGE, msg);
        request.setAttribute(Globals.MESSAGE_KEY, msgs);
    }

    /**
     * Set an error message when a user action fails with a user-level error.
     *
     * @param key the message resource key
     */
    public static void setError(HttpServletRequest request, String key) {
        setError(request, key, ActionMessages.GLOBAL_MESSAGE);
    }

    /**
     * Set an error message when a user action fails with a user-level error.
     *
     * @param key      the message resource key
     * @param property the form property for which the error occurred
     */
    public static void setError(HttpServletRequest request, String key, String property) {
        ActionMessage err = new ActionMessage(key);
        setError(request, err, property);
    }

    /**
     * Set an error message when a user action fails with a user-level error.
     *
     * @param key      the message resource key
     * @param property the form property for which the error occurred
     */
    public static void setErrorObject(HttpServletRequest request, String key, String object) {
        ActionMessage err = new ActionMessage(key, object);
        setError(request, err, ActionMessages.GLOBAL_MESSAGE);
    }

    public static void setError(HttpServletRequest request, ActionMessage msg, String property) {
        ActionErrors errs = new ActionErrors();
        errs.add(property, msg);
        request.setAttribute(Globals.ERROR_KEY, errs);
    }

    /**
     * sets an ActionErrors object into the request object.
     *
     * <p/>typically this api is used when an action class builds up a list of ActionError objects.
     *
     * <p/>Current use case is building of the ConfigOptions before saving into bizapp layer.
     */
    public static void setErrors(HttpServletRequest request, ActionErrors errs) {
        request.setAttribute(Globals.ERROR_KEY, errs);
    }

    /**
     * Set an error message with a replacement value when a user action fails with a user-level error.
     *
     * @param key    the message resource key
     * @param value0 the replacement value
     */
    public static void setError(HttpServletRequest request, String key, Object value0) {
        setError(request, key, value0, ActionMessages.GLOBAL_MESSAGE);
    }

    /**
     * Set an error message with a replacement value when a user action fails with a user-level error.
     *
     * @param key      the message resource key
     * @param value0   the replacement value
     * @param property the form property for which the error occurred
     */
    public static void setError(HttpServletRequest request, String key, Object value0, String property) {
        ActionMessage err = new ActionMessage(key, value0);
        ActionMessages errs = new ActionErrors();
        errs.add(property, err);
        request.setAttribute(Globals.ERROR_KEY, errs);
    }

    /**
     * Examine the request to see if an "cancel" button was clicked on the previous page. If so, one of the <code>
     * Constants.CANCEL_PARAM</code> or <code>Constants.CANCEL_X_PARAM</code> parameters will exist.
     */
    public static boolean isCancelClicked(HttpServletRequest request) {
        return ((request.getParameter(Constants.CANCEL_PARAM) != null) || (request
            .getParameter(Constants.CANCEL_X_PARAM) != null));
    }

    /**
     * Examine the request to see if an "ok" button was clicked on the previous page. If so, one of the <code>
     * Constants.OK_PARAM</code> or <code>Constants.OK_X_PARAM</code> parameters will exist.
     */
    public static boolean isOkClicked(HttpServletRequest request) {
        return ((request.getParameter(Constants.OK_PARAM) != null) || (request.getParameter(Constants.OK_X_PARAM) != null));
    }

    /**
     * Examine the request to see if an "okassign" button was clicked on the previous page. If so, one of the <code>
     * Constants.OK_ASSIGN_PARAM</code> or <code>Constants.OK_ASSIGN_X_PARAM</code> parameters will exist.
     */
    public static boolean isOkAssignClicked(HttpServletRequest request) {
        return ((request.getParameter(Constants.OK_ASSIGN_PARAM) != null) || (request
            .getParameter(Constants.OK_ASSIGN_X_PARAM) != null));
    }

    /**
     * Examine the request to see if an "reset" button was clicked on the previous page. If so, one of the <code>
     * Constants.RESET_PARAM</code> or <code>Constants.RESET_X_PARAM</code> parameters will exist.
     */
    public static boolean isResetClicked(HttpServletRequest request) {
        return ((request.getParameter(Constants.RESET_PARAM) != null) || (request.getParameter(Constants.RESET_X_PARAM) != null));
    }

    /**
     * Examine the request to see if an "new" button was clicked on the previous page. If so, one of the <code>
     * Constants.NEW_PARAM</code> or <code>Constants.NEW_X_PARAM</code> parameters will exist.
     */
    public static boolean isNewClicked(HttpServletRequest request) {
        return ((request.getParameter(Constants.NEW_PARAM) != null) || (request.getParameter(Constants.NEW_X_PARAM) != null));
    }

    /**
     * Examine the request to see if an "edit" button was clicked on the previous page. If so, one of the <code>
     * Constants.EDIT_PARAM</code> or <code>Constants.EDIT_X_PARAM</code> parameters will exist.
     */
    public static boolean isEditClicked(HttpServletRequest request) {
        return ((request.getParameter(Constants.EDIT_PARAM) != null) || (request.getParameter(Constants.EDIT_X_PARAM) != null));
    }

    /**
     * Examine the request to see if an "add" button was clicked on the previous page. If so, one of the <code>
     * Constants.ADD_PARAM</code> or <code>Constants.ADD_X_PARAM</code> parameters will exist.
     */
    public static boolean isAddClicked(HttpServletRequest request) {
        return ((request.getParameter(Constants.ADD_PARAM) != null) || (request.getParameter(Constants.ADD_X_PARAM) != null));
    }

    /**
     * Examine the request to see if a "remove" button was clicked on the previous page. If so, one of the <code>
     * Constants.REMOVE_PARAM</code> or <code>Constants.REMOVE_X_PARAM</code> parameters will exist.
     */
    public static boolean isRemoveClicked(HttpServletRequest request) {
        return ((request.getParameter(Constants.REMOVE_PARAM) != null) || (request
            .getParameter(Constants.REMOVE_X_PARAM) != null));
    }

    /**
     * Propogate a request parameter.
     *
     * @param params the parameters passed to the ActionForward
     * @param param  the parameter name to propogate
     */
    public static void propogateParam(HttpServletRequest request, Map params, String param) {
        Object paramValue = request.getParameter(param);
        log.debug("Propogating " + param + ": " + paramValue);
        if ((paramValue != null) && !"".equals(paramValue)) {
            params.put(param, paramValue);
        }
    }

    /**
     * Get an i18n message from the application resource bundle.
     *
     * @param key the message key we want
     */
    public static String message(HttpServletRequest request, String key) {
        return message(request, null, null, key, null);
    }

    /**
     * Get an i18n message from the application resource bundle.
     *
     * @param key  the message key we want
     * @param args the positional parameters for the message
     */
    public static String message(HttpServletRequest request, String key, Object[] args) {
        return message(request, null, null, key, args);
    }

    /**
     * Get an i18n message from the application resource bundle.
     *
     * @param bundle the resource bundle name
     * @param bundle the user locale
     * @param key    the message key we want
     */
    public static String message(HttpServletRequest request, String bundle, String locale, String key) {
        return message(request, bundle, locale, key, null);
    }

    /**
     * Get an i18n message from the application resource bundle.
     *
     * @param bundle the resource bundle name
     * @param bundle the user locale
     * @param key    the message key we want
     * @param args   the positional parameters for the message
     */
    public static String message(HttpServletRequest request, String bundle, String locale, String key, Object[] args) {
        if (null == bundle) {
            bundle = org.apache.struts.Globals.MESSAGES_KEY;
        }

        if (null == locale) {
            locale = org.apache.struts.Globals.LOCALE_KEY;
        }

        MessageResources resources = (MessageResources) request.getAttribute(bundle);
        if (null == resources) {
            resources = (MessageResources) request.getSession().getAttribute(bundle);
        }

        if (null == resources) {
            resources = (MessageResources) request.getSession().getServletContext().getAttribute(bundle);
        }

        if (null == resources) {
            return "???" + key + "???";
        }

        Locale userLocale = (Locale) request.getSession().getAttribute(locale);
        if (userLocale == null) {
            userLocale = (Locale) request.getAttribute(locale);
        }

        if (args == null) {
            return resources.getMessage(userLocale, key);
        } else {
            return resources.getMessage(userLocale, key, args);
        }
    }

    public static void bustaCache(ServletRequest request, ServletResponse response) {
        bustaCache(request, response, false);
    }

    /**
     * When you really really want to defeat a browser's cache, expire the content and set the no-cache header
     *
     * @param expire set <i>true</i> to have the content expire immediately
     */
    public static void bustaCache(ServletRequest request, ServletResponse response, boolean expire) {
        if (!(response instanceof HttpServletResponse)) {
            return;
        }

        HttpServletResponse res = (HttpServletResponse) response;
        if (request.getProtocol().equalsIgnoreCase("HTTP/1.0")) {
            res.setHeader("Pragma", "no-cache");
        } else if (request.getProtocol().equalsIgnoreCase("HTTP/1.1")) {
            res.setHeader("Cache-Control", "no-cache");
        }

        if (expire) {
            Date now = new Date();
            res.setDateHeader("Date", now.getTime());
            res.setDateHeader("Expires", now.getTime() + 1000);
        }
    }

    /**
     * A development aid.
     *
     * <p/>Since most parameter handling is wrapped by utility methods, quick-n-dirty debugging of what data is actually
     * passed in a request is sometimes necessary. Obviously, you wouldn't want this in production.
     *
     * @param out  output the parameter dump to a PrintWriter
     * @param html formats the dump with &lt;ol&gt; if true
     */
    public static void dumpRequestParams(ServletRequest request, PrintWriter out, boolean html) {
        out.println(dumpRequestParamsToString(request, html));
        out.flush();
    }

    public static void dumpRequestParams(ServletRequest request, OutputStream out, boolean html) {
        dumpRequestParams(request, new PrintWriter(out), html);
    }

    public static void dumpRequestParams(ServletRequest request, Log log, boolean html) {
        log.trace(dumpRequestParamsToString(request, html));
    }

    public static String dumpRequestParamsToString(ServletRequest request, boolean html) {
        StringBuffer output = new StringBuffer();
        if (html) {
            output.append("<ol>\n");
        }

        for (Enumeration e = request.getParameterNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            String[] values = request.getParameterValues(key);
            if (html) {
                output.append("<li>");
            }

            output.append("'" + key + "' = ");
            for (int i = 0; i < (values.length - 1); i++) {
                output.append("'" + values[i] + "', ");
            }

            output.append("'" + values[values.length - 1] + "'");
            output.append("\n");
        }

        if (html) {
            output.append("</ol>\n");
        }

        return output.toString();
    }

    public static String dumpRequestAttributesToString(ServletRequest request) {
        StringBuffer output = new StringBuffer();
        for (Enumeration e = request.getAttributeNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            Object value = request.getAttribute(name);
            output.append("name = '" + name + "'\n");
            output.append("type = '" + value.getClass().getCanonicalName() + "'\n");
            output.append("value = '" + value.toString() + "'\n\n");
        }

        return output.toString();
    }

    public static void dumpRequestAttributesToString(ServletRequest request, PrintWriter out) {
        out.println(dumpRequestAttributesToString(request));
        out.flush();
    }

    public static void dumpRequestAttributesToString(ServletRequest request, OutputStream out) {
        dumpRequestAttributesToString(request, new PrintWriter(out));
    }
}