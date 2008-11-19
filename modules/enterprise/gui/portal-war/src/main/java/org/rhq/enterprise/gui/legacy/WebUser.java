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
package org.rhq.enterprise.gui.legacy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import org.rhq.core.clientapi.util.StringUtil;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.legacy.util.MonitorUtils;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A representation of the person currently interacting with the application.
 * This essentially wraps a {@link Subject} and assigns that subject its
 * {@link Subject#setSessionId(Integer) session ID}.  Instances of this object
 * are placed in HTTP session.
 */
public class WebUser {
    public static final String PREF_FAV_RESOURCE_METRICS_PREFIX = ".resource.common.monitor.visibility.favoriteMetrics";
    public static final String PREF_METRIC_RANGE = ".resource.common.monitor.visibility.metricRange";
    public static final String PREF_METRIC_RANGE_LASTN = ".resource.common.monitor.visibility.metricRange.lastN";
    public static final String PREF_METRIC_RANGE_UNIT = ".resource.common.monitor.visibility.metricRange.unit";
    public static final String PREF_METRIC_RANGE_RO = ".resource.common.monitor.visibility.metricRange.ro";
    public static final String PREF_METRIC_THRESHOLD = ".resource.common.monitor.visibility.metricThreshold";
    public static final String PREF_PAGE_REFRESH_PERIOD = ".page.refresh.period";

    /** delimiter for preferences that are multi-valued and stringified */
    private static final String PREF_LIST_DELIM = ",";

    private final Log log = LogFactory.getLog(this.getClass());

    private Subject subject;
    private String password;

    /**
     * Indicates whether or not the user has an entry in the principals table.
     * If true, it means this user was or can be authenticated via the JDBC login module.
     * If false, it means this user is to be authenticated via LDAP.
     */
    private boolean hasPrincipal;

    public WebUser() {
        this(null);
    }

    public WebUser(Subject subject) {
        this.subject = subject;
        this.hasPrincipal = false;
    }

    public WebUser(Subject subject, Integer sessionId, String password, boolean hasPrincipal) {
        this.subject = subject;
        this.subject.setSessionId(sessionId);
        this.setPassword(password);
        this.hasPrincipal = hasPrincipal;
    }

    /**
     * Returns this web user's {@link Subject}.
     *
     * @return the logged-in user's Subject representation
     */
    public Subject getSubject() {
        return this.subject;
    }

    /**
     * Return the's user's {@link Subject#getId()} or <code>null</code>
     * if this web user is not associated with a particular Subject.
     *
     * @return subject ID or <code>null</code>
     */
    public Integer getId() {
        return (this.subject == null) ? null : this.subject.getId();
    }

    /**
     * Return the session id or <code>null</code> if there is not subject associated with this
     * web user or the session is not known.
     *
     * @return session ID of the currently logged in user, or <code>null</code> if unknown
     */
    public Integer getSessionId() {
        return (this.subject == null) ? null : this.subject.getSessionId();
    }

    /**
     * Set the session id for this web user.  If there is no Subject associated with this web user,
     * an exception is thrown.
     *
     * @param sessionId the new session id
     */
    public void setSessionId(Integer sessionId) {
        if (this.subject == null)
            throw new IllegalStateException("Cannot set a session ID for a web user that has no subject");

        this.subject.setSessionId(sessionId);
    }

    public String getUsername() {
        return (this.subject == null) ? null : this.subject.getName();
    }

    public void setUsername(String username) {
        this.subject.setName(username);
    }

    public String getName() {
        return getUsername();
    }

    public String getSmsaddress() {
        return (this.subject == null) ? null : this.subject.getSmsAddress();
    }

    public void setSmsaddress(String s) {
        this.subject.setSmsAddress(s);
    }

    public String getFirstName() {
        return (this.subject == null) ? null : this.subject.getFirstName();
    }

    public void setFirstName(String name) {
        this.subject.setFirstName(name);
    }

    public String getLastName() {
        return (this.subject == null) ? null : this.subject.getLastName();
    }

    public void setLastName(String name) {
        this.subject.setLastName(name);
    }

    public String getEmailAddress() {
        return (this.subject == null) ? null : this.subject.getEmailAddress();
    }

    public void setEmailAddress(String emailAddress) {
        this.subject.setEmailAddress(emailAddress);
    }

    public String getPhoneNumber() {
        return (this.subject == null) ? null : this.subject.getPhoneNumber();
    }

    public void setPhoneNumber(String phoneNumber) {
        this.subject.setPhoneNumber(phoneNumber);
    }

    public String getDepartment() {
        return (this.subject == null) ? null : this.subject.getDepartment();
    }

    public void setDepartment(String department) {
        this.subject.setDepartment(department);
    }

    public boolean getActive() {
        return (this.subject != null && this.subject.getFactive());
    }

    public void setActive(boolean active) {
        this.subject.setFactive(active);
    }

    /** Return a human readable serialization of this object */
    @Override
    public String toString() {
        StringBuffer str = new StringBuffer("{");
        str.append("id=").append(getId()).append(" ");
        str.append("sessionId=").append(getSessionId()).append(" ");
        str.append("hasPrincipal=").append(getHasPrincipal()).append(" ");
        str.append("subject=").append(getSubject()).append(" ");
        str.append("}");
        return (str.toString());
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean getHasPrincipal() {
        return this.hasPrincipal;
    }

    public void setHasPrincipal(boolean hasPrincipal) {
        this.hasPrincipal = hasPrincipal;
    }

    public Configuration getPreferences() {
        return this.subject.getUserConfiguration();
    }

    public void setPreferences(Configuration preferences) {
        this.subject.setUserConfiguration(preferences);
    }

    /**
     * This forces a flush of the user preferences to the database.
     */
    public void persistPreferences() {
        Integer sessionId = this.subject.getSessionId(); // let's remember our transient session ID
        this.subject = LookupUtil.getSubjectManager().updateSubject(this.subject, this.subject);
        this.subject.setSessionId(sessionId); // put the transient data back into our new subject
    }

    public String getPreference(String key) throws IllegalArgumentException {
        Configuration userConfiguration = this.subject.getUserConfiguration();
        PropertySimple prop = null;

        if (userConfiguration != null)
            prop = userConfiguration.getSimple(key);

        if (prop == null)
            throw new IllegalArgumentException("preference '" + key + "' requested is not valid");

        String value = prop.getStringValue();

        // null values are often the default for many props; let the caller determine whether this is an error
        if (value != null) {
            value = value.trim();
        }

        return value;
    }

    public String getPreference(String key, String defaultValue) {
        String value;
        try {
            value = getPreference(key);
        } catch (IllegalArgumentException iae) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Break the named preference into tokens delimited by <code>PREF_LIST_DELIM</code>.
     *
     * @param key the name of the preference
     * @return <code>List</code> of <code>String</code> tokens
     */
    public List<String> getPreferenceAsList(String key) {
        return getPreferenceAsList(key, PREF_LIST_DELIM);
    }

    /**
     * Tokenize the named preference into a List of Strings. If no such preference exists, or the preference is null,
     * an empty List will be returned.
     *
     * @param delimiter the delimiter to break it up by
     * @param key the name of the preference
     * @return <code>List</code> of <code>String</code> tokens
     */
    @NotNull
    public List<String> getPreferenceAsList(String key, String delimiter) {
        String pref = null;
        try {
            pref = getPreference(key);
        } catch (IllegalArgumentException e) {
            log.debug("A user preference named '" + key + "' does not exist.");
        }
        return (pref != null) ? StringUtil.explode(pref, delimiter) : new ArrayList<String>();
    }

    public void setPreference(String key, List values) throws IllegalArgumentException {
        setPreference(key, values, PREF_LIST_DELIM);
    }

    public void setPreference(String key, List values, String delim) throws IllegalArgumentException {
        String stringified = StringUtil.listToString(values, delim);
        setPreference(key, stringified);
    }

    public void setPreference(String key, Object value) throws IllegalArgumentException {
        String val = null;
        if (value == null) {
            val = "";
        } else if (value instanceof String) {
            val = (String) value;
        } else {
            val = value.toString();
        }

        PropertySimple existingProp = this.subject.getUserConfiguration().getSimple(key);
        if (existingProp == null) {
            this.subject.getUserConfiguration().put(new PropertySimple(key, val));
        } else {
            existingProp.setStringValue(val);
        }
    }

    public void unsetPreference(String key) {
        Configuration config = subject.getUserConfiguration();
        if (config != null)
            config.remove(key);
    }

    /**
     * Returns a list of metric ids saved as favorites for a particular appdef
     * type
     */
    public List getResourceFavoriteMetricsPreference(String appdefTypeName) throws IllegalArgumentException {
        return getPreferenceAsList(getResourceFavoriteMetricsKey(appdefTypeName));
    }

    /**
     * Method getResourceFavoriteMetricsKey.
     *
     * Encapsulates the logic for how the favorite metrics key for a particular appdef
     * type is calculated
     *
     * @param appdefTypeName i.e. application, platform, server, service
     * @return String the calculated preferences key
     */
    public String getResourceFavoriteMetricsKey(String appdefTypeName) {
        StringBuffer sb = new StringBuffer(PREF_FAV_RESOURCE_METRICS_PREFIX);
        sb.append('.').append(appdefTypeName);
        return sb.toString();
    }

    /**
     * Returns a Map of pref values:
     *
     * <ul>
     *   <li><code>MonitorUtils.RO</code>: Boolean
     *   <li><code>MonitorUtils.LASTN</code>: Integer
     *   <li><code>MonitorUtils.UNIT</code>: Unit
     *   <li><code>MonitorUtils.BEGIN</code>: Long
     *   <li><code>MonitorUtils.END</code>: Long
     * </ul>
     */
    public Map getMetricRangePreference(boolean defaultRange) throws IllegalArgumentException {
        Map m = new HashMap();

        //  properties may be empty or unparseable strings (ex:
        //  "null"). if so, use their default values.
        Boolean ro;
        try {
            ro = Boolean.valueOf(getPreference(PREF_METRIC_RANGE_RO));
        } catch (IllegalArgumentException nfe) {
            ro = MonitorUtils.DEFAULT_VALUE_RANGE_RO;
        }
        m.put(MonitorUtils.RO, ro);

        Integer lastN = null;
        try {
            lastN = Integer.valueOf(getPreference(PREF_METRIC_RANGE_LASTN));
        } catch (IllegalArgumentException nfe) {
            lastN = MonitorUtils.DEFAULT_VALUE_RANGE_LASTN;
        }
        m.put(MonitorUtils.LASTN, lastN);

        Integer unit = null;
        try {
            unit = Integer.valueOf(getPreference(PREF_METRIC_RANGE_UNIT));
        } catch (IllegalArgumentException nfe) {
            unit = MonitorUtils.DEFAULT_VALUE_RANGE_UNIT;
        }
        m.put(MonitorUtils.UNIT, unit);

        List range = null;
        try {
            range = getPreferenceAsList(PREF_METRIC_RANGE);
        } catch (IllegalArgumentException iae) {
            // that's ok
        }
        Long begin = null;
        Long end = null;
        if (range != null && range.size() > 0) {
            try {
                begin = new Long((String) range.get(0));
                end = new Long((String) range.get(1));
            } catch (NumberFormatException nfe) {
                begin = null;
                end = null;
            }
        }

        // sometimes we are satisfied with no range. other times we
        // need to calculate the "last n" units range and return
        // that.
        if (defaultRange && begin == null && end == null) {
            range = MonitorUtils.calculateTimeFrame(lastN.intValue(), unit.intValue());

            begin = (Long) range.get(0);
            end = (Long) range.get(1);
        }

        m.put(MonitorUtils.BEGIN, begin);
        m.put(MonitorUtils.END, end);

        return m;
    }

    public Map getMetricRangePreference() throws IllegalArgumentException {
        return getMetricRangePreference(true);
    }

    /**
     * Returns a list of metric ids saved as favorites for a particular appdef
     * type
     */
    public Integer getMetricThresholdPreference() throws IllegalArgumentException {
        return new Integer(getPreference(PREF_METRIC_THRESHOLD));
    }

    /**
     * Get the value of a preference as a boolean.
     * @param key the preference to get
     * @param ifNull if the pref is undefined, return this value instead
     * @return the boolean value of 'key', or if key is null, returns the
     * 'ifNull' value.
     */
    public boolean getBooleanPref(String key, boolean ifNull) {
        String val;
        try {
            val = getPreference(key);
        } catch (IllegalArgumentException e) {
            return ifNull;
        }
        return Boolean.valueOf(val).booleanValue();
    }

    /**
     * Get the value of a preference as an int.
     * @param key the preference to get
     * @param ifNull if the pref is null, return this value instead
     * @return the int value of 'key', or if key is null, returns the
     * 'ifNull' value.
     */
    public int getIntPref(String key, int ifNull) {
        String val;
        try {
            val = getPreference(key);
            if ("".equals(val)) {
                return ifNull;
            }
        } catch (IllegalArgumentException e) {
            return ifNull;
        }
        return Integer.parseInt(val);
    }

    public static final int DONT_REFRESH_PAGE = 0;

    /**
     * If the PREF_PAGE_REFRESH_PERIOD preference is set to a valid
     * value, i.e. one indicating that pages should be refreshed (not 0),
     * then the 'refreshPeriod' attribute on the request will set to the
     * value of this preference.
     *
     * @param request
     */
    public void setPageRefreshPeriodOnRequest(HttpServletRequest request) {
        int refreshPeriod = getIntPref(PREF_PAGE_REFRESH_PERIOD, DONT_REFRESH_PAGE);
        if (DONT_REFRESH_PAGE != refreshPeriod) {
            request.setAttribute("refreshPeriod", String.valueOf(refreshPeriod));
        }
    }

    public PageControl getPageControl(PageControlView view) {
        if (view == PageControlView.NONE) {
            return PageControl.getUnlimitedInstance();
        }

        List<String> pageControlProperties = getPreferenceAsList(view.toString());
        if (pageControlProperties.size() == 0) {
            PageControl defaultControl = null;
            if (view.getShowAll()) {
                defaultControl = PageControl.getUnlimitedInstance();
            } else {
                defaultControl = new PageControl(0, 15);
            }
            setPageControl(view, defaultControl);
            return defaultControl;
        } else {
            int pageSize = Integer.valueOf(pageControlProperties.get(0));
            PageControl pageControl = new PageControl(0, pageSize);

            int i = 2;
            while (i < pageControlProperties.size()) {
                String pageOrdering = pageControlProperties.get(i - 1);
                String sortColumn = pageControlProperties.get(i);

                pageControl.addDefaultOrderingField(sortColumn, PageOrdering.valueOf(pageOrdering));

                i += 2;
            }
            return pageControl;
        }
    }

    @SuppressWarnings("unchecked")
    public void setPageControl(PageControlView view, PageControl pageControl) {
        if (view == PageControlView.NONE) {
            return; // nothing is stored in session for the special NONE view
        }

        List pageControlProperties = new ArrayList();
        pageControlProperties.add(pageControl.getPageSize());

        for (OrderingField field : pageControl.getOrderingFieldsAsArray()) {
            pageControlProperties.add(field.getOrdering().toString());
            pageControlProperties.add(field.getField());
        }

        setPreference(view.toString(), pageControlProperties);
    }
}
