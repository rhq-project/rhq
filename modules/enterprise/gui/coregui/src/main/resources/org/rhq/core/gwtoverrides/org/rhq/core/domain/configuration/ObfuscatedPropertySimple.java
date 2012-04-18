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

/**
 * A custom GWT-friendly implementation of {@link ObfuscatedPropertySimple}. This file
 * must be kept in "functional sync" with the version "real" in the core/domain 
 * module.
 *
 * @author Lukas Krejci
 */
public class ObfuscatedPropertySimple extends PropertySimple {

    private static final long serialVersionUID = 1L;

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
    public ObfuscatedPropertySimple deepCopy(boolean keepId) {
        return new ObfuscatedPropertySimple(this, keepId);
    }    
}
