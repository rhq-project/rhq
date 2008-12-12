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
package org.rhq.enterprise.gui.util;

import java.util.Arrays;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.legacy.DefaultConstants;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.exception.ParameterNotFoundException;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.common.EntityContext;

/**
 * Utilities for the web tier. Named such so as not to clash with
 * {@link org.rhq.enterprise.gui.legacy.util.RequestUtils}.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public abstract class WebUtility {
    private static final Log LOG = LogFactory.getLog(WebUtility.class.getName());

    public static Integer getResourceId(ServletRequest request) {
        String resourceIdString = request.getParameter(ParamConstants.RESOURCE_ID_PARAM);
        Integer resourceId = null;
        if (resourceIdString != null) {
            resourceId = Integer.parseInt(resourceIdString);
        }

        return resourceId;
    }

    public static Integer[] getResourceIds(ServletRequest request) {
        String[] resourceIdStrings = request.getParameterValues(ParamConstants.RESOURCE_ID_PARAM);
        Integer[] ids;
        if (resourceIdStrings == null) {
            ids = new Integer[0];
        } else {
            ids = new Integer[resourceIdStrings.length];
            for (int i = 0; i < resourceIdStrings.length; i++) {
                ids[i] = Integer.parseInt(resourceIdStrings[i]);
            }
        }

        return ids;
    }

    public static PageControl getPageControl(ServletRequest request) {
        return getPageControl(request, null);
    }

    public static PageControl getPageControl(ServletRequest request, String postfix) {
        // TODO GH: Add a mechanism to persist these settings in user preferences and default from there
        if (postfix == null) {
            postfix = "";
        }

        int pageNumber = getOptionalIntRequestParameter(request, ParamConstants.PAGENUM_PARAM + postfix,
            DefaultConstants.PAGENUM_DEFAULT);
        int pageSize = getOptionalIntRequestParameter(request, ParamConstants.PAGESIZE_PARAM + postfix,
            DefaultConstants.PAGESIZE_DEFAULT);

        // Make sure the user doesn't go around us and create huge pages
        pageSize = Math.min(pageSize, PageControl.SIZE_MAX);

        String sortOrderString = request.getParameter(ParamConstants.SORTORDER_PARAM + postfix);
        PageOrdering sortOrder = PageOrdering.ASC;
        if ((sortOrderString != null) && (sortOrderString.length() > 0)) {
            try {
                sortOrder = PageOrdering.valueOf(sortOrderString.toUpperCase());
            } catch (RuntimeException e) {
                throw new RuntimeException("Request parameter '" + ParamConstants.SORTORDER_PARAM + postfix
                    + "' has an invalid value ('" + sortOrderString
                    + "') - valid values are 'asc' or 'desc' (case-insensitive).");
            }
        }

        String sortColumn = getOptionalRequestParameter(request, ParamConstants.SORTCOL_PARAM + postfix, null);
        if ((sortColumn != null) && sortColumn.contains(" ")) {
            throw new RuntimeException("This app is not vulnerable to SQL-injection attacks. Thanks for playing.");
        }

        return new PageControl(pageNumber, pageSize, new OrderingField(sortColumn, sortOrder));
    }

    public static int getResourceTypeId(HttpServletRequest request) throws ParameterNotFoundException {
        return getRequiredIntRequestParameter(request, ParamConstants.RESOURCE_TYPE_ID_PARAM);
    }

    public static int getChildResourceTypeId(HttpServletRequest request) throws ParameterNotFoundException {
        return getRequiredIntRequestParameter(request, ParamConstants.CHILD_RESOURCE_TYPE_ID_PARAM);
    }

    public static String getRequiredRequestParameter(ServletRequest request, String name) {
        logWarningIfParameterHasMultipleValues(request, name);
        String value = request.getParameter(name);
        if (value == null) {
            throw new ParameterNotFoundException("Required request parameter '" + name + "' does not exist.");
        }

        return value;
    }

    public static String getOptionalRequestParameter(ServletRequest request, String name, String defaultValue) {
        logWarningIfParameterHasMultipleValues(request, name);
        String value = request.getParameter(name);
        return (value == null) ? defaultValue : value;
    }

    public static int getRequiredIntRequestParameter(ServletRequest request, String name) {
        String value = getRequiredRequestParameter(request, name);
        if (value.length() == 0) {
            throw new ParameterNotFoundException("Required request parameter '" + name + "' has an empty value.");
        }

        int intValue;
        try {
            intValue = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ParameterNotFoundException("Request parameter '" + name + "' has value '" + value
                + "', which is not a valid integer.");
        }

        return intValue;
    }

    public static int getOptionalIntRequestParameter(ServletRequest request, String name, int defaultValue) {
        String value = request.getParameter(name);
        return ((value != null) && (value.length() > 0)) ? Integer.parseInt(value) : defaultValue;
    }

    private static void logWarningIfParameterHasMultipleValues(ServletRequest request, String name) {
        if ((request.getParameterValues(name) != null) && (request.getParameterValues(name).length > 1)) {
            LOG.warn("More than one '" + name + "' request parameter exists - exactly one is required (values are "
                + Arrays.asList(request.getParameterValues(name)) + ").");
        }
    }

    /**
     * Return the subject of the current request
     *
     * @param  request
     *
     * @return
     */
    public static Subject getSubject(HttpServletRequest request) {
        WebUser user = SessionUtils.getWebUser(request.getSession());
        Subject subject = user.getSubject();
        return subject;
    }

    /**
     * Return the current {@link MetricsDisplayMode} for this request. We need this to as actions often need to call
     * different code depending on the kind of metrics (single resource, compatible group, autogroup or definitions)
     * they are working on
     *
     * @param  request
     *
     * @return
     */
    public static MetricsDisplayMode getMetricsDisplayMode(HttpServletRequest request) {
        int resourceId = getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_ID_PARAM, -1);
        int groupId = getOptionalIntRequestParameter(request, ParamConstants.GROUP_ID_PARAM, -1);
        int resourceTypeId = getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_TYPE_ID_PARAM, -1);

        int parent = getOptionalIntRequestParameter(request, "parent", -1);

        // Find out what we want to configure
        MetricsDisplayMode mode;
        if (groupId > 0) {
            mode = MetricsDisplayMode.COMPGROUP;
        } else if ((resourceTypeId > 0) && (parent == -1)) {
            mode = MetricsDisplayMode.RESOURCE_DEFAULT;
        } else if (resourceId > 0) {
            mode = MetricsDisplayMode.RESOURCE;
        } else if ((resourceTypeId > 0) && (parent > 0)) {
            mode = MetricsDisplayMode.AUTOGROUP;
        } else {
            mode = MetricsDisplayMode.UNSET;
            if (LOG.isDebugEnabled())
                LOG.debug("Unknown input parameter combination - we can't go on");
        }

        return mode;
    }

    public static EntityContext getEntityContext() {
        HttpServletRequest request = FacesContextUtility.getRequest();
        return getEntityContext(request);
    }

    public static EntityContext getEntityContext(HttpServletRequest request) {
        int resourceId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_ID_PARAM, -1);
        int groupId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.GROUP_ID_PARAM, -1);
        int parentResourceId = WebUtility.getOptionalIntRequestParameter(request, "parent", -1);
        int resourceTypeId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_TYPE_ID_PARAM,
            -1);

        return new EntityContext(resourceId, groupId, parentResourceId, resourceTypeId);
    }
}