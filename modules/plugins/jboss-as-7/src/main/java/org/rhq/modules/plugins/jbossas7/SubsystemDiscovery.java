/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7;

import static org.rhq.modules.plugins.jbossas7.ASConnection.verbose;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ReadChildrenNames;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Discover subsystems. We need to distinguish two cases denoted by the path
 * plugin config:
 * <ul>
 *     <li>Path is a single 'word': here the value denotes a key in the resource path
 *     of AS7, that identifies a child type see e.g. the Connectors below the JBossWeb
 *     service in the plugin descriptor. There can be multiple resources of the given
 *     type. In addition it is possible that a path entry in configuration shares multiple
 *     types that are separated by the pipe symbol.</li>
 *     <li>Path is a key-value pair (e.g. subsystem=web ). This denotes a singleton
 *     subsystem with a fixes path within AS7 (perhaps below another resource in the
 *     tree.</li>
 * </ul>
 *
 * @author Heiko W. Rupp
 */
public class SubsystemDiscovery implements ResourceDiscoveryComponent<BaseComponent<?>> {

    private final Log log = LogFactory.getLog(this.getClass());

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<BaseComponent<?>> context)
        throws Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationConfig.Feature.READ_ENUMS_USING_TO_STRING, true);

        BaseComponent parentComponent = context.getParentResourceComponent();
        ASConnection connection = parentComponent.getASConnection();

        Configuration config = context.getDefaultPluginConfiguration();
        String confPath = config.getSimpleValue("path", "");
        if (confPath == null || confPath.isEmpty()) {
            log.error("Path plugin config is null for ResourceType [" + context.getResourceType().getName() + "].");
            return details;
        }

        boolean lookForChildren = false;

        if (!confPath.contains("=")) { // NO = -> no sub path, but a type
            lookForChildren = true;
        }

        // check if the parent is a JDG/Infinispan server. In this case ignore the as7 version
        // of the type and vice versa
        if (shouldSkipEntryWrtIspn(context, confPath)) {
            return details;
        }

        // Construct the full path including the parent
        String path;
        String parentPath = parentComponent.getPath();
        if (parentPath == null || parentPath.isEmpty()) {
            parentPath = "";
        }
        path = parentPath;

        PropertySimple managedRuntimeProp = config.getSimple("managedRuntime");
        if (managedRuntimeProp != null && managedRuntimeProp.getBooleanValue() != null
            && managedRuntimeProp.getBooleanValue()) {

            // path correction for managed servers, where the config is below host=x,server-config=y but
            // the runtime resource is below host=x,server=y
            if (path.startsWith("host=")) {
                path = path.replaceAll(",server-config=", ",server=");
                parentPath = parentPath.replaceAll(",server-config=", ",server=");
            }
        }

        if (verbose) {
            log.info("total path: [" + path + "]");
        }

        if (lookForChildren) {
            // Looking for multiple resource of type 'childType'

            // check if there are multiple types are present
            List<String> subTypes = new ArrayList<String>();
            if (confPath.contains("|")) {
                subTypes.addAll(Arrays.asList(confPath.split("\\|")));
            } else
                subTypes.add(confPath);

            for (String cpath : subTypes) {

                Address addr = new Address(parentPath);
                Result result = connection.execute(new ReadChildrenNames(addr, cpath));

                if (result.isSuccess()) {

                    @SuppressWarnings("unchecked")
                    List<String> subsystems = (List<String>) result.getResult();

                    // There may be multiple children of the given type
                    for (String val : subsystems) {

                        String newPath = cpath + "=" + val;
                        Configuration config2 = context.getDefaultPluginConfiguration();

                        String resKey;

                        if (path == null || path.isEmpty())
                            resKey = newPath;
                        else {
                            if (path.startsWith(","))
                                path = path.substring(1);
                            resKey = path + "," + cpath + "=" + val;
                        }

                        PropertySimple pathProp = new PropertySimple("path", resKey);
                        config2.put(pathProp);

                        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context.getResourceType(), // DataType
                            resKey, // Key
                            val, // Name
                            null, // Version
                            context.getResourceType().getDescription(), // subsystem.description
                            config2, null);
                        details.add(detail);
                    }
                }
            }
        } else {
            // Single subsystem
            path += "," + confPath;
            if (path.startsWith(","))
                path = path.substring(1);
            Result result = connection.execute(new ReadResource(new Address(path)));
            if (result.isSuccess()) {

                String resKey = path;
                String name = resKey.substring(resKey.lastIndexOf("=") + 1);
                Configuration config2 = context.getDefaultPluginConfiguration();
                PropertySimple pathProp = new PropertySimple("path", path);
                config2.put(pathProp);

                DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context.getResourceType(), // DataType
                    path, // Key
                    name, // Name
                    null, // Version
                    context.getResourceType().getDescription(), // Description
                    config2, null);
                details.add(detail);
            }
        }

        return details;
    }

    /**
     * The as7 plugin and the JDG/Infinispan Server plugin both have a subsystem=infinispan. We need to decide
     * which one to 'activate' depending on the type, plugin and the detected parent.
     * Rules are:<ul>
     *     <li>If the parent is a host controller or such, there is no jdg available</li>
     *     <li>If parent is eap/as7, use the type from the as7 plugin</li>
     *     <li>If parent is a jdg server, use the type from the jdg plugin.</li>
     * </ul>
     *
     *
     * @param context The parent's resource component
     * @param confPath The subsystem that got fed into discovery. Directly return is not subsystem=infinispan
     * @return True if this subsystem should be skipped.
     */
    private boolean shouldSkipEntryWrtIspn(ResourceDiscoveryContext<BaseComponent<?>> context, String confPath) {

        // If this is not subsystem=infinispan, we should not skip it at all
        if (!"subsystem=infinispan".equals(confPath))
            return false;

        ResourceType ourType = context.getResourceType();
        boolean ourPluginTypeIsJdg = ourType.getPlugin().equals("JDG")
            || (ourType.getPlugin().equals("InfinispanServer"));

        String productType = context.getParentResourceComponent().pluginConfiguration.getSimpleValue("productType",
            "AS7");
        boolean isJdgProduct = "JDG".equals(productType) || "ISPN".equals(productType);

        if (ourPluginTypeIsJdg && isJdgProduct) {
            log.debug("Ours is JDG and product is JDG/InfinispanServer");
            return false;
        }

        if (!ourPluginTypeIsJdg && !isJdgProduct) {
            if (log.isDebugEnabled()) {
                log.debug("Ours is not JDG (" + ourType.toString() + ") and product is not JDG/InfinispanServer ("
                    + productType + ")");
            }
            return false;
        }

        return true;
    }

}
