/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.ReadChildrenNames;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Discover subsystems
 *
 * @author Heiko W. Rupp
 */
@SuppressWarnings("unused")
public class SubsystemDiscovery implements ResourceDiscoveryComponent<BaseComponent> {

    private final Log log = LogFactory.getLog(this.getClass());

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<BaseComponent> context)
            throws Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>(1);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES,false);
        mapper.configure(DeserializationConfig.Feature.READ_ENUMS_USING_TO_STRING,true);

        BaseComponent parentComponent = context.getParentResourceComponent();
        ASConnection connection = parentComponent.getASConnection();


        Configuration config = context.getDefaultPluginConfiguration();
        String cpath = config.getSimpleValue("path", null);
        boolean recursive = false;

        String parentPath = parentComponent.getPath();

        String path;
        if (cpath!=null && cpath.endsWith("/*")) {
            path = cpath.substring(0,cpath.length()-2);
            recursive = true;
        }
        else
            path = cpath;

        if (parentPath!=null && !parentPath.isEmpty()) {
            if (parentPath.endsWith("/") || path.startsWith("/"))
                path = parentPath + path;
            else
                path = parentPath + "/" + path;
        }
        System.out.println("total path: [" + path + "]");


        JsonNode json ;
        if (!recursive)
            json = connection.getLevelData(path,recursive, false);
        else {
            List<PROPERTY_VALUE> addr ;
            addr = parentComponent.pathToAddress(parentPath);
            String childType = cpath.substring(0, cpath.length() - 2);
            if (childType.startsWith("/"))
                childType = childType.substring(1);
            json = connection.execute(new ReadChildrenNames(addr, childType));
        }
        if (!connection.isErrorReply(json)) {
            if (recursive) {
                int i = path.lastIndexOf("/");
                String subPath = path.substring(i+1);

                JsonNode subNode = json.findPath("result");
                if (subNode==null || subNode.isNull())
                    subNode = json.get(subPath);  // TODO clean this up. to get the 'key' in a path from the AS we need to use get()

//                Map<String,Subsystem> subsystemMap = mapper.readValue(subNode,new TypeReference<Map<String,Subsystem>>() {});
                if (subNode!=null && subNode.isArray()) {

                   Iterator<JsonNode> iter = subNode.getElements();
//                if (subsystemMap==null) {
//                    log.warn("SubsystemMap was null for path [" + path + "] and subPath ["+ subPath + "] and subNode [" + subNode + "]");
//                    return Collections.emptySet();
//                }
                    while (iter.hasNext()) {

                        JsonNode node = iter.next();
                    String val = node.getTextValue();


                    String newPath = cpath.replaceAll("\\*",val);
                    Configuration config2 = context.getDefaultPluginConfiguration();
                    PropertySimple pathProp = new PropertySimple("path",newPath);
                    config2.put(pathProp);

                    String resKey = context.getParentResourceContext().getResourceKey() + "/" + val;
                    String name = resKey.substring(resKey.lastIndexOf("/") + 1);


                    DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                            context.getResourceType(), // DataType
                            path + "/" + val, // Key
                            name, // Name
                            null, // Version
                            "TODO", // subsystem.description, // Description
                            config2,
                            null);
                    details.add(detail);
                }
                }
                else {

                    System.out.println("subnode was no array");
                    if (subNode==null)
                        log.error("subNode was null for " + path + " and type " + context.getResourceType().getName());
                }

            }
            else {


                String resKey = path;
                String name = resKey.substring(resKey.lastIndexOf("/") + 1);
                Configuration config2 = context.getDefaultPluginConfiguration();
                PropertySimple pathProp = new PropertySimple("path",path);
                config2.put(pathProp);



                DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                        context.getResourceType(), // DataType
                        path, // Key
                        name, // Name
                        null, // Version
                        path, // Description
                        config2,
                        null);
                details.add(detail);
            }

            return details;
        }

        return Collections.emptySet();
    }

}
