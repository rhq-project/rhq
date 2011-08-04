/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.domain.configuration;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This type of {@link Property} stores a simple Java primitive value in string form. Null values are allowed.
 *
 * @author Jason Dobies
 * @author Greg Hinkle
 */
@DiscriminatorValue("property")
@Entity
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class PropertySimple extends Property implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int MAX_VALUE_LENGTH = 2000;

    /**
     * This is the special value for simple properties of type PASSWORD that are masked. Masking and unmasking of
     * PASSWORD properties is done by the ConfigurationManagerBean SLSB, and, for the sake of security, prevents RHQ
     * clients from being able to view the current value of PASSWORD properties. The value is made obscure enough to
     * make the chances of it being the same as the property's unmasked value next to nil.
     */
    private static final String MASKED_VALUE = "_._._[MaSKeD]_._._";

    @Column(name = "string_value", length = MAX_VALUE_LENGTH)
    private String stringValue;

    @Column(name = "override")
    private Boolean override;

    /**
     * Constructor for {@link PropertySimple} that stores a <code>null</code> value.
     * NOTE: When using this constructor, you need to supply a name via #setName manually.
     */
    public PropertySimple() {
        override = Boolean.FALSE;
    }

    protected PropertySimple(PropertySimple original, boolean keepId) {
        super(original, keepId);

        this.stringValue = original.stringValue;
        this.override = original.override;
    }

    /**
     * Constructs a property that has the given name with the given value. The value's <code>toString</code>
     * representation will be stored in this object - <code>value</code> itself will not be stored (that is, this object
     * will not hold a reference to <code>value</code>).
     *
     * @param name
     * @param value
     */
    public PropertySimple(@NotNull String name, @Nullable Object value) {
        setName(name);
        setValue(value);
    }

    /**
     * Sets the value of this property to the <code>toString</code> form of the given <code>value</code>. This object
     * will not hold a reference to <code>value</code>.
     *
     * @param value
     */
    public void setValue(@Nullable Object value) {
        if (value == null) {
            stringValue = null;
            return;
        }

        String valueAsString = value.toString();
        stringValue = valueAsString.length() > MAX_VALUE_LENGTH ?
                valueAsString.substring(0, MAX_VALUE_LENGTH) : valueAsString;
    }

    /**
     * Returns the value (in string form) of this property, which may be <code>null</code>.
     *
     * <p>This may return <code>null</code></p>
     *
     * @return string form of the property value
     */
    @Nullable
    public String getStringValue() {
        return stringValue;
    }

    /**
     * Sets the value of this property to the given <code>value</code>.
     *
     * <p>Calling this method is the same as if calling {@link #setValue(Object)}.</p>
     *
     * @param value
     */
    public void setStringValue(@Nullable String value) {
        this.setValue(value);
    }

    /**
     * Returns this property value as a Boolean. See {@link Boolean#parseBoolean(String)} for the behavior of this
     * method.
     *
     * <p>This may return <code>null</code></p>
     *
     * @return the value as a Boolean
     */
    @Nullable
    public Boolean getBooleanValue() {
        return (stringValue == null) ? null : Boolean.valueOf(stringValue);
    }

    /**
     * Sets the value of this property to the <code>toString</code> form of the given <code>value</code>. This object
     * will not hold a reference to the <code>value</code>.
     *
     * <p>Calling this method is the same as if calling {@link #setValue(Object)}.</p>
     *
     * @param value
     */
    public void setBooleanValue(@Nullable Boolean value) {
        this.setValue(value);
    }

    /**
     * Returns this property value as a Long. See {@link Long#parseLong(String)} for the behavior of this method.
     *
     * <p>This may return <code>null</code></p>
     *
     * @return the value as a Long
     */
    @Nullable
    public Long getLongValue() {
        return (stringValue == null) ? null : Long.valueOf(stringValue);
    }

    /**
     * Sets the value of this property to the <code>toString</code> form of the given <code>value</code>. This object
     * will not hold a reference to the <code>value</code>.
     *
     * <p>Calling this method is the same as if calling {@link #setValue(Object)}.</p>
     *
     * @param value
     */
    public void setLongValue(@Nullable Long value) {
        this.setValue(value);
    }

    /**
     * Returns this property value as a Integer. See {@link Integer#parseInt(String)} for the behavior of this method.
     *
     * <p>This may return <code>null</code></p>
     *
     * @return the value as an Integer
     */
    @Nullable
    public Integer getIntegerValue() {
        return (stringValue == null) ? null : Integer.valueOf(stringValue);
    }

    /**
     * Sets the value of this property to the <code>toString</code> form of the given <code>value</code>. This object
     * will not hold a reference to the <code>value</code>.
     *
     * <p>Calling this method is the same as if calling {@link #setValue(Object)}.</p>
     *
     * @param value
     */
    public void setIntegerValue(@Nullable Integer value) {
        this.setValue(value);
    }

    /**
     * Returns this property value as a Float. See {@link Float#parseFloat(String)} for the behavior of this method.
     *
     * <p>This may return <code>null</code></p>
     *
     * @return the value as a Float
     */
    @Nullable
    public Float getFloatValue() {
        return (stringValue == null) ? null : Float.valueOf(stringValue);
    }

    /**
     * Sets the value of this property to the <code>toString</code> form of the given <code>value</code>. This object
     * will not hold a reference to the <code>value</code>.
     *
     * <p>Calling this method is the same as if calling {@link #setValue(Object)}.</p>
     *
     * @param value
     */
    public void setFloatValue(@Nullable Float value) {
        this.setValue(value);
    }

    /**
     * Returns this property value as a Double. See {@link Double#parseDouble(String)} for the behavior of this method.
     *
     * <p>This may return <code>null</code></p>
     *
     * @return the value as a Double
     */
    @Nullable
    public Double getDoubleValue() {
        return (stringValue == null) ? null : Double.valueOf(stringValue);
    }

    /**
     * Sets the value of this property to the <code>toString</code> form of the given <code>value</code>. This object
     * will not hold a reference to the <code>value</code>.
     *
     * <p>Calling this method is the same as if calling {@link #setValue(Object)}.</p>
     *
     * @param value
     */
    public void setDoubleValue(@Nullable Double value) {
        this.setValue(value);
    }

    public Boolean getOverride() {
        return this.override;
    }

    public void setOverride(Boolean override) {
        this.override = override;
    }

    public boolean isMasked() {
        return MASKED_VALUE.equals(this.stringValue);
    }

    public void mask() {
        // Don't mask properties with null values (i.e. unset properties), otherwise they will appear to have a
        // value when rendered in the GUI (see http://jira.jboss.com/jira/browse/JBNADM-2248).
        if (this.stringValue != null) {
            this.stringValue = MASKED_VALUE;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof PropertySimple)) {
            return false;
        }

        if (!super.equals(obj)) {
            return false; // superclass checks equality of the name fields
        }

        PropertySimple that = (PropertySimple) obj;

        // Treat empty string as if it were null See JBNADM-2715
        String compareToA = ((this.stringValue != null) && (this.stringValue.length() == 0)) ? null : this.stringValue;
        String compareToB = ((that.stringValue != null) && (that.stringValue.length() == 0)) ? null : that.stringValue;

        if ((compareToA != null) ? (!compareToA.equals(compareToB)) : (compareToB != null)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode(); // superclass hashCode is derived from the name field
        result = (31 * result) + ((this.stringValue != null) ? this.stringValue.hashCode() : 0);
        return result;
    }

    public PropertySimple deepCopy(boolean keepId) {
        return new PropertySimple(this, keepId);
    }

    @Override
    protected void appendToStringInternals(StringBuilder str) {
        super.appendToStringInternals(str);
        str.append(", value=").append(getStringValue());
        str.append(", override=").append(getOverride());
    }
}