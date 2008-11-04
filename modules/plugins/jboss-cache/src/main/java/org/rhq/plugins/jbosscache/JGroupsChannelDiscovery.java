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
package org.rhq.plugins.jbosscache;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.mc4j.ems.connection.EmsConnection;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jbossas.util.DeploymentUtility;

/**
 * This component will discover JGroups channels within a JBoss Cache instance.
 * The deal here is that we want to have zero or one JGroups channels per cache.
 * We will look at the Cache config and see if it contains a 
 * &lt;attribute name="ClusterConfig"&gt; element. If so, we return a JGroups instance,
 * else we return nothing.
 *
 * @author Heiko W. Rupp
 */
public class JGroupsChannelDiscovery implements ResourceDiscoveryComponent {

    private final Log log = LogFactory.getLog(JGroupsChannelDiscovery.class);

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent#discoverResources(org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext)
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context)
        throws InvalidPluginConfigurationException, Exception {

        JBossCacheComponent parent = (JBossCacheComponent) context.getParentResourceComponent();
        EmsConnection emsConnection = parent.getEmsConnection();
        String resKey = context.getParentResourceContext().getResourceKey();
        File file = DeploymentUtility.getDescriptorFile(emsConnection, resKey);

        boolean found = false;
        try {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(file);

            // Get the root element
            Element root = doc.getRootElement();

            //            XPath xpath = XPathFactory.newInstance().newXPath();
            // TODO this expression would need to work against non-normalized versions of the name attribute
            //            XPathExpression xp = xpath
            //                .compile("/server/mbean[@name='" + resKey + "']/attribute[@name='ClusterConfig']");
            //            InputSource inputSource = new InputSource(new FileInputStream(file));
            //            NodeList cconfig = (NodeList) xp.evaluate(inputSource, XPathConstants.NODESET);
            //            if (cconfig != null && cconfig.getLength() > 0)
            //                found = true;

            // First look for the right mbean of *our* cache - the file may contain more than one

            // TODO move code in helper, as we'll need it later again
            // TODO replace the access of 'our' ClusterConfig attribute with an XPath expression
            for (Object mbeanObj : root.getChildren("mbean")) {
                if (mbeanObj instanceof Element) {
                    Element mbean = (Element) mbeanObj;
                    String nameAttrib = mbean.getAttributeValue("name");
                    try {
                        ObjectName on = new ObjectName(nameAttrib);
                        nameAttrib = on.getCanonicalName();
                    } catch (MalformedObjectNameException e) {
                        log.warn("Can't canonicalize " + nameAttrib);
                    }
                    if (nameAttrib.equals(resKey)) {
                        // our cache instance, look for the right attribute
                        List children = mbean.getChildren("attribute");
                        for (Object childObj : children) {
                            if (childObj instanceof Element) {
                                Element child = (Element) childObj;
                                String name = child.getAttributeValue("name");
                                if (name.equals("ClusterConfig"))
                                    found = true;
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("IO error occurred while reading file: " + file, e);
        } catch (JDOMException e) {
            log.error("Parsing error occurred while reading file: " + file, e);
        }

        if (found) {
            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context.getResourceType(), // Resource Type
                resKey + "jgroupsChannel", // ResourceKey TODO good choice ?
                "JGroups channel", // resource name
                null, // Version
                "JGroups config for parent JBossCache", // description
                context.getDefaultPluginConfiguration(), // config
                null); // process info
            Set<DiscoveredResourceDetails> res = new HashSet<DiscoveredResourceDetails>(1);
            res.add(detail);
            return res;
        }

        return null;
    }

}
