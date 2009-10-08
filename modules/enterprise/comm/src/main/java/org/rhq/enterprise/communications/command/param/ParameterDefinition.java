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

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import mazz.i18n.Logger;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * Defines a parameter that is accepted by a {@link Command}. This class also provides some convienence methods to check
 * if a value conforms to this parameter and to convert a value so it does conform.
 *
 * <p>Note that a parameter definition is equal to another parameter definition if they have the same name. Equality is
 * based solely on name, irregardless of any other metadata defined in this object.
 *
 * @author John Mazzitelli
 */
public class ParameterDefinition implements Serializable {
    /**
     * Logger
     */
    private static final Logger LOG = CommI18NFactory.getLogger(ParameterDefinition.class);

    /**
     * Indicator that says a parameter is required
     */
    public static final boolean REQUIRED = true;

    /**
     * Indicator that says a parameter is optional
     */
    public static final boolean OPTIONAL = false;

    /**
     * Indicator that says a parameter is nullable (the parameter's value may be <code>null</code>)
     */
    public static final boolean NULLABLE = true;

    /**
     * Indicator that says a parameter is not nullable (the parameter's value must <b>not</b> be <code>null</code>)
     */
    public static final boolean NOT_NULLABLE = false;

    /**
     * Indicator that says a parameter should be hidden from a user's view. Used by user interfaces.
     */
    public static final boolean HIDDEN = true;

    /**
     * Indicator that says a parameter should be visible to a user. Used by user interfaces.
     */
    public static final boolean NOT_HIDDEN = false;

    /**
     * The name of the parameter - this is the name you use to look up the parameter value via
     * {@link Command#getParameterValue(String)}.
     */
    private final String m_name;

    /**
     * The type of the parameter specified "the Java way" (e.g. java.lang.String).
     */
    private final String m_type;

    /**
     * if <code>true</code>, this parameter's value is required to execute a command successfully.
     */
    private final boolean m_required;

    /**
     * if <code>true</code>, this parameter's value is allowed to be <code>null</code>.
     */
    private final boolean m_nullable;

    /**
     * if <code>true</code>, this parameter's existence should be hidden from view from a user interface. A user should
     * not know about this parameter if it is hidden.
     */
    private final boolean m_hidden;

    /**
     * a description of the parameter's function
     */
    private final String m_description;

    /**
     * information useful to clients who are trying to render the parameter for reading/editing
     */
    private ParameterRenderingInformation m_renderingInfo;

    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for {@link ParameterDefinition} that defines a parameter whose value is optional and may be nullable.
     * The rendering information is used for other metadata (such as if the parameter is hidden and its description).
     *
     * @param  name          the name of the parameter (this is the name you use to look up the parameter value via
     *                       {@link Command#getParameterValue(String)} (must not be <code>null</code>)
     * @param  type          the parameter data type specified "the Java way"; e.g. "java.lang.String" (must not be
     *                       <code>null</code>)
     * @param  renderingInfo information relating to how the parameter should be rendered by clients. See
     *                       {@link ParameterRenderingInformation}
     *
     * @throws IllegalArgumentException if <code>name</code> or <code>type</code> is <code>null</code>
     */
    public ParameterDefinition(String name, String type, ParameterRenderingInformation renderingInfo)
        throws IllegalArgumentException {
        this(name, type, false, null, renderingInfo);
    }

    /**
     * Constructor for {@link ParameterDefinition} that defines a parameter whose value is optional and may be nullable.
     *
     * @param  name        the name of the parameter (this is the name you use to look up the parameter value via
     *                     {@link Command#getParameterValue(String)} (must not be <code>null</code>)
     * @param  type        the parameter data type specified "the Java way"; e.g. "java.lang.String" (must not be <code>
     *                     null</code>)
     * @param  hidden      if <code>true</code>, this parameter should be hidden from any user interface - the user
     *                     should not know about this parameter (although <code>true</code> values do not prohibit users
     *                     from setting this parameter's value should they choose to)
     * @param  description a human readable description string that describes the function of the parameter (may be
     *                     <code>null</code>)
     *
     * @throws IllegalArgumentException if <code>name</code> or <code>type</code> is <code>null</code>
     */
    public ParameterDefinition(String name, String type, boolean hidden, String description)
        throws IllegalArgumentException {
        this(name, type, hidden, description, (ParameterRenderingInformation) null);
    }

    /**
     * Constructor for {@link ParameterDefinition} that defines a parameter whose value is optional and may be nullable.
     * Note that <code>hidden</code> and <code>description</code> are default values and can be overridden by the
     * analogous values found in the {@link #getRenderingInfo()}.
     *
     * @param  name          the name of the parameter (this is the name you use to look up the parameter value via
     *                       {@link Command#getParameterValue(String)} (must not be <code>null</code>)
     * @param  type          the parameter data type specified "the Java way"; e.g. "java.lang.String" (must not be
     *                       <code>null</code>)
     * @param  hidden        if <code>true</code>, this parameter should be hidden from any user interface - the user
     *                       should not know about this parameter (although <code>true</code> values do not prohibit
     *                       users from setting this parameter's value should they choose to)
     * @param  description   a human readable description string that describes the function of the parameter (may be
     *                       <code>null</code>)
     * @param  renderingInfo information relating to how the parameter should be rendered by clients. See
     *                       {@link ParameterRenderingInformation}
     *
     * @throws IllegalArgumentException if <code>name</code> or <code>type</code> is <code>null</code>
     */
    public ParameterDefinition(String name, String type, boolean hidden, String description,
        ParameterRenderingInformation renderingInfo) throws IllegalArgumentException {
        this(name, type, false, true, hidden, description, renderingInfo);
    }

    /**
     * Constructor for {@link ParameterDefinition} that allows for a description string.
     *
     * @param  name        the name of the parameter (this is the name you use to look up the parameter value via
     *                     {@link Command#getParameterValue(String)} (must not be <code>null</code>)
     * @param  type        the parameter data type specified "the Java way"; e.g. "java.lang.String" (must not be <code>
     *                     null</code>)
     * @param  required    if <code>true</code>, the parameter's value is required to successfully execute a command
     * @param  nullable    if <code>true</code>, the parameter's value is allowed to be <code>null</code>
     * @param  hidden      if <code>true</code>, this parameter should be hidden from any user interface - the user
     *                     should not know about this parameter (although <code>true</code> values do not prohibit users
     *                     from setting this parameter's value should they choose to)
     * @param  description a human readable description string that describes the function of the parameter (may be
     *                     <code>null</code>)
     *
     * @throws IllegalArgumentException if <code>name</code> or <code>type</code> is <code>null</code>
     */
    public ParameterDefinition(String name, String type, boolean required, boolean nullable, boolean hidden,
        String description) throws IllegalArgumentException {
        this(name, type, required, nullable, hidden, description, (ParameterRenderingInformation) null);
    }

    /**
     * Constructor for {@link ParameterDefinition} that allows for a description string. Note that <code>hidden</code>
     * and <code>description</code> are default values and can be overridden by the analogous values found in the
     * {@link #getRenderingInfo()}
     *
     * @param  name          the name of the parameter (this is the name you use to look up the parameter value via
     *                       {@link Command#getParameterValue(String)} (must not be <code>null</code>)
     * @param  type          the parameter data type specified "the Java way"; e.g. "java.lang.String" (must not be
     *                       <code>null</code>)
     * @param  required      if <code>true</code>, the parameter's value is required to successfully execute a command
     * @param  nullable      if <code>true</code>, the parameter's value is allowed to be <code>null</code>
     * @param  hidden        if <code>true</code>, this parameter should be hidden from any user interface - the user
     *                       should not know about this parameter (although <code>true</code> values do not prohibit
     *                       users from setting this parameter's value should they choose to)
     * @param  description   a human readable description string that describes the function of the parameter (may be
     *                       <code>null</code>)
     * @param  renderingInfo information relating to how the parameter should be rendered by clients. See
     *                       {@link ParameterRenderingInformation}
     *
     * @throws IllegalArgumentException if <code>name</code> or <code>type</code> is <code>null</code>
     */
    public ParameterDefinition(String name, String type, boolean required, boolean nullable, boolean hidden,
        String description, ParameterRenderingInformation renderingInfo) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("name=null");
        }

        if (type == null) {
            throw new IllegalArgumentException("type=null");
        }

        m_name = name;
        m_type = type;
        m_required = required;
        m_nullable = nullable;
        m_hidden = hidden;
        m_description = description;

        // this way clients will always have something to render with
        // even if its just the regular name and description,
        // subclasses can override getDefaultRenderingInfo to provide their
        // own settings, FixedValuesParameterDefinition does this
        if (renderingInfo == null) {
            renderingInfo = getDefaultRenderingInfo();
        }

        m_renderingInfo = renderingInfo;
    }

    /**
     * Returns the value of the name of the parameter.
     *
     * @return parameter name
     */
    public String getName() {
        return m_name;
    }

    /**
     * Returns the type of the parameter's value. The returned type string follows the Java conventions on type
     * specification strings (i.e. <code>java.lang.Integer</code>). See <code>java.lang.Class</code> javadocs for more
     * information.
     *
     * @return the type of the paramter's value
     */
    public String getType() {
        return m_type;
    }

    /**
     * Defines whether or not the parameter value is required to exist in order to be able to successfully execute a
     * command.
     *
     * @return if <code>true</code>, this parameter must be specified when invoking a command
     */
    public boolean isRequired() {
        return m_required;
    }

    /**
     * Defines whether or not the parameter's value is allowed to be <code>null</code>.
     *
     * @return if <code>true</code>, the parameter's value is allowed to be <code>null</code>
     */
    public boolean isNullable() {
        return m_nullable;
    }

    /**
     * Returns a flag to indicate if this parameter should be hidden from user interfaces. If <code>true</code>, this
     * parameter should not be known to the user and thus the user interfaces should not show this parameter's
     * existence. If <code>false</code>, this parameter should be visible to users and hence can be shown to users via a
     * user interface (which is typically the case).
     *
     * @return flag to indicate if this parameter should be hidden from a user or if it should be visible to the user
     */
    public boolean isHidden() {
        boolean flag = m_hidden;

        // rendering information overrides the default hidden flag, but only if rendering info is available
        if (getRenderingInfo() != null) {
            flag = getRenderingInfo().isHidden();
        }

        return flag;
    }

    /**
     * Returns a description indicating the purpose and function of the parameter. This may be <code>null</code> if it
     * was never defined.
     *
     * @return human readable description string
     */
    public String getDescription() {
        String desc = m_description;

        // rendering information overrides the default description, but only if rendering info is available
        if (getRenderingInfo() != null) {
            desc = getRenderingInfo().getDescription();
        }

        return desc;
    }

    /**
     * Get the rendering information that can be used to render the parameter in a user interface.
     *
     * @return the rendering info
     */
    public ParameterRenderingInformation getRenderingInfo() {
        return m_renderingInfo;
    }

    /**
     * Sets the rendering information that can be used to render the parameter in a user interface.
     *
     * @param renderingInfo the new rendering info
     */
    public void setRenderingInfo(ParameterRenderingInformation renderingInfo) {
        m_renderingInfo = renderingInfo;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer strbuf = new StringBuffer("ParamDef: ");

        strbuf.append("name=[");
        strbuf.append(m_name);
        strbuf.append("]; type=[");
        strbuf.append(m_type);
        strbuf.append("]; required=[");
        strbuf.append(m_required);
        strbuf.append("]; nullable=[");
        strbuf.append(m_nullable);
        strbuf.append("]; hidden=[");
        strbuf.append(m_hidden);
        strbuf.append("]; description=[");
        strbuf.append(m_description);
        strbuf.append("]");

        return strbuf.toString();
    }

    /**
     * Equality is based solely on {@link #getName() name} - a definition is the same as another if their names are the
     * same.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if ((obj == null) || (!(obj instanceof ParameterDefinition))) {
            return false;
        }

        return this.m_name.equals(((ParameterDefinition) obj).m_name);
    }

    /**
     * The hash code for a parameter definition is the same hash code as the parameter definition
     * {@link #getName() name}.
     *
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return m_name.hashCode();
    }

    /**
     * Checks the validity of the given object to ensure it conforms to this parameter definition. The given object's
     * type is compared to the parameter's desired {@link #getType() type}. If the given object is an instance of the
     * parameter's type, this method returns <code>true</code>. The actual type check is performed via <code>
     * java.lang.Class.isInstance(Object)</code>.
     *
     * <p>Nullability is also checked (that is, if the given object is <code>null</code>, this parameter definition must
     * {@link #isNullable() allow for null}.</p>
     *
     * <p>Note that if the parameter type class (i.e. java.lang.Class.forName({@link #getType()}) is unknown or
     * unloadable, this method returns <code>false</code>.</p>
     *
     * @param  valueToCheck checking the type validity of this object
     *
     * @return <code>true</code> if the given object conforms to this parameter definition, <code>false</code> otherwise
     */
    public boolean isValidValue(Object valueToCheck) {
        boolean valid;

        if (valueToCheck == null) {
            valid = isNullable();
        } else {
            try {
                Class parameterTypeClass = Class.forName(getType());
                valid = parameterTypeClass.isInstance(valueToCheck);
            } catch (ClassNotFoundException e) {
                valid = false;
            }
        }

        return valid;
    }

    /**
     * This method converts the given <code>objectToConvert</code> value into an instance of this parameter's
     * {@link #getType() type}. If the object to convert is not already of the parameter's type, a constructor is called
     * to build one. It will be assumed that this parameter's type class has a constructor that takes a single argument
     * of the same type as <code>objectToConvert</code>. If this constructor does exist, it is used to create the new
     * instance by passing <code>objectToConvert</code> to it. The resulting object is returned (this object will be of
     * the parameter's defined type. The original <code>objectToConvert</code> will be returned as-is (i.e. no
     * conversion will be performed) if either of the following is true:
     *
     * <ul>
     *   <li><code>objectToConvert</code> is <code>null</code></li>
     *   <li><code>objectToConvert</code> is already an instance of this parameter's type</li>
     * </ul>
     *
     * Note that if <code>objectToConvert</code> is <code>null</code>, but this parameter definition
     * {@link #isNullable() does not allow for null}, an exception is thrown.
     *
     * <p>This method is useful when needing to convert text-based command line parameters to their actual Java type
     * representations.</p>
     *
     * @param  objectToConvert the object to convert to the given type
     *
     * @return the converted object
     *
     * @throws InvalidParameterValueException if the given object is <code>null</code> but this parameter definition
     *                                        does not allow for <code>null</code>, or the parameter's type specifies a
     *                                        primitive type, or the conversion failed due to a problem occurring while
     *                                        instantiating the new typed object
     */
    public Object convertObject(Object objectToConvert) throws InvalidParameterValueException {
        if (objectToConvert == null) {
            if (!isNullable()) {
                throw new InvalidParameterValueException(LOG.getMsgString(
                    CommI18NResourceKeys.PARAMETER_DEFINITION_NOT_NULLABLE, m_name));
            }

            return objectToConvert;
        }

        String conversionClassString = getType();
        Class conversionClass;

        try {
            conversionClass = Class.forName(conversionClassString);
        } catch (ClassNotFoundException cnfe) {
            throw new InvalidParameterValueException(cnfe);
        }

        Object convertedObject;

        if (!conversionClass.isInstance(objectToConvert)) {
            convertedObject = convertObject(objectToConvert, conversionClass);

            // double-check that our new converted value is really valid now
            if (!isValidValue(convertedObject)) {
                throw new InvalidParameterValueException(LOG.getMsgString(
                    CommI18NResourceKeys.PARAMETER_DEFINITION_STILL_NOT_VALID, convertedObject.getClass()));
            }
        } else {
            // no conversion necessary
            convertedObject = objectToConvert;
        }

        return convertedObject;
    }

    /**
     * Returns this definition's default rendering information. If no rendering information is provided to this object's
     * constructors, then the returned rendering info from this method is used. Subclasses are free to override this
     * method to define their own default rendering information.
     *
     * <p>This method implementation will by default render all
     *
     * @return a default rendering information object that can be used by this parameter definition object
     */
    protected ParameterRenderingInformation getDefaultRenderingInfo() {
        ParameterRenderingInformation renderingInfo;
        Class valueClass = getClassFromTypeName(m_type);

        if (Map.class.isAssignableFrom(valueClass)) {
            renderingInfo = new TextFieldRenderingInformation(50, 5);
        } else if (Collection.class.isAssignableFrom(valueClass) || valueClass.isArray()) {
            renderingInfo = new TextFieldRenderingInformation(50, 1);
        } else {
            renderingInfo = new ParameterRenderingInformation();
        }

        setDefaultRenderingAttributes(renderingInfo);

        return renderingInfo;
    }

    /**
     * Sets the label, description and isHidden fields on the passed in <code>renderingInfo</code> object using this
     * definition for the default values. The purpose of this method is to allow this class (or subclasses) to populate
     * a given rendering info object with a set of appropriate default values. Subclasses are free to use this method or
     * override it to fill in rendering info with their own set of defaults.
     *
     * @param renderingInfo the rendering information that is to be populated with default attribute values
     */
    protected void setDefaultRenderingAttributes(ParameterRenderingInformation renderingInfo) {
        renderingInfo.setLabel(m_name);
        renderingInfo.setDescription(m_description);
        renderingInfo.setHidden(m_hidden);
        return;
    }

    /**
     * Convienence method that returns a <code>Class</code> object for the given type name. If the class is <code>
     * null</code>, an empty string or an invalid class name, a runtime exception will be thrown.
     *
     * @param  className the type name as a string
     *
     * @return the <code>Class</code> representation of the given type name string
     *
     * @throws IllegalArgumentException if the <code>className</code> is <code>null</code>, empty or invalid
     */
    private static Class getClassFromTypeName(String className) throws IllegalArgumentException {
        if ((className == null) || className.equals("")) {
            throw new IllegalArgumentException("className=null");
        }

        try {
            Class clazz = Class.forName(className);
            return clazz;
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.CLASS_NOT_FOUND, className));
        }
    }

    /**
     * This method converts the given <code>objectToConvert</code> value into an instance of the given type. See
     * {@link #convertObject(Object)} for additional information on how the convertion works.
     *
     * @param  objectToConvert the object to convert to the given type
     * @param  conversionClass the type to convert to
     *
     * @return the converted object that is of type <code>conversionClass</code>
     *
     * @throws InvalidParameterValueException if the specified <code>conversionClass</code> is a primitive type, or the
     *                                        conversion failed due to a problem occurring while instantiating the new
     *                                        typed object
     */
    private Object convertObject(Object objectToConvert, Class conversionClass) throws InvalidParameterValueException {
        if (conversionClass.isPrimitive()) {
            throw new InvalidParameterValueException(LOG.getMsgString(CommI18NResourceKeys.CANNOT_CONVERT_PRIMITIVE,
                conversionClass));
        }

        if (conversionClass.isArray()) {
            // special processing is required to convert to an array type
            return convertArrayObject(objectToConvert, conversionClass);
        }

        // IF the object to convert is not already of the desired type
        // THEN
        //    Get all the constructors supported by the desired type
        //    FOR each constructor AND we haven't already converted the value yet
        //       IF the current constructor has a single parameter whose type the original object can be cast to
        //       THEN
        //          Call that constructor, passing in the original object, thus converting the original to the new type
        //       END IF
        //    END FOR
        // ELSE
        //    Pass back the object as-is since it is already of the desired type
        // END IF
        Object convertedObject = null;

        if (!conversionClass.isInstance(objectToConvert)) {
            // if an exception occurred while attempting to call a constructor whose signature looks like one
            // that should work for us, that exception will be recorded here
            Exception constructorInvocationException = null;
            Constructor[] constructors = conversionClass.getConstructors();

            for (int i = 0; (i < constructors.length) && (convertedObject == null); i++) {
                Class[] params = constructors[i].getParameterTypes();

                if ((params.length == 1) && params[0].isInstance(objectToConvert)) {
                    try {
                        convertedObject = constructors[i].newInstance(new Object[] { objectToConvert });
                    } catch (Exception e) {
                        // remember this exception, but do not abort -- keep checking, maybe we'll get lucky and another
                        // overloaded constructor with a compatible parameter is available to us
                        constructorInvocationException = e;
                    }
                }
            }

            // we could not convert the object to the desired type
            if (convertedObject == null) {
                if (constructorInvocationException != null) {
                    throw new InvalidParameterValueException(LOG.getMsgString(
                        CommI18NResourceKeys.PARAMETER_DEFINITION_CANNOT_CONVERT, conversionClass),
                        constructorInvocationException);
                }

                throw new InvalidParameterValueException(LOG.getMsgString(
                    CommI18NResourceKeys.PARAMETER_DEFINITION_CANNOT_CONVERT_NO_CONSTRUCTOR, conversionClass,
                    objectToConvert.getClass()));
            }
        } else {
            convertedObject = objectToConvert;
        }

        return convertedObject;
    }

    /**
     * Converts the given object where that object actually represents an array of objects. If the <code>
     * objectToConvert</code> is an actual array itself, each object in that array will be converted to this
     * definition's type. Otherwise, the <code>objectToConvert</code>'s toString() will be tokenized and each token will
     * be converted to this definition's type. If the object is a tokenizable string, it can denote its delimiter if its
     * first character is one of the following:
     *
     * <ul>
     *   <li>,</li>
     *   <li>.</li>
     *   <li>;</li>
     *   <li>:</li>
     *   <li>|</li>
     *   <li>/</li>
     *   <li>!</li>
     *   <li>#</li>
     *   <li>$</li>
     *   <li>%</li>
     *   <li>^</li>
     *   <li>&amp;</li>
     *   <li>*</li>
     *   <li>-</li>
     *   <li>_</li>
     *   <li>+</li>
     *   <li>=</li>
     *   <li>space</li>
     *   <li>tab</li>
     *   <li>newline</li>
     *   <li>carriage-return</li>
     * </ul>
     *
     * If the first character is not one of the above, the default delimiter is a comma (,). If the string is <code>
     * null</code> or has 0*length, the returned converted array will be empty.
     *
     * <p>The <code>objectToConvert</code> must not be <code>null</code>.</p>
     *
     * @param  objectToConvert the object to convert (must be either an array or have tokenizable <code>
     *                         toString()</code>)
     * @param  conversionClass the array class to convert to
     *
     * @return the returned object array whose elements are of the converted type
     *
     * @throws InvalidParameterValueException if the conversion failed due to a problem occurring while instantiating
     *                                        the new typed object array
     */
    private Object[] convertArrayObject(Object objectToConvert, Class conversionClass)
        throws InvalidParameterValueException {
        Object[] objectArrayToConvert;

        // If the object to convert is not an array, then assume its String form is tokenizable and convert each string token
        if (!objectToConvert.getClass().isArray()) {
            List<String> stringArray = new ArrayList<String>();

            // convert the object to a tokenizable string
            String objectToString = objectToConvert.toString();

            if (objectToString == null) {
                objectToString = "";
            }

            if (objectToString.length() > 0) {
                // determine what the delimiter should be
                String delimiters = ",.;:|/!#$%^&*-_+= \t\n\r";
                char firstChar = objectToString.charAt(0);
                char theDelimiter = (delimiters.indexOf(firstChar) != -1) ? firstChar : ',';

                // go through the array elements in the string and put them in the objectArrayToConvert
                StringTokenizer strtok = new StringTokenizer(objectToString, Character.toString(theDelimiter));
                while (strtok.hasMoreTokens()) {
                    stringArray.add((String) strtok.nextElement());
                }
            }

            objectArrayToConvert = stringArray.toArray(new String[stringArray.size()]);
        } else {
            objectArrayToConvert = (Object[]) objectToConvert;
        }

        // determine the type of the array elements (which may in turn be arrays themselves; enter the realm of recursion to convert multi-dim arrays)
        Class arrayElementType = conversionClass.getComponentType();

        // prepare our return array of converted objects - we know the size of it by the size of the array of the objects to convert
        Object[] retConvertedArray = (Object[]) Array.newInstance(arrayElementType, objectArrayToConvert.length);

        // go through each object to convert, and convert it the normal way via convertObject
        for (int i = 0; i < objectArrayToConvert.length; i++) {
            retConvertedArray[i] = convertObject(objectArrayToConvert[i], arrayElementType);
        }

        return retConvertedArray;
    }
}