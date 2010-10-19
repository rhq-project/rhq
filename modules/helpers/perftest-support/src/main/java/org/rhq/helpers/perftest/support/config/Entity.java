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

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents an entity in the dependency graph.
 * 
 * @author Lukas Krejci
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Entity {

    @XmlID
    @XmlAttribute
    private String name;

    @XmlAttribute
    private Boolean includeAllFields;

    @XmlAttribute
    private Boolean root;
    
    @XmlElement(name = "rel")
    private Set<Relationship> relationships = new HashSet<Relationship>();

    @XmlElement(name = "filter")
    private String filter;

    /**
     * @return the relationships defined on this entity.
     */
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    public void setRelationships(Set<Relationship> relationships) {
        this.relationships = relationships;
    }

    /**
     * @return the name of this entity
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return true if all fields on this entity are considered as included 
     * (as opposed to  explicitly defined in the {@link #getRelationships()}). 
     */
    public boolean isIncludeAllFields() {
        return includeAllFields == null ? false : includeAllFields;
    }

    /**
     * @param allDependents the allDependents to set
     */
    public void setIncludeAllFields(Boolean allDependents) {
        this.includeAllFields = allDependents;
    }

    /**
     * @return true if this entity is to be considered the root of the exported hierarchy.
     * The root entities are considered the base of the exported entity graph. All other non-root
     * entities only contain additional configuration for given resources should they appear
     * in the entity dependency graph as a dependency or dependent of some of the root entities.
     */
    public boolean isRoot() {
        return root == null ? false : root;
    }
    
    public void setRoot(boolean root) {
        this.root = root;
    }
    
    /**
     * @return the SQL statement that returns the primary keys to be considered. Setting this
     * property only makes sense for {@link #isRoot() root} entities.
     */
    public String getFilter() {
        return filter;
    }

    /**
     * @param filter the filter to set
     */
    public void setFilter(String filter) {
        this.filter = filter;
    }

    public int hashCode() {
        return name.hashCode();
    }

    public boolean equals(Object other) {
        if (!(other instanceof Entity)) {
            return false;
        }

        return name.equals(((Entity) other).name);
    }
}
