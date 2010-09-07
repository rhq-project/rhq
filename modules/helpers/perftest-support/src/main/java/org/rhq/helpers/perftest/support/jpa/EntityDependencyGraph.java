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

import static org.rhq.helpers.perftest.support.jpa.JPAUtil.getJPAFields;
import static org.rhq.helpers.perftest.support.jpa.JPAUtil.isEntity;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.helpers.perftest.support.jpa.mapping.MappingTranslator;

/**
 *
 * @author Lukas Krejci
 */
public class EntityDependencyGraph {

    private static final Log LOG = LogFactory.getLog(EntityDependencyGraph.class);

    Map<Node, Node> nodes = new HashMap<Node, Node>();
    private MappingTranslator mappingTranslator = new MappingTranslator();
    
    public Node addEntity(Class<?> entity) {
        Node n = new Node(entity);
        n = analyze(n);
        
        translateEverything();
        
        return n;
    }

    public Set<Node> addEntities(Class<?>... entities) {
        return addEntities(Arrays.asList(entities));
    }
    
    public Set<Node> addEntities(Collection<Class<?>> entities) {
        Set<Node> ret = new HashSet<Node>();
        for (Class<?> e : entities) {
            ret.add(analyze(new Node(e)));
        }
        
        translateEverything();
        
        return ret;
    }
    
    public Set<Node> getAllNodes() {
        return nodes.keySet();
    }

    public Node getNode(Class<?> entityClass) {
        return nodes.get(new Node(entityClass));
    }

    public Set<Node> getRootNodes() {
        Set<Node> ret = new HashSet<Node>();
        
        for (Node n : nodes.keySet()) {
            if (n.getParents(false).isEmpty()) {
                ret.add(n);
            }
        }
        return ret;
    }
    
    public Set<Node> getLeafNodes() {
        Set<Node> ret = new HashSet<Node>();
        
        for (Node n : nodes.keySet()) {
            if (n.getChildren(false).isEmpty()) {
                ret.add(n);
            }
        }
        return ret;
    }
    
    public String toString() {
        StringBuilder bld = new StringBuilder();
        bld.append("EntityDependencyGraph[\n");
        for (Node n : nodes.keySet()) {
            bld.append(n).append("\n");
        }
        bld.append("]");
        return bld.toString();
    }

    private void translateEverything() {
        for (Node n : getAllNodes()) {
            if (n.getTranslation() == null) {
                n.setTranslation(mappingTranslator.translate(n));
            }
        }
        
        for (Node n : getAllNodes()) {
            for(Edge e : n.getEdges()) {
                if (e.getTranslation() == null) {
                    e.setTranslation(mappingTranslator.translate(e));
                }
            }
        }
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
                analyzeManyToMany(n, field, annotations, manyToMany);
            }
        }

        return n;
    }

    private void analyzeOneToOne(Node n, Field field, Annotations annotations, OneToOne oneToOne) {
        try {
            Class<?> targetEntity = oneToOne.targetEntity();
            Node target = getTargetNode(field, targetEntity);
            Field targetField = null;
            boolean forward = true; //forward = true, backwards = false

            String mappedBy = oneToOne.mappedBy();

            if (!mappedBy.isEmpty()) {
                targetField = JPAUtil.getField(target.getEntity(), mappedBy);
                forward = false;
            } else {
                //try to find the matching @OneToOne in target
                Set<Field> possibleTargetFields = JPAUtil.getJPAFields(target.getEntity(), OneToOne.class);
                for (Field f : possibleTargetFields) {
                    if (n.getEntity().equals(getRelevantType(f, null)) &&
                        f.getAnnotation(OneToOne.class).mappedBy().equals(field.getName())) {
                        
                        targetField = f;
                        break;
                    }
                }
            }

            if (forward) {
                n.addChild(target, targetField, field, DependencyType.ONE_TO_ONE);
            } else {
                n.addParent(target, targetField, field, DependencyType.ONE_TO_ONE);
            }
        } catch (Exception e) {
            LOG.error("Failed to analyze a @OneToOne relationship '" + field.getName() + "' on " + n.getEntity(), e);
        }
    }

    private void analyzeManyToOne(Node n, Field field, Annotations annotations, ManyToOne manyToOne) {
        Class<?> targetEntity = manyToOne.targetEntity();
        Node target = getTargetNode(field, targetEntity);
        Field targetField = null;

        Set<Field> possibleTargetFields = JPAUtil.getJPAFields(target.getEntity(), OneToMany.class);
        for (Field f : possibleTargetFields) {
            if (f.getAnnotation(OneToMany.class).mappedBy().equals(field.getName())
                && n.getEntity().equals(getRelevantType(f, null))) {
                targetField = f;
                break;
            }
        }

        n.addParent(target, targetField, field, DependencyType.ONE_TO_MANY);
    }

    private void analyzeOneToMany(Node n, Field field, Annotations annotations, OneToMany oneToMany) {
        Class<?> targetEntity = oneToMany.targetEntity();
        Node target = getTargetNode(field, targetEntity);
        Field targetField = null;

        if (!oneToMany.mappedBy().isEmpty()) {
            targetField = JPAUtil.getField(target.getEntity(), oneToMany.mappedBy());
        }

        n.addChild(target, targetField, field, DependencyType.ONE_TO_MANY);
    }

    private void analyzeManyToMany(Node n, Field field, Annotations annotations, ManyToMany manyToMany) {
        Class<?> targetEntity = manyToMany.targetEntity();
        Node target = getTargetNode(field, targetEntity);
        Field targetField = null;
        boolean forward = true;

        Set<Field> possibleTargetFields = JPAUtil.getJPAFields(target.getEntity(), ManyToMany.class);
        String thisMappedBy = manyToMany.mappedBy();
        for (Field f : possibleTargetFields) {
            if (thisMappedBy.equals(f.getName())) {
                targetField = f;
                forward = false;
                break;
            } else if (f.getAnnotation(ManyToMany.class).mappedBy().equals(field.getName())) {
                targetField = f;
                break;
            }
        }

        if (forward) {
            n.addChild(target, targetField, field, DependencyType.MANY_TO_MANY);
        } else {
            n.addParent(target, targetField, field, DependencyType.MANY_TO_MANY);
        }
    }

    private Class<?> getRelevantType(Field field, Class<?> declaredTargetEntity) {
        if (declaredTargetEntity == null || declaredTargetEntity == void.class) {
            declaredTargetEntity = field.getType();
        }

        if (isCollection(declaredTargetEntity)) {
            Type type = field.getGenericType();

            declaredTargetEntity = getCollectionTypeParameter(type, 0);
        } else if (isMap(declaredTargetEntity)) {
            Type type = field.getGenericType();

            declaredTargetEntity = getCollectionTypeParameter(type, 1);
        }

        return declaredTargetEntity;
    }

    private Node getTargetNode(Field field, Class<?> declaredTargetEntity) {
        declaredTargetEntity = getRelevantType(field, declaredTargetEntity);

        return analyze(new Node(declaredTargetEntity));
    }

    private static boolean isCollection(Class<?> clazz) {
        //this is actually copied from Hibernate impl. It seems to be rather strict about
        //the JPA collection classes.
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
        //g.addEntity(Class.forName("org.rhq.core.domain.configuration.Configuration"));
        g.addEntity(Class.forName("org.rhq.core.domain.resource.Resource"));
        System.out.println(g);
    }
}
