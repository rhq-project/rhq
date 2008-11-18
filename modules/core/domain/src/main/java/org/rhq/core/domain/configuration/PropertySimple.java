 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
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
public class PropertySimple extends Property implements Externalizable {
    public static final int MAX_VALUE_LENGTH = 2000;

    private static final long serialVersionUID = 1L;

    @Column(name = "override")
    private Boolean override;

    @Column(name = "string_value", length = MAX_VALUE_LENGTH)
    private String stringValue;

    @Transient
    private transient String unmaskedStringValue;

    /**
     * Constructor for {@link PropertySimple} that stores a <code>null</code> value.
     * NOTE: When using this constructor, you need to supply a name via #setName manually.
     */
    public PropertySimple() {
        override = Boolean.FALSE;
    }

    /**
     * Constructs a property that has the given name with the given value. The value's <code>toString</code>
     * representation will be stored in this object - <code>value</code> itself will not be stored (that is, this object
     * will not hold a reference to <code>value</code>).
     *
     * @param name
     * @param value
     */
    public PropertySimple(@NotNull
    String name, @Nullable
    Object value) {
        setName(name);
        setValue(value);
    }

    /**
     * Sets the value of this property to the <code>toString</code> form of the given <code>value</code>. This object
     * will not hold a reference to <code>value</code>.
     *
     * @param value
     */
    public void setValue(@Nullable
    Object value) {
        if (value == null) {
            stringValue = null;
            return;
        }

        String sVal = value.toString();
        if (sVal.length() > MAX_VALUE_LENGTH)
            stringValue = sVal.substring(0, MAX_VALUE_LENGTH);
        else
            stringValue = sVal;
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
    public void setStringValue(@Nullable
    String value) {
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
    public void setBooleanValue(@Nullable
    Boolean value) {
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
    public void setLongValue(@Nullable
    Long value) {
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
    public void setIntegerValue(@Nullable
    Integer value) {
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
    public void setFloatValue(@Nullable
    Float value) {
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
    public void setDoubleValue(@Nullable
    Double value) {
        this.setValue(value);
    }

    public Boolean getOverride() {
        return this.override;
    }

    public void setOverride(Boolean override) {
        this.override = override;
    }

    public String getUnmaskedStringValue() {
        return this.unmaskedStringValue;
    }

    public void setUnmaskedStringValue(String unmaskedStringValue) {
        this.unmaskedStringValue = unmaskedStringValue;
    }

    /**
     * @see org.rhq.core.domain.configuration.Property#readExternal(java.io.ObjectInput)
     */
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        stringValue = (String) in.readObject();
        override = in.readBoolean();
    }

    /**
     * @see org.rhq.core.domain.configuration.Property#writeExternal(java.io.ObjectOutput)
     */
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(stringValue);
        if (override == null) {
            out.writeBoolean(Boolean.FALSE);
        } else {
            out.writeBoolean(override);
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

    @Override
    protected void appendToStringInternals(StringBuilder str) {
        super.appendToStringInternals(str);
        str.append(", value=").append(getStringValue());
        str.append(", override=").append(getOverride());
    }
}