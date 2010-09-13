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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.rhq.helpers.perftest.support.jpa.Edge;
import org.rhq.helpers.perftest.support.jpa.Node;

/**
 *
 * @author Lukas Krejci
 */
@XmlRootElement(name = "graph")
@XmlAccessorType(XmlAccessType.FIELD)
public class ExportConfiguration {

    @XmlElement(name = "entity")
    private Set<Entity> entity = new HashSet<Entity>();

    @XmlAttribute
    private String packagePrefix;

    @XmlAttribute
    private Boolean includeExplicitDependentsImplicitly;

    @XmlTransient
    private Properties settings;

    public Set<Entity> getEntities() {
        return entity;
    }

    public void setEntities(Set<Entity> nodes) {
        this.entity = nodes;
    }

    public Class<?> getClassForEntity(Entity n) {
        String className = packagePrefix == null ? n.getName() : packagePrefix + "." + n.getName();

        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            //maybe it's an absolute name after all
            try {
                return Class.forName(n.getName());
            } catch (ClassNotFoundException e1) {
                //hmm... run out of options
                return null;
            }
        }
    }

    public Entity getEntity(Class<?> clazz) {
        for (Entity e : entity) {
            if (getClassForEntity(e).equals(clazz)) {
                return e;
            }
        }

        return null;
    }

    public String getPackagePrefix() {
        return packagePrefix;
    }

    public void setPackagePrefix(String packagePrefix) {
        this.packagePrefix = packagePrefix;
    }

    /**
     * @return the includeExplicitDependentsImplicitly
     */
    public boolean isIncludeExplicitDependentsImplicitly() {
        return includeExplicitDependentsImplicitly == null ? true : includeExplicitDependentsImplicitly;
    }

    /**
     * @param includeExplicitDependentsImplicitly the includeExplicitDependentsImplicitly to set
     */
    public void setIncludeExplicitDependentsImplicitly(boolean includeExplicitDependentsImplicitly) {
        this.includeExplicitDependentsImplicitly = includeExplicitDependentsImplicitly;
    }

    /**
     * @return the settings
     */
    public Properties getSettings() {
        return settings;
    }

    /**
     * @param settings the settings to set
     */
    public void setSettings(Properties settings) {
        this.settings = settings;
    }

    public static JAXBContext getJAXBContext() throws JAXBException {
        return JAXBContext.newInstance(ExportConfiguration.class, Entity.class, Relationship.class);
    }

    public static ExportConfiguration fromRealDependencyGraph(String packagePrefix,
        org.rhq.helpers.perftest.support.jpa.EntityDependencyGraph g) {
        ExportConfiguration ret = new ExportConfiguration();
        ret.setPackagePrefix(packagePrefix);

        Map<Node, Entity> realToSerialized = new HashMap<Node, Entity>();

        for (Node node : g.getAllNodes()) {
            Entity serialized = fromRealNode(packagePrefix, node);
            realToSerialized.put(node, serialized);
            ret.getEntities().add(serialized);
        }

        //now go through the edges.. this has to be done only after we have all the nodes
        for (Node node : g.getAllNodes()) {
            Set<Relationship> edges = new HashSet<Relationship>();

            for (Edge edge : node.getEdges()) {
                if (edge.getFrom() == node) {
                    Relationship r = fromRealEdge(node, edge, realToSerialized);
                    if (r != null) {
                        edges.add(r);
                    }
                }
            }

            Entity serialized = realToSerialized.get(node);
            serialized.setRelationships(edges);
        }

        return ret;
    }

    private static Entity fromRealNode(String packageNamePrefix, Node node) {
        String className = node.getEntity().getName();
        if (className.startsWith(packageNamePrefix)) {
            className = className.substring(packageNamePrefix.length() + 1);
        }

        Entity ret = new Entity();
        ret.setName(className);

        return ret;
    }

    private static Relationship fromRealEdge(Node currentNode, Edge edge, Map<Node, Entity> realToSerializedNodes) {
        Relationship ret = new Relationship();
        if (currentNode.equals(edge.getFrom())) {
            if (edge.getFromField() != null) {
                ret.setField(edge.getFromField().getName());
            } else {
                return null;
            }
        } else {
            if (edge.getToField() != null) {
                ret.setField(edge.getToField().getName());
            } else {
                return null;
            }
        }
        return ret;
    }

    public static void main(String[] args) throws Exception {
        JAXBContext c = JAXBContext.newInstance(ExportConfiguration.class, Entity.class, Relationship.class);
        Marshaller m = c.createMarshaller();

        org.rhq.helpers.perftest.support.jpa.EntityDependencyGraph edg = new org.rhq.helpers.perftest.support.jpa.EntityDependencyGraph();
        edg.addEntity(Class.forName("org.rhq.core.domain.resource.Resource"));

        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
        m.marshal(ExportConfiguration.fromRealDependencyGraph("org.rhq.core.domain", edg), System.out);
    }
}
