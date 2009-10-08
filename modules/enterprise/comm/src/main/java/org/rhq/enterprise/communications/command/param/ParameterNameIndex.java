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
 * Used as the key to the internal Map found in {@link ParametersImpl}. Its {@link #equals(Object)} method only checks
 * the name (which allows the Map to know to only have one parameter with the same name at any one time). It also has an
 * index that can be used for sorting. This object has a natural sort order by its name.
 *
 * <p>This class is packaged scope because it really is for use only by {@link ParametersImpl}; no one else should care
 * about this class or even know that it exists.
 *
 * @author John Mazzitelli
 * @see    ParametersImpl
 */
class ParameterNameIndex implements Comparable, Serializable {
    /**
     * the UID to identify the serializable version of this class. Be careful if you want to change this - parameters in
     * a serialized form may be persisted.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The parameter name.
     */
    private final String m_name;

    /**
     * The parameter index - may be -1 if you don't know or care
     */
    private final int m_index;

    /**
     * Creates a new {@link ParameterNameIndex} object. The index may be -1 if you don't know or care.
     *
     * @param name  the parameter name
     * @param index the parameter index (for sorting)
     */
    public ParameterNameIndex(String name, int index) {
        m_name = name;
        m_index = index;
    }

    /**
     * Returns the index of the parameter - which defines its position in the ordering of all the parameters.
     *
     * @return index (may be -1 if it is not known)
     */
    public int getIndex() {
        return m_index;
    }

    /**
     * Returns the name of the parameter.
     *
     * @return name
     */
    public String getName() {
        return m_name;
    }

    /**
     * Only compares name - doesn't care about the index.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        return ((obj != null) && (obj instanceof ParameterNameIndex)) ? this.m_name
            .equals(((ParameterNameIndex) obj).m_name) : false;
    }

    /**
     * Only use the {@link #getName() name} - don't care about the index.
     *
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return m_name.hashCode();
    }

    /**
     * @see Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object obj) {
        return this.m_name.compareTo(((ParameterNameIndex) obj).m_name);
    }

    /**
     * The {@link #getIndex() index} followed by the {@link #getName() name} separated with a colon (":").
     *
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return m_index + ":" + m_name;
    }
}