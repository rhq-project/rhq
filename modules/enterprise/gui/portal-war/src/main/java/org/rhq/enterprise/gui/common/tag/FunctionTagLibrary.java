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
package org.rhq.enterprise.gui.common.tag;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.sun.facelets.tag.AbstractTagLibrary;

import org.rhq.core.clientapi.util.units.DateFormatter;
import org.rhq.core.clientapi.util.units.FormattedNumber;
import org.rhq.core.clientapi.util.units.ScaleConstants;
import org.rhq.core.clientapi.util.units.UnitNumber;
import org.rhq.core.clientapi.util.units.UnitsConstants;
import org.rhq.core.clientapi.util.units.UnitsFormat;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.StringConstants;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderPluginManager;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A Facelets tag library containing custom EL functions for use in RHQ GUI pages.
 *
 * *NOTE*: There are weird issues with Facelets function taglibs -
 *         whenever possible, managed beans should be used instead.
 *
 * @author     Ian Springer
 */
public class FunctionTagLibrary extends AbstractTagLibrary {

    private enum ElideMode {
        LEFT, RIGHT, MIDDLE
    }

    public static final String ELLIPSIS = "\u2026";

    /**
     * Namespace used to import this library in Facelets pages
     */
    public static final String NAMESPACE = "http://jboss.org/on/function";

    /**
     * Current instance of library.
     */
    public static final FunctionTagLibrary INSTANCE = new FunctionTagLibrary();

    public FunctionTagLibrary() {
        super(NAMESPACE);
        try {
            Method[] methods = FunctionTagLibrary.class.getMethods();
            for (Method method : methods) {
                if (Modifier.isStatic(method.getModifiers())) {
                    this.addFunction(method.getName(), method);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads the Resource with the id specified via the 'id' query string parameter in the current request, and
     * sticks the Resource into the request context as the "Resource" attribute.
     */
    public static void loadResource() {
        EnterpriseFacesContextUtility.getResource();
    }

    /**
     * Returns a <code>ResourceFacets</code> object that represents the facets (measurement, configuration, etc.) that
     * are supported by the resource type with the specified id. Individual facets can be checked easily via EL, e.g.:
     * <code>&lt;c:set var="resourceFacets" value="${onf:getResourceFacets(Resource.resourceType.id)}"/&gt; &lt;c:if
     * test="${resourceFacets.measurement}"&gt; ...</code>
     *
     * @param  resourceTypeId a {@link org.rhq.core.domain.resource.ResourceType} id
     *
     * @return a <code>ResourceFacets</code> object for the resource type with the specified id
     */
    public static ResourceFacets getResourceFacets(int resourceTypeId) {
        ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
        return resourceTypeManager.getResourceFacets(resourceTypeId);

    }

    public static ResourceFacets getFacets() {
        Resource resource = EnterpriseFacesContextUtility.getResource();
        ResourceType resourceType = resource.getResourceType();
        int resourceTypeId = resourceType.getId();
        return getResourceFacets(resourceTypeId);
    }

    /**
     * Returns a <code>ResourcePermission</code> object for the resource associated with the current request. Individual
     * permissions can be checked easily via EL, e.g.: <code>&lt;c:set var="resourcePerm"
     * value="${onf:getResourcePermission()}"/&gt; &lt;c:if test="${resourcePerm.measure}"&gt; ...</code>
     *
     * @return a <code>ResourcePermission</code> object for the resource with the specified id
     */
    public static ResourcePermission getResourcePermission() {
        AuthorizationManagerLocal authorizationManager = LookupUtil.getAuthorizationManager();
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Resource resource = EnterpriseFacesContextUtility.getResource();
        Set<Permission> resourcePerms = authorizationManager.getImplicitResourcePermissions(subject, resource.getId());
        return new ResourcePermission(resourcePerms);
    }

    /**
     * Returns a <code>ResourcePermission</code> object for the group with the specified id. Individual permissions can
     * be checked easily via EL, e.g.: <code>&lt;c:set var="groupPerm" value="${onf:getGroupPermission(groupId)}"/&gt;
     * &lt;c:if test="${groupPerm.measure}"&gt; ...</code>
     *
     * @param  groupId a {@link ResourceGroup} id
     *
     * @return a <code>ResourcePermission</code> object for the group with the specified id
     */
    public static ResourcePermission getGroupPermission(int groupId) {
        AuthorizationManagerLocal authorizationManager = LookupUtil.getAuthorizationManager();
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Set<Permission> groupPerms = authorizationManager.getImplicitGroupPermissions(subject, groupId);
        return new ResourcePermission(groupPerms);
    }

    /**
     * Formats the specified milliseconds-since-epoch timestamp as a String.
     *
     * @param      timestamp a milliseconds-since-epoch timestamp
     *
     * @return     a String representation of the timestamp
     *
     * @deprecated use <code>f:convertDateTime</code> tag instead
     */
    @Deprecated
    public static String formatTimestamp(long timestamp) {
        // NOTE: The below code was snarfed from org.rhq.enterprise.gui.legacy.taglib.DateFormatter.

        UnitsConstants unit = UnitsConstants.UNIT_DATE;
        String key = StringConstants.UNIT_FORMAT_PREFIX_KEY + "epoch-millis";
        String formatString = RequestUtils.message(FacesContextUtility.getRequest(), key);
        DateFormatter.DateSpecifics specs = new DateFormatter.DateSpecifics();
        specs.setDateFormat(new SimpleDateFormat(formatString));
        FormattedNumber fmtd = UnitsFormat.format(new UnitNumber(timestamp, unit, ScaleConstants.SCALE_MILLI),
            FacesContextUtility.getRequest().getLocale(), specs);
        return fmtd.toString();
    }

    public static int sizeOf(Collection<?> collection) {
        if (collection == null) {
            return 0;
        }
        return collection.size();
    }

    public static <T> int length(T[] collection) {
        if (collection == null) {
            return 0;
        }
        return collection.length;
    }

    public static WebUserPreferences getWebUserPreferences() {
        return EnterpriseFacesContextUtility.getWebUser().getWebPreferences();
    }

    public static String contextFragmentURL() {
        EntityContext context = WebUtility.getEntityContext();
        switch (context.type) {
        case Resource:
            return ParamConstants.RESOURCE_ID_PARAM + "=" + String.valueOf(context.resourceId);
        case ResourceGroup:
            return ParamConstants.GROUP_ID_PARAM + "=" + String.valueOf(context.groupId);
        case AutoGroup:
            return ParamConstants.PARENT_RESOURCE_ID_PARAM + "=" + String.valueOf(context.parentResourceId) + "&"
                + ParamConstants.RESOURCE_TYPE_ID_PARAM + "=" + String.valueOf(context.resourceTypeId);
        default:
            throw new IllegalArgumentException(context.getUnknownContextMessage());
        }
    }

    /**
     * This method is akin to {@link #contextFragmentURL()} but produces a correct fragment
     * for the indicators chart action (/resource/common/monitor/visibility/IndicatorCharts.do).
     *
     * This legacy struts action expects a "ctype" parameter where the new UI uses "type".
     *
     * @return context fragment of the URL based on the current entity.
     */
    public static String contextFragmentURLForIndicatorsChart() {
        EntityContext context = WebUtility.getEntityContext();
        switch (context.type) {
        case AutoGroup:
            return ParamConstants.PARENT_RESOURCE_ID_PARAM + "=" + String.valueOf(context.parentResourceId) + "&"
                + ParamConstants.CHILD_RESOURCE_TYPE_ID_PARAM + "=" + String.valueOf(context.resourceTypeId);
        default:
            return contextFragmentURL();
        }
    }

    public static Resource getResource(int resourceId) {
        Subject user = EnterpriseFacesContextUtility.getSubject();
        return LookupUtil.getResourceManager().getResourceById(user, resourceId);
    }

    /**
     * Elides given string using an ellipsis character.
     *
     * The mode is one of "left", "right", "middle" (case insensitive).
     *
     * @see #elideStringCustom(String, int, String, String)
     */
    public static String elideString(String str, int numChars, String mode) {
        return elideStringCustom(str, numChars, mode, ELLIPSIS);
    }

    /**
     * Elides given string using the specified ellipsis.
     * The mode is one of "left", "right", "middle" (case insensitive).
     * The resulting string has at most numChars characters.
     *
     * @param str the string to elide
     * @param numChars the length of the elided string
     * @param mode the elide mode
     * @param ellipsis the ellipsis string
     * @return the elided string
     */
    public static String elideStringCustom(String str, int numChars, String mode, String ellipsis) {
        ElideMode eMode = Enum.valueOf(ElideMode.class, mode.toUpperCase());
        return elideString(str, numChars, eMode, ellipsis);
    }

    private static String elideString(String str, int numChars, ElideMode mode, String ellipsis) {
        if (str == null) {
            throw new IllegalArgumentException("Cannot elide a null string");
        }

        if (ellipsis == null) {
            throw new IllegalArgumentException("Ellipsis can't be null when eliding a string");
        }

        if (numChars >= str.length()) {
            return str;
        }

        int ellipsisLength = ellipsis.length();

        StringBuilder result = new StringBuilder(numChars);

        if (mode == ElideMode.LEFT) {
            result.append(ellipsis);
            result.append(str.substring(str.length() - numChars - ellipsisLength + 1, str.length()));
        } else if (mode == ElideMode.MIDDLE) {
            int firstHalf = (numChars - ellipsisLength) / 2;
            int secondHalf = firstHalf + ((numChars - ellipsisLength) % 2);

            result.append(str.substring(0, firstHalf));
            result.append(ellipsis);
            result.append(str.substring(str.length() - secondHalf + 1, str.length()));
        } else if (mode == ElideMode.RIGHT) {
            result.append(str.substring(0, numChars - ellipsisLength + 1));
            result.append(ellipsis);
        }

        return result.toString();
    }

    private final static String AMP = "&amp;";

    public static String getChartURLParams(MetricDisplaySummary summary) {
        StringBuilder buffer = new StringBuilder();

        buffer.append(contextFragmentURL()).append(AMP);
        buffer.append("imageWidth=647").append(AMP);
        buffer.append("imageHeight=100").append(AMP);
        buffer.append("schedId=").append(formatNullURLParams(summary.getScheduleId())).append(AMP);
        buffer.append("definitionId=").append(formatNullURLParams(summary.getDefinitionId())).append(AMP);
        buffer.append("measurementUnits=").append(formatNullURLParams(summary.getUnits())).append(AMP);
        buffer.append("now=").append(System.currentTimeMillis());

        return buffer.toString();
    }

    public static <T> List<T> getListFromMap(Map<String, T> map) {
        List<T> results = new ArrayList<T>();
        results.addAll(map.values());
        return results;
    }

    // either the object itself or its corresponding string representation can be null - handle both
    private static String formatNullURLParams(Object value) {
        String valueStr = (value == null ? null : value.toString());
        return (valueStr == null ? "" : valueStr);
    }

    public static String getDefaultContextTabURL(EntityContext context) {
        if (context.type == EntityContext.Type.Resource) {
            return getDefaultResourceTabURL();
        } else if (context.type == EntityContext.Type.ResourceGroup) {
            return "/rhq/group/monitor/graphs.xhtml";
        } else if (context.type == EntityContext.Type.AutoGroup) {
            return "/rhq/autogroup/monitor/graphs.xhtml";
        } else {
            throw new IllegalArgumentException("Do not support getting defaultTabURL for " + context);
        }
    }

    // needs to exist separately from getDefaultContextTabURL because only some facelets understand EntityContext
    public static String getDefaultResourceTabURL() {
        return "/rhq/resource/summary/overview.xhtml";
    }

    public static String getDefaultGroupTabURL() {
        return "/rhq/group/inventory/view.xhtml";
    }

    public static String getDefaultAutoGroupTabURL() {
        return "/rhq/autogroup/monitor/graphs.xhtml";
    }

    public static String getAvailabilityURL(AvailabilityType type, int size) {
        if (size != 16 && size != 24) {
            throw new IllegalArgumentException("No availability icon for size " + size);
        }
        if (type == null) {
            return (size == 16) ? "/portal/images/icons/availability_grey_16.png"
                : "/portal/images/icons/availability_grey_24.png";
        } else if (type == AvailabilityType.UP) {
            return (size == 16) ? "/portal/images/icons/availability_green_16.png"
                : "/portal/images/icons/availability_green_24.png";
        } else if (type == AvailabilityType.DOWN) {
            return (size == 16) ? "/portal/images/icons/availability_red_16.png"
                : "/portal/images/icons/availability_red_24.png";
        } else {
            throw new IllegalArgumentException("No icon for AvailabilityType[" + type + "]");
        }
    }

    public static String getAlertPriorityURL(AlertPriority priority) {
        switch (priority) {
        case HIGH:
            return "/portal/images/icons/Flag_red_16.png";
        case MEDIUM:
            return "/portal/images/icons/Flag_yellow_16.png";
        case LOW:
            return "/portal/images/icons/Flag_blue_16.png";
        default:
            throw new IllegalArgumentException("No icon for AlertPriority[" + priority + "]");
        }
    }

    public static String getResourceConfigStatusURL(ConfigurationUpdateStatus status) {
        switch (status) {
        case SUCCESS:
            return "/portal/images/icons/Configure_ok_16.png";
        case FAILURE:
            return "/portal/images/icons/Configure_failed_16.png";
        case INPROGRESS:
            return "/portal/images/icons/Configure_16.png";
        default:
            throw new IllegalArgumentException("No icon for ConfigurationUpdateStatus[" + status + "]");
        }
    }

    public static String getOperationStatusURL(OperationRequestStatus status) {
        switch (status) {
        case SUCCESS:
            return "/portal/images/icons/Operation_ok_16.png";
        case FAILURE:
            return "/portal/images/icons/Operation_failed_16.png";
        case INPROGRESS:
            return "/portal/images/icons/Operation_16.png";
        case CANCELED:
            return "/portal/images/icons/Operation_cancel_16.png";
        default:
            throw new IllegalArgumentException("No icon for OperationRequestStatus[" + status + "]");
        }
    }

    public static String getEventSeverityURL(EventSeverity severity, boolean grouped) {
        String color = null;
        switch (severity) {
        case DEBUG:
            color = "debug";
            break;
        case INFO:
            color = "info";
            break;
        case WARN:
            color = "warning";
            break;
        case ERROR:
            color = "error";
            break;
        case FATAL:
            color = "fatal";
            break;
        default:
            throw new IllegalArgumentException("No icon for EventSeverity[" + severity + "]");
        }

        String additional = (grouped ? "_multi" : "");
        return "/portal/images/icons/Events_" + color + additional + "_16.png";
    }

    public static String getAlertSenderConfigurationPreview(AlertNotification alertNotification) {
        AlertSenderPluginManager alertSenderPluginManager = LookupUtil.getAlertManager().getAlertPluginManager();
        AlertSender sender = alertSenderPluginManager.getAlertSenderForNotification(alertNotification);
        return sender.previewConfiguration();
    }

    public static boolean isIE6() {
        HttpServletRequest request = FacesContextUtility.getRequest();
        String userAgent = request.getHeader("User-Agent").toLowerCase();
        return (userAgent.indexOf("msie") != -1 && userAgent.indexOf("6.0") != -1);
    }
}