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
package org.rhq.enterprise.server.auth;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.util.StringUtil;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.util.LookupUtil;

public abstract class SubjectPreferencesBase {

    protected final Log log = LogFactory.getLog(SubjectPreferencesBase.class);

    /** delimiter for preferences that are multi-valued and stringified */
    protected static final String PREF_LIST_DELIM = ",";
    protected static final String PREF_ITEM_DELIM = "|";

    private Subject subject;

    public SubjectPreferencesBase(Subject subject) {
        this.subject = subject;
    }

    /**
     * Get the value of a preference as a boolean.
     * @param key the preference to get
     * @param ifNull if the pref is undefined, return this value instead
     * @return the boolean value of 'key', or if key is null, returns the
     * 'ifNull' value.
     */
    protected boolean getBooleanPref(String key) {
        String val = getPreference(key);
        return Boolean.valueOf(val).booleanValue();
    }

    /**
     * Get the value of a preference as a boolean.
     * @param key the preference to get
     * @param ifNull if the pref is undefined, return this value instead
     * @return the boolean value of 'key', or if key is null, returns the
     * 'ifNull' value.
     */
    protected boolean getBooleanPref(String key, boolean ifNull) {
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
     * @return the int value of 'key'
     */
    protected int getIntPref(String key) {
        String val = getPreference(key);
        return Integer.parseInt(val);
    }

    /**
     * Get the value of a preference as an int.
     * @param key the preference to get
     * @param ifNull if the pref is null, return this value instead
     * @return the int value of 'key', or if key is null, returns the
     * 'ifNull' value.
     */
    protected int getIntPref(String key, int ifNull) {
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

    /**
     * Get the value of a preference as an long.
     * @param key the preference to get
     * @return the long value of 'key'
     */
    protected Long getLongPref(String key) {
        String val = getPreference(key);
        return Long.parseLong(val);
    }

    protected String getPreference(String key) throws IllegalArgumentException {
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
        log.debug("Getting " + key + "[" + value + "]");

        return value;
    }

    @SuppressWarnings("unchecked")
    public <T> T getPreference(String key, T defaultValue) {
        T result;
        try {
            String preferenceValue = getPreference(key);

            Class<T> type = (Class<T>) String.class;
            if (defaultValue != null) {
                type = (Class<T>) defaultValue.getClass();
            }

            if (type == String.class) {
                result = (T) preferenceValue; // cast string to self-type
            } else {
                if (type == Boolean.class) {
                    if (preferenceValue.equalsIgnoreCase("on") || preferenceValue.equalsIgnoreCase("yes")) {
                        preferenceValue = "true"; // flexible support for boolean translations from forms
                    }
                }

                try {
                    Method m = type.getMethod("valueOf", String.class);
                    result = (T) m.invoke(null, preferenceValue); // static method
                } catch (Exception e) {
                    throw new IllegalArgumentException("No support for automatic conversion of preferences of type "
                        + type);
                }
            }
        } catch (IllegalArgumentException iae) {
            result = defaultValue;
        }
        return result;
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
    protected List<String> getPreferenceAsList(String key, String delimiter) {
        String pref = null;
        try {
            pref = getPreference(key);
        } catch (IllegalArgumentException e) {
            log.debug("A user preference named '" + key + "' does not exist.");
        }
        return (pref != null) ? StringUtil.explode(pref, delimiter) : new ArrayList<String>();
    }

    protected List<Integer> getPreferenceAsIntegerList(String key, String delimiter) {
        try {
            List<String> value = getPreferenceAsList(key, delimiter);

            List<Integer> result = new ArrayList<Integer>(value.size());
            for (int i = 0; i < value.size(); i++) {
                String trimmed = value.get(i).trim();
                if (trimmed.length() > 0) {
                    result.add(Integer.valueOf(trimmed));
                }
            }

            return result;
        } catch (Exception e) {
            return new ArrayList<Integer>();
        }
    }

    protected void setPreference(String key, List<?> values) throws IllegalArgumentException {
        setPreference(key, values, PREF_LIST_DELIM);
    }

    protected void setPreference(String key, List<?> values, String delim) throws IllegalArgumentException {
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
            log.debug("Setting " + key + "[" + value + "]");
            this.subject.getUserConfiguration().put(new PropertySimple(key, val));
        } else {
            log.debug("Overriding " + key + "[" + value + "]");
            existingProp.setStringValue(val);
        }
    }

    protected void unsetPreference(String key) {
        Configuration config = subject.getUserConfiguration();
        if (config != null) {
            config.remove(key);
        }
    }

    /**
     * This forces a flush of the user preferences to the database.
     */
    public void persistPreferences() {
        Integer sessionId = this.subject.getSessionId(); // let's remember our transient session ID
        this.subject = LookupUtil.getSubjectManager().updateSubject(this.subject, this.subject);
        this.subject.setSessionId(sessionId); // put the transient data back into our new subject
    }

}
