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
package org.rhq.core.system.windows;

/**
 * Denotes a value of a registry entry.
 *
 * @author John Mazzitelli
 */
public abstract class RegistryValue {
    public enum Type {
        /**
         * Binary data in the form of a byte array.
         */
        BINARY,

        /**
         * A 32-bit number.
         */
        DWORD,

        /**
         * A 64-bit number.
         */
        QWORD,

        /**
         * An array of strings.
         */
        MULTI_SZ,

        /**
         * A string that contains references to environment variables.
         */
        EXPAND_SZ,

        /**
         * A string.
         */
        SZ
    }

    private final Type type;
    private final Object value;

    protected RegistryValue(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    /**
     * Indicates the registry value's type. This is not a Java type - it is the type of the registry item.
     *
     * @return type of registry
     */
    public Type getType() {
        return this.type;
    }

    /**
     * Returns the value as a generic Object, which may be of a different type depending on the value of
     * {@link #getType()}. Subclasses will have additional methods to allow the caller to get a more strongly-typed
     * object.
     *
     * @return the registry value
     */
    public Object getValue() {
        return this.value;
    }

    public String toString() {
        return "[" + getType() + "]" + getValue();
    }
}