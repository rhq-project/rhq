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
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author Lukas Krejci
 */
@XmlRootElement(name = "rel")
@XmlAccessorType(XmlAccessType.FIELD)
public class Relationship {

    @XmlAttribute
    private String sourceField;
    
    @XmlAttribute
    private String targetField;
    
    @XmlIDREF
    @XmlAttribute
    private Entity targetEntity;

    @XmlTransient
    private Entity sourceEntity;
    
    /**
     * @return the fromField
     */
    public String getSourceField() {
        return sourceField;
    }

    /**
     * @param fromField the fromField to set
     */
    public void setSourceField(String fromField) {
        this.sourceField = fromField;
    }

    /**
     * @return the toField
     */
    public String getTargetField() {
        return targetField;
    }

    /**
     * @param toField the toField to set
     */
    public void setTargetField(String toField) {
        this.targetField = toField;
    }

    /**
     * @return the targetNode
     */
    public Entity getTargetEntity() {
        return targetEntity;
    }

    /**
     * @param targetNode the targetNode to set
     */
    public void setTargetEntity(Entity targetNode) {
        this.targetEntity = targetNode;
    }

    /**
     * @return the sourceNode
     */
    public Entity getSourceEntity() {
        return sourceEntity;
    }

    /**
     * @param sourceNode the sourceNode to set
     */
    public void setSourceEntity(Entity sourceNode) {
        this.sourceEntity = sourceNode;
    }
    
    public int hashCode() {
        int fromHash = sourceField == null ? 1 : sourceField.hashCode();
        int toHash = targetField == null ? 1 : targetField.hashCode();
        int targetHash = targetEntity == null ? 1 : targetEntity.hashCode();
        
        return fromHash * toHash * targetHash;
    }
    
    public boolean equals(Object other) {
        if (!(other instanceof Relationship)) {
            return false;
        }
        
        Relationship o = (Relationship) other;
        
        boolean fromEq = sourceField == null ? o.sourceField == null : sourceField.equals(o.sourceField);
        boolean toEq = targetField == null ? o.targetField == null : targetField.equals(o.targetField);
        boolean targetEq = targetEntity == null ? o.targetEntity == null : targetEntity.equals(o.targetEntity);
        
        return fromEq && toEq && targetEq;
    }
}
