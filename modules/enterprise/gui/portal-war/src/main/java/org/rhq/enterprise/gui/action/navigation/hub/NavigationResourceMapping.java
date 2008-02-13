/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.action.navigation.hub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A class which currently holds a mapping between Navigation nodes, e.g. resource.navigation.jboss.Servers, to actual
 * resource type names. This static configuration should be migrated so it is read in from a configuration file. This
 * map is required since there currently doesn't exist a way to associate like services together, e.g. Jboss 3.2 and
 * JBoss 4.0 servers
 *
 * @author     <a href="ccrouch@jboss.com">Charles Crouch</a>
 * @deprecated ips, 01/22/07
 */
@Deprecated
public class NavigationResourceMapping {
    private static final Map navNodesToResources = new HashMap();

    // the map keys are the navigation keys taken from ApplicationResources.properties
    static {
        // JBoss related servers and services
        ArrayList jbossServerTypeNames = new ArrayList();
        jbossServerTypeNames.add("JBoss 3.2");
        jbossServerTypeNames.add("JBoss 4.0");
        navNodesToResources.put("resource.navigation.jboss.Servers", jbossServerTypeNames);

        ArrayList datasourceTypeNames = new ArrayList();
        datasourceTypeNames.add("JBoss 4.0 JCA Data Source");
        navNodesToResources.put("resource.navigation.jboss.DataSources", datasourceTypeNames);

        ArrayList connPoolTypeNames = new ArrayList();
        connPoolTypeNames.add("JBoss 4.0 JCA Connection Pool");
        connPoolTypeNames.add("JBoss 3.2 JCA Connection Pool");
        navNodesToResources.put("resource.navigation.jboss.ConnPools", connPoolTypeNames);

        ArrayList hibFactoryEJBTypeNames = new ArrayList();
        hibFactoryEJBTypeNames.add("JBoss 4.0 Hibernate Session Factory");
        hibFactoryEJBTypeNames.add("JBoss 3.2 Hibernate Session Factory");
        navNodesToResources.put("resource.navigation.jboss.HibernateFactories", hibFactoryEJBTypeNames);

        ArrayList statelessEJBTypeNames = new ArrayList();
        statelessEJBTypeNames.add("JBoss 3.2 Stateless Session EJB");
        statelessEJBTypeNames.add("JBoss 4.0 Stateless Session EJB");
        navNodesToResources.put("resource.navigation.jboss.StatelessEJBs", statelessEJBTypeNames);

        ArrayList statefulEJBTypeNames = new ArrayList();
        statefulEJBTypeNames.add("JBoss 4.0 Stateful Session EJB");
        statefulEJBTypeNames.add("JBoss 3.2 Stateful Session EJB");
        navNodesToResources.put("resource.navigation.jboss.StatefulEJBs", statefulEJBTypeNames);

        ArrayList messageEJBTypeNames = new ArrayList();
        messageEJBTypeNames.add("JBoss 3.2 Message Driven EJB");
        messageEJBTypeNames.add("JBoss 4.0 Message Driven EJB");
        navNodesToResources.put("resource.navigation.jboss.MessageEJBs", messageEJBTypeNames);

        ArrayList entityEJBTypeNames = new ArrayList();
        entityEJBTypeNames.add("JBoss 4.0 Entity EJB");
        entityEJBTypeNames.add("JBoss 3.2 Entity EJB");
        navNodesToResources.put("resource.navigation.jboss.EntityEJBs", entityEJBTypeNames);

        ArrayList EJB3TypeNames = new ArrayList();
        EJB3TypeNames.add("JBoss 4.0 EJB3");
        navNodesToResources.put("resource.navigation.jboss.EJB3", EJB3TypeNames);

        ArrayList JMSTopicTypeNames = new ArrayList();
        JMSTopicTypeNames.add("JBoss 4.0 JMS Topic");
        JMSTopicTypeNames.add("JBoss 3.2 JMS Topic");
        navNodesToResources.put("resource.navigation.jboss.JMSTopics", JMSTopicTypeNames);

        ArrayList JMSQueueTypeNames = new ArrayList();
        JMSQueueTypeNames.add("JBoss 4.0 JMS Queue");
        JMSQueueTypeNames.add("JBoss 3.2 JMS Queue");
        navNodesToResources.put("resource.navigation.jboss.JMSQueues", JMSQueueTypeNames);

        ArrayList jgroupsChannelTypeNames = new ArrayList();
        jgroupsChannelTypeNames.add("JBoss 4.0 JGroups Channel");
        jgroupsChannelTypeNames.add("JBoss 3.2 JGroups Channel");
        navNodesToResources.put("resource.navigation.jboss.JGroupsChannels", jgroupsChannelTypeNames);

        // Tomcat related servers and services
        ArrayList tomcatServerTypeNames = new ArrayList();
        tomcatServerTypeNames.add("Tomcat 5.0");
        tomcatServerTypeNames.add("Tomcat 4.0");
        tomcatServerTypeNames.add("Tomcat 4.1");
        tomcatServerTypeNames.add("Tomcat 5.5");
        navNodesToResources.put("resource.navigation.tomcat.Servers", tomcatServerTypeNames);

        ArrayList tomcatWebAppTypeNames = new ArrayList();
        tomcatWebAppTypeNames.add("Tomcat 4.0 Webapp");
        tomcatWebAppTypeNames.add("Tomcat 4.1 Webapp");
        tomcatWebAppTypeNames.add("Tomcat 5.0 Webapp");
        tomcatWebAppTypeNames.add("Tomcat 5.5 Webapp");
        navNodesToResources.put("resource.navigation.tomcat.WebApps", tomcatWebAppTypeNames);

        ArrayList tomcatConnectorTypeNames = new ArrayList();
        tomcatConnectorTypeNames.add("Tomcat 4.0 Connector"); // not presently supported
        tomcatConnectorTypeNames.add("Tomcat 4.1 Connector");
        tomcatConnectorTypeNames.add("Tomcat 5.0 Connector");
        tomcatConnectorTypeNames.add("Tomcat 5.5 Connector");
        navNodesToResources.put("resource.navigation.tomcat.Connectors", tomcatConnectorTypeNames);

        ArrayList tomcatServletTypeNames = new ArrayList();
        tomcatServletTypeNames.add("Tomcat 4.0 Servlet");
        tomcatServletTypeNames.add("Tomcat 4.1 Servlet");
        tomcatServletTypeNames.add("Tomcat 5.0 Servlet");
        tomcatServletTypeNames.add("Tomcat 5.5 Servlet");
        navNodesToResources.put("resource.navigation.tomcat.Servlets", tomcatServletTypeNames);

        // Apache related servers and services
        ArrayList apacheServerTypeNames = new ArrayList();
        apacheServerTypeNames.add("Apache-ERS 2.3");
        apacheServerTypeNames.add("Apache-ERS 2.4");
        apacheServerTypeNames.add("Apache 1.3");
        apacheServerTypeNames.add("Apache 2.0");
        navNodesToResources.put("resource.navigation.apache.Servers", apacheServerTypeNames);

        ArrayList apacheVHostTypeNames = new ArrayList();
        apacheVHostTypeNames.add("Apache-ERS 2.3 VHost");
        apacheVHostTypeNames.add("Apache-ERS 2.4 VHost");
        apacheVHostTypeNames.add("Apache 1.3 VHost");
        apacheVHostTypeNames.add("Apache 2.0 VHost");
        navNodesToResources.put("resource.navigation.apache.VirtualHosts", apacheVHostTypeNames);
    }

    public static Map getNavigationNodeToResourcesMapping() {
        return navNodesToResources;
    }
}