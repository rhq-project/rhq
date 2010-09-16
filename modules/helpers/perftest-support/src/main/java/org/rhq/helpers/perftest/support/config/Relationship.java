/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.helpers.perftest.support.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a relationship between two {@link Entity entities}.
 * @author Lukas Krejci
 */
@XmlRootElement(name = "rel")
@XmlAccessorType(XmlAccessType.FIELD)
public class Relationship {

    @XmlAttribute
    private String field;
    
    @XmlAttribute
    private Boolean exclude;
    
    /**
     * @return the name of the field on the owning {@link Entity} that represents the relationship.
     */
    public String getField() {
        return field;
    }

    /**
     * @param fromField the fromField to set
     */
    public void setField(String fromField) {
        this.field = fromField;
    }
    
    public boolean isExclude() {
        return exclude != null && exclude.booleanValue();
    }
    
    public void setExclude(boolean exclude) {
        this.exclude = exclude;
    }
    
    public int hashCode() {
        int hash = field == null ? 1 : field.hashCode();
        return hash;
    }
    
    public boolean equals(Object other) {
        if (!(other instanceof Relationship)) {
            return false;
        }
        
        Relationship o = (Relationship) other;
        
        return field == null ? o.field == null : field.equals(o.field);
    }
}
