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
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.rhq.helpers.perftest.support.jpa.mapping.EntityTranslation;

public class Node implements Comparable<Node> {
    
    private Class<?> entity;
    
    private Set<Edge> edges;
    
    private EntityTranslation translation;
    
    public Node(Class<?> entity) {
        this.entity = entity;
        edges = new HashSet<Edge>();
    }

    public Class<?> getEntity() {
        return entity;
    }

    public EntityTranslation getTranslation() {
        return translation;
    }
    
    public void setTranslation(EntityTranslation translation) {
        this.translation = translation;
    }
    
    public void addParent(Node parent, Field parentField, Field thisField, DependencyType dependencyType) {
        Edge edge = new Edge(parent, this, parentField, thisField, dependencyType);

        if (edges.add(edge)) {
            parent.addEdge(edge);
        }
    }

    public void addChild(Node child, Field childField, Field thisField, DependencyType dependencyType) {
        Edge edge = new Edge(this, child, thisField, childField, dependencyType);

        if (edges.add(edge)) {
            child.addEdge(edge);
        }
    }
    
    protected void addEdge(Edge edge) {
        edges.add(edge);
    }
    
    public Set<Edge> getEdges() {
        return edges;
    }

    public Set<Edge> getOutgoingEdges() {
        Set<Edge> ret = new HashSet<Edge>();
        for(Edge e : edges) {
            if (this == e.getFrom()) {
                ret.add(e);
            }
        }
        
        return ret;
    }
    
    public Set<Edge> getIncomingEdges() {
        Set<Edge> ret = new HashSet<Edge>();
        for(Edge e : edges) {
            if (this == e.getTo()) {
                ret.add(e);
            }
        }
        
        return ret;
    }
    
    public SortedSet<Node> getParents(boolean onlyExplicitRelations) {
        SortedSet<Node> ret = new TreeSet<Node>();
        
        for(Edge e : edges) {
            //== *is* correct here
            if (e.getTo() == this && (onlyExplicitRelations ? e.getFromField() != null : true)) { 
                ret.add(e.getFrom());
            }
        }
        
        return ret;
    }
    
    public SortedSet<Node> getChildren(boolean onlyExplicitRelations) {
        SortedSet<Node> ret = new TreeSet<Node>();
        
        for(Edge e : edges) {
            //== *is* correct here
            if (e.getFrom() == this && (onlyExplicitRelations ? e.getToField() != null : true)) {
                ret.add(e.getTo());
            }
        }
        
        return ret;
    }
    
    public SortedSet<Node> getTransitiveParents(boolean onlyExplicitRelations) {
        SortedSet<Node> ret = new TreeSet<Node>();
        
        ret.add(this);
        
        Set<Node> currentParents = getParents(onlyExplicitRelations);
        
        while (!currentParents.isEmpty()) {
            Set<Node> parentsCopy = new HashSet<Node>(currentParents);
            
            if (!ret.addAll(currentParents)) {
                //if the returned set already contained all the current parents,
                //then we're inside a cycle and there's no need to continue.
                break;
            }
            
            currentParents.clear();
            
            for (Node p : parentsCopy) {
                currentParents.addAll(p.getParents(onlyExplicitRelations));
            }
        }
        
        return ret;
    }
    
    public SortedSet<Node> getTransitiveChildren(boolean onlyExplicitRelations) {
        SortedSet<Node> ret = new TreeSet<Node>();
        
        ret.add(this);
        
        Set<Node> currentChildren = getChildren(onlyExplicitRelations);
        
        while (!currentChildren.isEmpty()) {
            Set<Node> childrenCopy = new HashSet<Node>(currentChildren);
            
            if (!ret.addAll(currentChildren)) {
                //if the returned set already contained all the current children,
                //then we're inside a cycle and there's no need to continue.
                break;
            }
            
            currentChildren.clear();
            
            for (Node c : childrenCopy) {
                currentChildren.addAll(c.getChildren(onlyExplicitRelations));
            }
        }
        
        return ret;
    }
    
    public int hashCode() {
        return entity.hashCode();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Node)) {
            return false;
        }

        return entity.equals(((Node) other).getEntity());
    }
    
    public int compareTo(Node o) {
        return entity.getName().compareTo(o.entity.getName());
    }
    
    public String toString() {
        StringBuilder bld = new StringBuilder();
        
        bld.append(entity.getSimpleName()).append("[\n");
        bld.append("attachment=").append(translation).append("\n");
        for(Edge e : edges) {
            String fromField = e.getFromField() != null ? e.getFromField().getName() : "?";
            String toField = e.getToField() != null ? e.getToField().getName() : "?";
            
            if (e.getFrom() == this) {
                bld.append("(").append(fromField);
                bld.append(") -").append(e.getDependencyType()).append("> ").append(e.getTo().getEntity().getSimpleName());
                bld.append("(").append(toField).append(")");
            } else {
                bld.append("(").append(toField);
                bld.append(") <").append(e.getDependencyType()).append("- ").append(e.getFrom().getEntity().getSimpleName());
                bld.append("(").append(fromField).append(")");
            }
            bld.append("\n").append("(attachment=").append(e.getTranslation()).append(")\n");
        }
        bld.append("]");
        
        return bld.toString();
    }
}