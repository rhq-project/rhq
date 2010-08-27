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

package org.rhq.helpers.perftest.support;

import static org.rhq.helpers.perftest.support.util.JPAUtil.getJPAFields;
import static org.rhq.helpers.perftest.support.util.JPAUtil.isEntity;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.rhq.helpers.perftest.support.util.Annotations;

/**
 *
 * @author Lukas Krejci
 */
public class EntityDependencyGraph {

    Map<Node, Node> nodes = new HashMap<Node, Node>();

    public enum DependencyType {
        ONE_TO_ONE { 
            public Class<? extends Annotation> annotationType() {
                return OneToOne.class;
            }
            
            public DependencyType getOpposite() {
                return ONE_TO_ONE;
            }
        }, 
        MANY_TO_ONE{
            public Class<? extends Annotation> annotationType() {
                return ManyToOne.class;
            }
            
            public DependencyType getOpposite() {
                return ONE_TO_MANY;
            }
        }, 
        ONE_TO_MANY{
            public Class<? extends Annotation> annotationType() {
                return OneToMany.class;
            }
            
            public DependencyType getOpposite() {
                return MANY_TO_ONE;
            }
        }, 
        MANY_TO_MANY{
            public Class<? extends Annotation> annotationType() {
                return ManyToMany.class;
            }
            
            public DependencyType getOpposite() {
                return MANY_TO_MANY;
            }
        };
        
        public abstract DependencyType getOpposite();
        
        public abstract Class<? extends Annotation> annotationType();
    }
    
    public static class Edge {
        private Node from;
        private Node to;
        
        private Field fromField;
        private Field toField;
    
        private DependencyType dependencyType;
        
        public Edge(Node from, Node to, Field fromField, Field toField, DependencyType dependencyType) {
            this.from = from;
            this.to = to;
            this.fromField = fromField;
            this.toField = toField;
            this.dependencyType = dependencyType;
        }

        /**
         * @return the from
         */
        public Node getFrom() {
            return from;
        }

        /**
         * @return the to
         */
        public Node getTo() {
            return to;
        }

        /**
         * @return the fromField
         */
        public Field getFromField() {
            return fromField;
        }

        /**
         * @return the toField
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
        
        public int hashCode() {
            return from.hashCode() * to.hashCode();
        }
        
        public boolean equals(Object other) {
            if (!(other instanceof Edge)) {
                return false;
            }
            
            Edge o = (Edge) other;
         
            return fromField == o.fromField && toField == o.toField;
        }
    }
    
    public static class Node {
        private Class<?> entity;
        private Set<Edge> edges;

        public Node(Class<?> entity) {
            this.entity = entity;
            edges = new HashSet<Edge>();
        }

        public String getTable() {
            Table tableAnnotation = entity.getAnnotation(Table.class);

            if (tableAnnotation == null) {
                //I'm sure there are more complicated rules than this...
                return entity.getSimpleName().toUpperCase();
            }

            return tableAnnotation.name();
        }

        public Class<?> getEntity() {
            return entity;
        }

        public void addParent(Node parent, Field parentField, Field thisField, DependencyType dependencyType) {
            Edge edge = new Edge(parent, this, parentField, thisField, dependencyType);
            
            if (edges.add(edge)) {
                parent.addChild(this, thisField, parentField, dependencyType.getOpposite());
            }
        }

        public void addChild(Node child) {
            if (children.add(child)) {
                child.addParent(this);
            }
        }

        public Set<Edge> getEdges() {
            return edges;
        }
        
        public int hashCode() {
            return entity.hashCode();
        }

        /**
         * Returns true if other is a Node representing the same entity
         * *OR* if other is a Class instance representing the same class
         * as the entity of this node.
         * 
         * This basically violates the contract of Object.equals() but
         * is extremely handy for quick lookup in maps where nodes are keys
         * without needing to create a new node instance.
         */
        public boolean equals(Object other) {
            if (!(other instanceof Node)) {

                if (other instanceof Class) {
                    return entity.equals(other);
                }
            }

            return entity.equals(((Node) other).getEntity());
        }
    }

    public Node addEntity(Class<?> entity) {
        Node n = new Node(entity);
        return analyze(n);
    }

    public Set<Node> getRootNodes() {
        Set<Node> ret = new HashSet<Node>();

        for (Node n : nodes.keySet()) {
            if (n.getParents().isEmpty()) {
                ret.add(n);
            }
        }

        return ret;
    }

    public Set<Node> getLeafNodes() {
        Set<Node> ret = new HashSet<Node>();

        for (Node n : nodes.keySet()) {
            if (n.getChildren().isEmpty()) {
                ret.add(n);
            }
        }

        return ret;
    }

    public String toString() {
        StringBuilder bld = new StringBuilder();
        bld.append("Child tree:\n");
        toString(bld, getRootNodes(), false);
        bld.append("\nParent tree:\n");
        toString(bld, getLeafNodes(), true);
        return bld.toString();
    }

    private void toString(StringBuilder bld, Set<Node> nodes, boolean parentTree) {
        bld.append("[");
        for (Node root : nodes) {
            bld.append("\n");
            root.indentingToString(bld, 0, parentTree);
        }
        bld.append("\n]");
    }

    /**
     * Analyzes the node and returns true if the node was incorporated into the 
     * dependency graph (or if it was already there).
     * 
     * @param n the node to analyze.
     * @return
     */
    private Node analyze(Node n) {
        //skip if already analyzed
        Node existingNode = nodes.get(n);
        if (existingNode != null) {
            return existingNode;
        }

        //we only care about JPA entities
        if (!isEntity(n.getEntity())) {
            return null;
        }

        //first, let's add the node to the set so that we prevent
        //recursion on possible circular references further below.
        nodes.put(n, n);

        //RHQ has convention of declaring the JPA annotations strictly
        //on fields.
        Map<Field, Annotations> fields = getJPAFields(n.getEntity());

        for (Map.Entry<Field, Annotations> entry : fields.entrySet()) {
            Field field = entry.getKey();
            Annotations annotations = entry.getValue();

            OneToOne oneToOne = annotations.get(OneToOne.class);
            ManyToOne manyToOne = annotations.get(ManyToOne.class);
            OneToMany oneToMany = annotations.get(OneToMany.class);
            ManyToMany manyToMany = annotations.get(ManyToMany.class);
            if (oneToOne != null) {
                analyzeOneToOne(n, field, annotations, oneToOne);
            } else if (manyToOne != null) {
                analyzeManyToOne(n, field, annotations, manyToOne);
            } else if (oneToMany != null) {
                analyzeOneToMany(n, field, annotations, oneToMany);
            } else if (manyToMany != null) {
                analyzManyToMany(n, field, annotations, manyToMany);
            }
        }

        return n;
    }

    private void analyzeOneToOne(Node n, Field field, Annotations annotations, OneToOne oneToOne) {
        Class<?> targetEntity = oneToOne.targetEntity();
        analyzeCommon(n, field, targetEntity, false);
    }

    private void analyzeManyToOne(Node n, Field field, Annotations annotations, ManyToOne manyToOne) {
        Class<?> targetEntity = manyToOne.targetEntity();
        analyzeCommon(n, field, targetEntity, true);
    }

    private void analyzeOneToMany(Node n, Field field, Annotations annotations, OneToMany oneToMany) {
        Class<?> targetEntity = oneToMany.targetEntity();
        analyzeCommon(n, field, targetEntity, false);
    }

    private void analyzManyToMany(Node n, Field field, Annotations annotations, ManyToMany manyToMany) {
        Class<?> targetEntity = manyToMany.targetEntity();
        analyzeCommon(n, field, targetEntity, false);
    }

    private void analyzeCommon(Node n, Field field, Class<?> declaredTargetEntity, boolean asParent) {
        if (declaredTargetEntity == null || declaredTargetEntity == void.class) {
            declaredTargetEntity = field.getType();
        }

        if (isCollection(declaredTargetEntity)) {
            Type type = field.getGenericType();

            declaredTargetEntity = getCollectionTypeParameter(type, 0);
        }

        if (isMap(declaredTargetEntity)) {
            Type type = field.getGenericType();

            declaredTargetEntity = getCollectionTypeParameter(type, 1);
        }

        Node targetNode = analyze(new Node(declaredTargetEntity));

        if (asParent) {
            //avoid cycles in the graph
            if (!n.getTransitiveChildren().contains(targetNode)) {
                n.addParent(targetNode);
            }
        } else {
            //avoid cycles in the graph
            if (!n.getTransitiveParents().contains(targetNode)) {
                n.addChild(targetNode);
            }
        }
    }

    private static boolean isCollection(Class<?> clazz) {
        return clazz == Collection.class || clazz == List.class || clazz == Set.class;
    }

    private static boolean isMap(Class<?> clazz) {
        return clazz == Map.class;
    }

    /**
     * Returns the class of the collection's type parameter.
     * 
     * @param collectionType
     * @return
     */
    private static Class<?> getCollectionTypeParameter(Type collectionType, int parameterPosition) {
        if (collectionType instanceof ParameterizedType) {
            Type[] typeArguments = ((ParameterizedType) collectionType).getActualTypeArguments();
            return resolveTypeParameter(typeArguments[parameterPosition]);
        } else {
            return (Class<?>) collectionType;
        }
    }

    private static Class<?> resolveTypeParameter(Type typeParameter) {
        if (typeParameter instanceof WildcardType) {
            Type[] upperBounds = ((WildcardType) typeParameter).getUpperBounds();
            if (upperBounds.length > 1) {
                return Object.class;
            } else {
                return (Class<?>) upperBounds[0];
            }
        } else {
            return (Class<?>) typeParameter;
        }
    }

    public static void main(String[] args) throws Exception {
        EntityDependencyGraph g = new EntityDependencyGraph();
        g.addEntity(Class.forName("org.rhq.core.domain.resource.Resource"));
        System.out.println(g);
    }
}
