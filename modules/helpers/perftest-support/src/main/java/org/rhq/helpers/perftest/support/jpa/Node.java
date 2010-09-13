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

/**
 * Represents a node in the entity dependency graph.
 *
 * @author Lukas Krejci
 */
public class Node implements Comparable<Node> {

    private Class<?> entity;

    private Set<Edge> edges;

    private EntityTranslation translation;

    public Node(Class<?> entity) {
        this.entity = entity;
        edges = new HashSet<Edge>();
    }

    /**
     * The class of the JPA entity this node represents.
     */
    public Class<?> getEntity() {
        return entity;
    }

    /**
     * @return the translation of this entity to the SQL terms.
     */
    public EntityTranslation getTranslation() {
        return translation;
    }

    public void setTranslation(EntityTranslation translation) {
        this.translation = translation;
    }

    /**
     * Add a parent node. This creates a new edge with the parent node
     * set as the "from" node and this node as the "to" node. The edge instance is
     * shared in both this' and parent's edge set.
     * 
     * @param parent the parent node
     * @param parentField the field on the parent node's entity that the edge is to be linked with
     * @param thisField the field on this node's entity that the edge is to be linked with
     * @param dependencyType the type of the JPA dependency
     */
    public void addParent(Node parent, Field parentField, Field thisField, DependencyType dependencyType) {
        Edge edge = new Edge(parent, this, parentField, thisField, dependencyType);

        if (edges.add(edge)) {
            parent.addEdge(edge);
        }
    }

    /**
     * Similar to {@link #addParent(Node, Field, Field, DependencyType)} but creates an edge
     * with this node being the "from" and the child node being the "to" of the newly created edge.
     * 
     * @param child
     * @param childField
     * @param thisField
     * @param dependencyType
     */
    public void addChild(Node child, Field childField, Field thisField, DependencyType dependencyType) {
        Edge edge = new Edge(this, child, thisField, childField, dependencyType);

        if (edges.add(edge)) {
            child.addEdge(edge);
        }
    }

    /**
     * This method is called from {@link #addParent(Node, Field, Field, DependencyType)} and
     * {@link #addChild(Node, Field, Field, DependencyType)} methods to actually store an instance
     * of an edge in this instance's edge set.
     * 
     * @param edge
     */
    protected void addEdge(Edge edge) {
        edges.add(edge);
    }

    /**
     * @return all the edges leading from or to this node.
     */
    public Set<Edge> getEdges() {
        return edges;
    }

    /**
     * @return the edges going out of this node (i.e. edges leading to children of this node)
     */
    public Set<Edge> getOutgoingEdges() {
        Set<Edge> ret = new HashSet<Edge>();
        for (Edge e : edges) {
            if (this == e.getFrom()) {
                ret.add(e);
            }
        }

        return ret;
    }

    /**
     * @return the edges coming to this node (i.e. edges leading from parents of this node)
     */
    public Set<Edge> getIncomingEdges() {
        Set<Edge> ret = new HashSet<Edge>();
        for (Edge e : edges) {
            if (this == e.getTo()) {
                ret.add(e);
            }
        }

        return ret;
    }

    /**
     * Return the parents of this node.
     * When the <code>onlyExplicitRelations</code> is true, then only
     * the edges that have and non-null {@link Edge#getFromField() "from" field} are considered.
     * This means that only parents that explicitly link to this entity are considered.
     * A null from field means that the relation was only defined on the "to" side and that
     * the parent entity has no explicit knowledge of the relationship.
     *  
     * @param onlyExplicitRelations
     * @return
     */
    public SortedSet<Node> getParents(boolean onlyExplicitRelations) {
        SortedSet<Node> ret = new TreeSet<Node>();

        for (Edge e : edges) {
            //== *is* correct here
            if (e.getTo() == this && (onlyExplicitRelations ? e.getFromField() != null : true)) {
                ret.add(e.getFrom());
            }
        }

        return ret;
    }

    /**
     * Returns the children of this node.
     * The <code>onlyExplicitRelations</code> argument has the same meaning as for the {@link #getParents(boolean)} method.
     * 
     * @param onlyExplicitRelations
     * @return
     */
    public SortedSet<Node> getChildren(boolean onlyExplicitRelations) {
        SortedSet<Node> ret = new TreeSet<Node>();

        for (Edge e : edges) {
            //== *is* correct here
            if (e.getFrom() == this && (onlyExplicitRelations ? e.getToField() != null : true)) {
                ret.add(e.getTo());
            }
        }

        return ret;
    }

    /**
     * Returns this node, its parents, their parents, etc, recursively up the dependency graph.
     * 
     * @param onlyExplicitRelations the same meaning as for {@link #getParents(boolean)}
     * @return
     */
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

    /**
     * Returns this node, its children, their children, etc. recursively.
     * 
     * @param onlyExplicitRelations the same meaning as for {@link #getParents(boolean)}
     * @return
     */
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
        for (Edge e : edges) {
            String fromField = e.getFromField() != null ? e.getFromField().getName() : "?";
            String toField = e.getToField() != null ? e.getToField().getName() : "?";

            if (e.getFrom() == this) {
                bld.append("(").append(fromField);
                bld.append(") -").append(e.getDependencyType()).append("> ")
                    .append(e.getTo().getEntity().getSimpleName());
                bld.append("(").append(toField).append(")");
            } else {
                bld.append("(").append(toField);
                bld.append(") <").append(e.getDependencyType()).append("- ")
                    .append(e.getFrom().getEntity().getSimpleName());
                bld.append("(").append(fromField).append(")");
            }
            bld.append("\n").append("(attachment=").append(e.getTranslation()).append(")\n");
        }
        bld.append("]");

        return bld.toString();
    }
}
