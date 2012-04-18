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
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
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
 * @author Lukas Krejci
 */
@DiscriminatorValue("obfuscated")
@Entity
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class ObfuscatedPropertySimple extends PropertySimple {

    private static final long serialVersionUID = 1L;

    private static final Log LOG = LogFactory.getLog(ObfuscatedPropertySimple.class);
    
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
    }

    /**
     * @param name
     * @param value
     */
    public ObfuscatedPropertySimple(String name, Object value) {
        super(name, value);
    }

    @Override
    public PropertySimple deepCopy(boolean keepId) {
        return new ObfuscatedPropertySimple(this, keepId);
    }
    
    /**
     * We deobfuscate right after the entity has been loaded from the database or right
     * after we persist or update the value.
     * 
     * Because we change the value before persist or update, we have to swap the value back
     * as soon as those DB changes are done, so that we only use the raw value in memory.
     */
    @PostLoad
    @PostPersist
    @PostUpdate
    protected void deobfuscate() {
        String value = getStringValue();
        if (value != null) {
            try {
                setStringValue(Obfuscator.decode(getStringValue()));
            } catch (Exception e) {
                LOG.error("Failed to deobfuscate property value: [" + value + "]", e);
            }
        }
    }
    
    /**
     * Obfuscate the value right before it gets pushed down to the database.
     */
    @PrePersist
    @PreUpdate
    protected void obfuscate() {
        String value = getStringValue();
        if (value != null) {
            try {
                setStringValue(Obfuscator.encode(value));
            } catch (Exception e) {
                LOG.error("Failed to obfuscate property value: [" + value + "]", e);
            }
        }
    }
    
    private void writeObject(ObjectOutputStream str) throws IOException {
        obfuscate();
        str.defaultWriteObject();        
    }
    
    private void readObject(ObjectInputStream str) throws IOException, ClassNotFoundException {
        str.defaultReadObject();
        deobfuscate();
    }
}
