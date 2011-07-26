/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.core.domain.sync.entity;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 *
 * @author Lukas Krejci
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class AbstractExportedEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlAttribute
    int referencedEntityId;
    
    public int getReferencedEntityId() {
        return referencedEntityId;
    }

    public void setReferencedEntityId(int referencedEntityId) {
        this.referencedEntityId = referencedEntityId;
    }
    
    @Override
    public int hashCode() {
        return referencedEntityId;
    }
    
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        
        if (other == null) {
            return false;
        }
        
        //this is important because we only want to match the entities if their types
        //match.. I.e. I don't want a Subject with id = 1 to match Role with id = 1.
        //subclasses are free to override this method of course, but this is a sensible
        //default thing to do.
        if (!other.getClass().equals(getClass())) {
            return false;
        }
        
        return referencedEntityId == ((AbstractExportedEntity)other).getReferencedEntityId();
    }
}
