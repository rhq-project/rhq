/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.core.domain.configuration;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PostLoad;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.obfuscation.Obfuscator;

/**
 * This is a specialization of {@link PropertySimple} that provides password obfuscation
 * methods.
 *
 * In memory, the value is always kept in clear text. It is only at persisting or
 * serialization time that the value is stored in its obfuscated form.
 * 
 * @author Lukas Krejci
 */
@DiscriminatorValue("obfuscated")
@Entity
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class ObfuscatedPropertySimple extends PropertySimple {

    //note that while there were changes since RHQ 4.4.0.GA (or JON 3.1.0.GA) version of this class
    //the serialization has NOT changed. The over-the-wire format of this class remained the same.
    //Hence, serializationVersionUID is still at 1.
    private static final long serialVersionUID = 1L;

    private static final Log LOG = LogFactory.getLog(ObfuscatedPropertySimple.class);
    
    private transient String clearTextValue;
    
    public ObfuscatedPropertySimple() {
    }

    /**
     * A conversion constructor - makes the provided unobfuscated simple property
     * an obfuscated one.
     * 
     * @param unobfuscated
     */
    public ObfuscatedPropertySimple(PropertySimple unobfuscated) {
        this(unobfuscated, true);
    }
    
    /**
     * @param original
     * @param keepId
     */
    protected ObfuscatedPropertySimple(PropertySimple original, boolean keepId) {
        super(original, keepId);
        setValue(original.getStringValue());
    }

    /**
     * @param original
     * @param keepId
     */
    protected ObfuscatedPropertySimple(ObfuscatedPropertySimple original, boolean keepId) {
        super(original, keepId);
        this.clearTextValue = original.clearTextValue;
    }

    /**
     * @param name
     * @param value
     */
    public ObfuscatedPropertySimple(String name, Object value) {
        super(name, null);
        setValue(value);
    }

    @Override
    public PropertySimple deepCopy(boolean keepId) {
        return new ObfuscatedPropertySimple(this, keepId);
    }
    
    @PostLoad
    protected void initClearTextValue() {
        clearTextValue = deobfuscate(getObfuscatedStringValue());
    }
       
    /**
     * @return the value as being stored in the database
     */
    public String getObfuscatedStringValue() {
        return super.getStringValue();
    }
    
    /**
     * The value of this property as string. Note that this is always in "clear text". I.e. the value
     * you get from this method is NOT obfuscated (but it gets stored in the database obfuscated).
     */
    @Override
    public String getStringValue() {
        if (clearTextValue == null) {
            initClearTextValue();
        }
        return clearTextValue;
    }
    
    /**
     * Sets the value of this property. You should pass the "clear text" value - the obfuscation of
     * the value in the database is done for you behind the scenes.
     */
    @Override
    public void setValue(Object value) {
        //just use the logic in the superclass to set the value
        super.setValue(value);
        //and obtain the result
        this.clearTextValue = super.getStringValue();
        
        //now set the underlying value to the obfuscated one
        super.setValue(obfuscate(clearTextValue));
        
        //now we have the clear text string representation of the value in "clearTextValue",
        //the stringValue in the superclass contains the corresponding obfuscated string.
    }
    
    
    @Override
    public Boolean getBooleanValue() {
        String val = getStringValue();
        return val == null ? null : Boolean.valueOf(val);
    }

    @Override
    public Long getLongValue() {
        String val = getStringValue();
        return val == null ? null : Long.valueOf(val);
    }

    @Override
    public Integer getIntegerValue() {
        String val = getStringValue();
        return val == null ? null : Integer.valueOf(val);
    }

    @Override
    public Float getFloatValue() {
        String val = getStringValue();
        return val == null ? null : Float.valueOf(val);
    }

    @Override
    public Double getDoubleValue() {
        String val = getStringValue();
        return val == null ? null : Double.valueOf(val);
    }

    @Override
    public boolean isMasked() {
        return MASKED_VALUE.equals(getStringValue());
    }

    @Override
    public void mask() {
        if (getStringValue() != null) {
            setValue(MASKED_VALUE);
        }
    }

    protected String deobfuscate(String value) {
        try {
            return value == null ? null : Obfuscator.decode(value);
        } catch (NumberFormatException nfe) {//detect unobfuscated properties from before patch
            //Assuming that this was in incorrect state from BZ840512
            //logging that we found an unobfuscated value and if it's not part of patch/upgrade contact administrator
            LOG.error("Failed to deobfuscate property value: [" + value + "]. If this is not part of a patch/upgrade "
                + "then you should contact System Administrator to have the property details reset.");
            //Returning plain value to prevent Content Source load failure. On save should be correctly obfuscated
            return value;
        } catch (Exception e) {
            LOG.error("Failed to deobfuscate property value: [" + value + "]", e);
            throw new IllegalArgumentException("Failed to deobfuscate property value: [" + value + "]", e);
        }
    }
    
    /**
     * Obfuscate the value right before it gets pushed down to the database.
     */
    protected String obfuscate(String value) {
        try {
            return value == null ? null : Obfuscator.encode(value);
        } catch (Exception e) {
            LOG.error("Failed to obfuscate property value: [" + value + "]", e);
            throw new IllegalArgumentException("Failed to obfuscate property value: [" + value + "]", e);
        }
    }
    
    /**
     * Overriden to not leak the unobfuscated value in the toString() method, output of which
     * might end up in logs, etc.
     */
    @Override
    protected void appendToStringInternals(StringBuilder str) {
        str.append(", obfuscated-value=").append(getObfuscatedStringValue());
        str.append(", override=").append(getOverride());
    };

    private void writeObject(ObjectOutputStream str) throws IOException {
        str.defaultWriteObject();        
    }
    
    private void readObject(ObjectInputStream str) throws IOException, ClassNotFoundException {
        str.defaultReadObject();
        initClearTextValue();
    }
}
