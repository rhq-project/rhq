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

/**
 * A parameter encapsulates both a {@link ParameterDefinition definition} and an object value. Note that a parameter is
 * allowed to have a <code>null</code> definition, that is, the parameter can be of an unknown type and name; in this
 * case, only its value is known.
 *
 * <p>A definition is immutable but this class allows for its parameter value to be set.</p>
 *
 * @author <a href="ccrouch@jboss.com">Charles Crouch</a>
 * @author <a href="mazz@jboss.com">John Mazzitelli</a>
 */
public class Parameter implements Serializable {
    /**
     * The serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    /**
     * The definition that describes the parameter - note it is final/immutable
     */
    private final ParameterDefinition m_definition;

    /**
     * the actual value of the parameter - this value can change during its lifetime
     */
    private Object m_value;

    /**
     * this will be <code>true</code> when the {@link #getValue() value} has changed from its initial value as set by a
     * call to one of the constructors.
     */
    private boolean m_dirty;

    /**
     * Copy-constructor for {@link Parameter}.
     *
     * @param original the original to copy
     */
    public Parameter(Parameter original) {
        this(original.getDefinition(), original.getValue(), original.isDirty());
    }

    /**
     * Creates a new {@link Parameter} object given the individual parts of the parameter.
     *
     * @param definition the parameter's definition
     * @param value      the value of the parameter
     * @param isDirty    the flag to indicate if the parameter's value was changed (i.e. different than its original or
     *                   default value)
     */
    protected Parameter(ParameterDefinition definition, Object value, boolean isDirty) {
        m_definition = definition;
        m_value = value;
        m_dirty = isDirty;

        return;
    }

    /**
     * Constructor for {@link Parameter} with the given initializers. Note that the definition may be <code>null</code>,
     * in which case nothing is known about the parameter other than its <code>value</code>.
     *
     * @param definition the object that defines the parameter metadata like its name, type, etc. (may be <code>
     *                   null</code>)
     * @param value      the actual value of the parameter (may be <code>null</code>)
     */
    public Parameter(ParameterDefinition definition, Object value) {
        this(definition, value, false);
    }

    /**
     * This is a convienence method to get the name of the parameter from its {@link #getDefinition() definition}.
     *
     * @return the parameter name
     */
    public String getName() {
        return getDefinition().getName();
    }

    /**
     * Returns the parameter's definition. This may be <code>null</code>, in which case you don't know anything about
     * the parameter other than its {@link #getValue() value}.
     *
     * @return the parameter's metadata definition (like its name, type, etc.)
     */
    public ParameterDefinition getDefinition() {
        return m_definition;
    }

    /**
     * Returns the parameter's data value. This may be <code>null</code>.
     *
     * @return the parameter value
     */
    public Object getValue() {
        return m_value;
    }

    /**
     * Sets the parameter's data value. This may be <code>null</code>. The {@link #isDirty() dirty flag} is set when
     * this method is called.
     *
     * @param value the parameter value
     */
    public void setValue(Object value) {
        this.m_value = value;
        m_dirty = true;
    }

    /**
     * Returns <code>true</code> if the dirty flag has been set; that is, if {@link #setValue(Object)} has been called,
     * thus changing the value of this object's original value.
     *
     * @return <code>true</code> if the parameter's value was changed from its original value from the time this object
     *         was instantiated
     */
    public boolean isDirty() {
        return m_dirty;
    }

    /**
     * Same as {@link #toString(boolean) toString(true)}.
     *
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return toString(true);
    }

    /**
     * Will provide the string form of the parameter but the value's actual toString will only be returned in the string
     * if <code>showValue</code> is <code>true</code>. If it is <code>false</code>, the value will be omitted from the
     * string.
     *
     * @param  showValue if <code>true</code>, the parameter's value will be in the returned string, it will not be if
     *                   <code>false</code>
     *
     * @return the string form of the parameter
     *
     * @see    java.lang.Object#toString()
     */
    public String toString(boolean showValue) {
        StringBuffer str = new StringBuffer("Parameter: ");

        str.append("def=[" + m_definition + "]");
        str.append("; value's class=[" + ((m_value == null) ? "null" : m_value.getClass().toString()) + "]");
        str.append("; dirty=[" + m_dirty + "]");
        if (showValue) {
            str.append("; value=[" + ((m_value == null) ? "null" : m_value.toString()) + "]");
        }

        return str.toString();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if ((obj == null) || (!(obj instanceof Parameter))) {
            return false;
        }

        Parameter other = (Parameter) obj;

        // first check the value
        if (this.m_value == null) {
            if (other.m_value != null) {
                return false;
            }
        } else if (!this.m_value.equals(other.m_value)) {
            return false;
        }

        // value is the same, now check the definition
        if (this.m_definition == null) {
            if (other.m_definition != null) {
                return false;
            }
        } else if (!this.m_definition.equals(other.m_definition)) {
            return false;
        }

        // everything is the same
        return true;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        int hash = 7;
        hash = (hash * 31) + ((m_definition == null) ? 0 : m_definition.hashCode());
        hash = (hash * 31) + ((m_value == null) ? 0 : m_value.hashCode());
        return hash;
    }
}