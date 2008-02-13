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
package org.rhq.enterprise.communications.command.param;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import mazz.i18n.Logger;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * An extenstion to a parameter definition that defines a limited, fixed set of values that are allowed to be assigned
 * to the parameter.
 *
 * <p>Note that only non-array types may have fixed values.</p>
 *
 * @author John Mazzitelli
 */
public class FixedValuesParameterDefinition extends ParameterDefinition {
    /**
     * Logger
     */
    private static final Logger LOG = CommI18NFactory.getLogger(FixedValuesParameterDefinition.class);

    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    /**
     * list of the fixed values this parameter is allowed to have
     */
    private List<Object> m_allowedValues;

    /**
     * Constructor for {@link FixedValuesParameterDefinition} that allows for an optional description to be set and
     * allows the caller to specify an array, as opposed to a <code>List</code> of allowed objects.
     *
     * @param  name        the name of the parameter (this is the name you use to look up the parameter value via
     *                     {@link Command#getParameterValue(String)} (must not be <code>null</code>)
     * @param  type        the parameter data type specified "the Java way"; e.g. "<code>java.lang.String</code>","
     *                     <code>[Ljava.util.Date;</code>" (must not be <code>null</code>)
     * @param  required    if <code>true</code>, this parameter is required to successfully execute a command
     * @param  nullable    if <code>true</code>, this parameter is allowed to be <code>null</code>
     * @param  hidden      if <code>true</code>, this parameter should be hidden from users (though it doesn't prohibit
     *                     users from setting the parameter, it only hides it from user interfaces)
     * @param  allowed     contains elements that define the only values that are allowed for this parameter (must not
     *                     be <code>null</code> or empty)
     * @param  description a human readable description string that describes the function of the parameter (may be
     *                     <code>null</code>)
     *
     * @throws IllegalArgumentException if <code>name</code> or <code>type</code> is <code>null</code> or <code>
     *                                  allowed</code> is <code>null</code>, empty or contains an element whose type is
     *                                  not convertible to <code>type</code>. Also, if <code>type</code> represents an
     *                                  unknown class or an array type.
     */
    public FixedValuesParameterDefinition(String name, String type, boolean required, boolean nullable, boolean hidden,
        Object[] allowed, String description) throws IllegalArgumentException {
        this(name, type, required, nullable, hidden, (allowed != null) ? Arrays.asList(allowed) : null, description,
            (ParameterRenderingInformation) null);
    }

    /**
     * Constructor for {@link FixedValuesParameterDefinition} that allows for an optional description to be set.
     *
     * @param  name        the name of the parameter (this is the name you use to look up the parameter value via
     *                     {@link Command#getParameterValue(String)} (must not be <code>null</code>)
     * @param  type        the parameter data type specified "the Java way"; e.g. "<code>java.lang.String</code>","
     *                     <code>[Ljava.util.Date;</code>" (must not be <code>null</code>)
     * @param  hidden      if <code>true</code>, this parameter should be hidden from users (though it doesn't prohibit
     *                     users from setting the parameter, it only hides it from user interfaces)
     * @param  allowed     contains elements that define the only values that are allowed for this parameter (must not
     *                     be <code>null</code> or empty)
     * @param  description a human readable description string that describes the function of the parameter (may be
     *                     <code>null</code>)
     *
     * @throws IllegalArgumentException if <code>name</code> or <code>type</code> is <code>null</code> or <code>
     *                                  allowed</code> is <code>null</code>, empty or contains an element whose type is
     *                                  not convertible to <code>type</code>. Also, if <code>type</code> represents an
     *                                  unknown class or an array type.
     */
    public FixedValuesParameterDefinition(String name, String type, boolean hidden, List<Object> allowed,
        String description) throws IllegalArgumentException {
        this(name, type, hidden, allowed, description, (ParameterRenderingInformation) null);
    }

    /**
     * Constructor for {@link FixedValuesParameterDefinition} that allows for an optional description to be set.
     *
     * @param  name          the name of the parameter (this is the name you use to look up the parameter value via
     *                       {@link Command#getParameterValue(String)} (must not be <code>null</code>)
     * @param  type          the parameter data type specified "the Java way"; e.g. "<code>java.lang.String</code>","
     *                       <code>[Ljava.util.Date;</code>" (must not be <code>null</code>)
     * @param  hidden        if <code>true</code>, this parameter should be hidden from users (though it doesn't
     *                       prohibit users from setting the parameter, it only hides it from user interfaces)
     * @param  allowed       contains elements that define the only values that are allowed for this parameter (must not
     *                       be <code>null</code> or empty)
     * @param  description   a human readable description string that describes the function of the parameter (may be
     *                       <code>null</code>)
     * @param  renderingInfo information relating to how the parameter should be rendered by clients. See
     *                       {@link ParameterRenderingInformation}
     *
     * @throws IllegalArgumentException if <code>name</code> or <code>type</code> is <code>null</code> or <code>
     *                                  allowed</code> is <code>null</code>, empty or contains an element whose type is
     *                                  not convertible to <code>type</code>. Also, if <code>type</code> represents an
     *                                  unknown class or an array type.
     */
    public FixedValuesParameterDefinition(String name, String type, boolean hidden, List<Object> allowed,
        String description, ParameterRenderingInformation renderingInfo) throws IllegalArgumentException {
        this(name, type, false, true, hidden, allowed, description, renderingInfo);
    }

    /**
     * Constructor for {@link FixedValuesParameterDefinition} that allows for an optional description to be set.
     *
     * @param  name        the name of the parameter (this is the name you use to look up the parameter value via
     *                     {@link Command#getParameterValue(String)} (must not be <code>null</code>)
     * @param  type        the parameter data type specified "the Java way"; e.g. "<code>java.lang.String</code>","
     *                     <code>[Ljava.util.Date;</code>" (must not be <code>null</code>)
     * @param  required    if <code>true</code>, this parameter is required to successfully execute a command
     * @param  nullable    if <code>true</code>, this parameter is allowed to be <code>null</code>
     * @param  hidden      if <code>true</code>, this parameter should be hidden from users (though it doesn't prohibit
     *                     users from setting the parameter, it only hides it from user interfaces)
     * @param  allowed     contains elements that define the only values that are allowed for this parameter (must not
     *                     be <code>null</code> or empty)
     * @param  description a human readable description string that describes the function of the parameter (may be
     *                     <code>null</code>)
     *
     * @throws IllegalArgumentException if <code>name</code> or <code>type</code> is <code>null</code> or <code>
     *                                  allowed</code> is <code>null</code>, empty or contains an element whose type is
     *                                  not convertible to <code>type</code>. Also, if <code>type</code> represents an
     *                                  unknown class or an array type.
     */
    public FixedValuesParameterDefinition(String name, String type, boolean required, boolean nullable, boolean hidden,
        List<Object> allowed, String description) throws IllegalArgumentException {
        this(name, type, required, nullable, hidden, allowed, description, (ParameterRenderingInformation) null);
    }

    /**
     * Constructor for {@link FixedValuesParameterDefinition} that allows for an optional description to be set.
     *
     * @param  name          the name of the parameter (this is the name you use to look up the parameter value via
     *                       {@link Command#getParameterValue(String)} (must not be <code>null</code>)
     * @param  type          the parameter data type specified "the Java way"; e.g. "<code>java.lang.String</code>","
     *                       <code>[Ljava.util.Date;</code>" (must not be <code>null</code>)
     * @param  required      if <code>true</code>, this parameter is required to successfully execute a command
     * @param  nullable      if <code>true</code>, this parameter is allowed to be <code>null</code>
     * @param  hidden        if <code>true</code>, this parameter should be hidden from users (though it doesn't
     *                       prohibit users from setting the parameter, it only hides it from user interfaces)
     * @param  allowed       contains elements that define the only values that are allowed for this parameter (must not
     *                       be <code>null</code> or empty)
     * @param  description   a human readable description string that describes the function of the parameter (may be
     *                       <code>null</code>)
     * @param  renderingInfo information relating to how the parameter should be rendered by clients. See
     *                       {@link ParameterRenderingInformation}
     *
     * @throws IllegalArgumentException if <code>name</code> or <code>type</code> is <code>null</code> or <code>
     *                                  allowed</code> is <code>null</code>, empty or contains an element whose type is
     *                                  not convertible to <code>type</code>. Also, if <code>type</code> represents an
     *                                  unknown class or an array type.
     */
    public FixedValuesParameterDefinition(String name, String type, boolean required, boolean nullable, boolean hidden,
        List<Object> allowed, String description, ParameterRenderingInformation renderingInfo)
        throws IllegalArgumentException {
        super(name, type, required, nullable, hidden, description, renderingInfo);

        try {
            Class typeClass = Class.forName(type);
            if (typeClass.isArray()) {
                throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.NO_ARRAY_TYPES_ALLOWED));
            }
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.INVALID_PARAM_TYPE, type, cnfe));
        }

        setAllowedValues(allowed);

        return;
    }

    /**
     * Returns the list of the parameter's only allowed values. These fixed values may not be changed. The returned list
     * is only a copy; changes to the returned list do not affect the internal list.
     *
     * @return allowedValues contains elements defining the only allowed values this parameter may be
     */
    public List<Object> getAllowedValues() {
        return new ArrayList<Object>(m_allowedValues);
    }

    /**
     * This method allows subclasses to replace this object's fixed set of allowed values with a new set. The items in
     * <code>allowedValues</code> will be copied into this object's own storage area. The old
     * {@link #getAllowedValues() allowed values} will be removed - they will no longer be valid.
     *
     * @param  newAllowedValues the new list of allowed values
     *
     * @throws IllegalArgumentException if failed to convert the values to the parameter definition's type or <code>
     *                                  allowedValues</code> was <code>null</code> or empty and this parameter
     *                                  definition object does not accept that (see {@link #allowZeroFixedValues()}).
     */
    protected void setAllowedValues(List<Object> newAllowedValues) throws IllegalArgumentException {
        if ((newAllowedValues == null) || (newAllowedValues.size() == 0)) {
            if (!allowZeroFixedValues()) {
                throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.NEED_AT_LEAST_ONE_FIXED_VALUE,
                    getName()));
            }
        }

        // prepare to get a new list by setting the current allowed values list to null;
        // setting this to null "turns off" isValidValue check that allows us to convert allowed values below
        m_allowedValues = null;

        List<Object> convertedAllowedValues = null;

        try {
            try {
                convertedAllowedValues = convertAllowedValues(newAllowedValues);
            } catch (InvalidParameterValueException ipve) {
                throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.ALLOWED_VALUE_INVALID_TYPE,
                    getType(), ipve));
            }
        } finally {
            // note that the allowed values list will be empty if an exception occurs during conversion
            // in other words, the caller will change the state of this object even if an exception is thrown
            m_allowedValues = (convertedAllowedValues != null) ? convertedAllowedValues : new ArrayList<Object>();
        }

        return;
    }

    /**
     * Ensures that the parameter's value is one of the fixed set of allowed values. The value is checked against the
     * list of {@link #getAllowedValues() allowed values} using <code>Object.equals(Object)</code>.
     *
     * @see ParameterDefinition#isValidValue(Object)
     */
    public boolean isValidValue(Object valueToCheck) {
        boolean retValid = super.isValidValue(valueToCheck);

        if ((retValid) && (valueToCheck != null) && (m_allowedValues != null)) {
            // so far so good - now make sure the value is equal to one of the allowed values;
            retValid = m_allowedValues.contains(valueToCheck);
        }

        return retValid;
    }

    /**
     * Makes sure that, after conversion, the object still matches one of the fixed, allowed values.
     *
     * @see ParameterDefinition#convertObject(Object)
     */
    public Object convertObject(Object objectToConvert) throws InvalidParameterValueException {
        Object convertedObject = super.convertObject(objectToConvert);

        // object has been converted; but make sure it conforms to one of the allowable values
        if (!isValidValue(convertedObject)) {
            throw new InvalidParameterValueException(LOG.getMsgString(
                CommI18NResourceKeys.FIXED_VALUES_PARAMETER_DEFINITION_INVALID_VALUE, getName(), m_allowedValues));
        }

        return convertedObject;
    }

    /**
     * @see ParameterDefinition#toString()
     */
    public String toString() {
        StringBuffer strBuf = new StringBuffer(super.toString());

        strBuf.append("; allowed-values=[");
        strBuf.append(m_allowedValues);
        strBuf.append("]");

        return strBuf.toString();
    }

    /**
     * This parameter definition class will, by default, rendering the parameter as an option list.
     *
     * @see ParameterDefinition#getDefaultRenderingInfo()
     */
    protected ParameterRenderingInformation getDefaultRenderingInfo() {
        OptionListRenderingInformation renderingInfo = new OptionListRenderingInformation();
        setDefaultRenderingAttributes(renderingInfo);

        return renderingInfo;
    }

    /**
     * Returns <code>true</code> if this parameter definition object allows an empty set of allowed values. It will
     * return <code>false</code> if the fixed set of allowed values must have at least one value. This implementation
     * always returns <code>false</code> since this class does not allow for the list of fixed values to change
     * dynamically at runtime (thus, having an empty set of fixed values essentially renders this definition useless
     * since no values will ever be considered valid). This is only useful if the parameter definition allows the fixed
     * list of allowed values to be changed dynamically - if subclasses do allow for this use-case, those subclasses
     * should override this method to have it return <code>true</code>.
     *
     * @return this method implementation always returns <code>false</code>
     */
    protected boolean allowZeroFixedValues() {
        return false;
    }

    /**
     * Given a list of values, this will convert them to this parameter definition's type and return those converted
     * types in a list. If a value in <code>allowed</code> didn't need to be converted, it will be returned as-is in the
     * returned list. The purpose of this method is convert a list whose values will eventually be used as this object's
     * {@link #getAllowedValues() allowed values}.
     *
     * @param  allowed list of values that will eventually be used as this object's allowed values (may be <code>
     *                 null</code>)
     *
     * @return list of <code>allowed</code> values with all the values assured to be of the parameter definition's type
     *
     * @throws InvalidParameterValueException if a valid in <code>allowed</code> was not convertible
     */
    private List<Object> convertAllowedValues(List<Object> allowed) throws InvalidParameterValueException {
        List<Object> convertedAllowed = new ArrayList<Object>();

        if (allowed != null) {
            for (Iterator iter = allowed.iterator(); iter.hasNext();) {
                Object element = iter.next();

                convertedAllowed.add(super.convertObject(element));
            }
        }

        return convertedAllowed;
    }
}