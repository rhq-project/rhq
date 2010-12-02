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

package org.rhq.helpers.perftest.support.jpa;

import java.lang.reflect.Field;

import org.rhq.helpers.perftest.support.jpa.mapping.RelationshipTranslation;

/**
 * Represents an edge in the {@link EntityDependencyGraph}.
 *
 * @author Lukas Krejci
 */
public class Edge {
    
    private Node from;
    
    private Node to;

    private Field fromField;
    
    private Field toField;
    
    private RelationshipTranslation translation;
    
    private DependencyType dependencyType;
    
    public Edge(Node from, Node to, Field fromField, Field toField, DependencyType dependencyType) {
        this.from = from;
        this.to = to;
        this.fromField = fromField;
        this.toField = toField;
        this.dependencyType = dependencyType;
    }

    /**
     * @return the node this edge comes from.
     */
    public Node getFrom() {
        return from;
    }

    /**
     * @return the node this edge goes to.
     */
    public Node getTo() {
        return to;
    }

    /**
     * @return  the field on the "from node"'s class that this edge represents. 
     */
    public Field getFromField() {
        return fromField;
    }

    /**
     * @return the field on the "to node"'s class that this edge represents.
     */
    public Field getToField() {
        return toField;
    }

    /**
     * @return the dependencyType
     */
    public DependencyType getDependencyType() {
        return dependencyType;
    }

    /**
     * The translation of this edge to the terms of SQL table and column names.
     * @return
     */
    public RelationshipTranslation getTranslation() {
        return translation;
    }

    public void setTranslation(RelationshipTranslation translation) {
        this.translation = translation;
    }
        
    
    public int hashCode() {
        return from.hashCode() * to.hashCode();
    }

    public boolean equals(Object other) {
        if (!(other instanceof Edge)) {
            return false;
        }

        Edge o = (Edge) other;

        boolean fromEqual = fromField == null ? o.fromField == null : fromField.equals(o.fromField);
        boolean toEqual = toField == null ? o.toField == null : toField.equals(o.toField);
        return fromEqual && toEqual;
    }
}
