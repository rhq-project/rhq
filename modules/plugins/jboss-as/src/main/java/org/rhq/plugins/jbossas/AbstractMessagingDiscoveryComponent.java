 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */

package org.rhq.plugins.jbossas;

 import java.util.Arrays;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Set;

 import org.mc4j.ems.connection.EmsConnection;
 import org.mc4j.ems.connection.bean.EmsBean;
 import org.mc4j.ems.connection.bean.attribute.EmsAttribute;

 import org.rhq.core.domain.configuration.Configuration;
 import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
 import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
 import org.rhq.plugins.jmx.JMXComponent;
 import org.rhq.plugins.jmx.MBeanResourceDiscoveryComponent;
 import org.rhq.plugins.jmx.ObjectNameQueryUtility;

 /**
 * Abstract base class to discover JBossMessaging and JBossMQ related stuff
 * @author Heiko W. Rupp
 */
public abstract class AbstractMessagingDiscoveryComponent extends MBeanResourceDiscoveryComponent {

    /**
     * Do the real discovery
     * @param context ResourceContext
     * @param objectName Object name of the object that determines if the service is available
     * @param resourceName Name of the resource
     * @param resourceDescription Description of the resource
     * @param versionSource A string that determines how to obtain the version. See {@link #getVersionFromSource(org.mc4j.ems.connection.EmsConnection, String)}
     * @return The details of a discovered resource.
     */
    protected Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JMXComponent> context,
                                                               String objectName, String resourceName,
                                                               String resourceDescription, String versionSource) {

        Set<DiscoveredResourceDetails> result = new HashSet<DiscoveredResourceDetails>();

        ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(objectName);
        JMXComponent parentResourceComponent = context.getParentResourceComponent();
        EmsConnection connection = parentResourceComponent.getEmsConnection();

        Configuration pluginConfig = context.getDefaultPluginConfiguration();

        List<EmsBean> beans = connection.queryBeans(queryUtility.getTranslatedQuery());
        if (beans.size() == 1) {
            String version = getVersionFromSource(connection,versionSource);
            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context.getResourceType(), objectName,
                resourceName, version, resourceDescription, pluginConfig, null);
            result.add(detail);
        }
        return result;

    }

    /**
     * Parse the passed source information and construct the version string from it
     * @param connection EmsConnection to use for this.
     * @param versionSource The source string. This is of the form "domain:mbean;att1,att2,..."
     * @return A string naming the resource
     */
    private String getVersionFromSource(EmsConnection connection, String versionSource) {

        int pos ;
        pos = versionSource.indexOf(';');
        if (pos<0)
            return "unknown";
        String beanName = versionSource.substring(0,pos);
        String attribs = versionSource.substring(pos+1);
        String[] attrs = attribs.split(",");

        ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(beanName);
        List<EmsBean> beans = connection.queryBeans(queryUtility.getTranslatedQuery());
        if (beans.size() == 1) {
            EmsBean bean = beans.get(0);
            bean.refreshAttributes(Arrays.asList(attrs));
            StringBuilder ret = new StringBuilder();
            Iterator<String> iter = Arrays.asList(attrs).iterator();
            while (iter.hasNext()) {
                String att = iter.next()   ;
                EmsAttribute eatt = bean.getAttribute(att);
                ret.append((String)eatt.getValue());
                if (iter.hasNext())
                    ret.append(" ");
            }
            return ret.toString();
        }
        else
            return "unknown";

    }
}