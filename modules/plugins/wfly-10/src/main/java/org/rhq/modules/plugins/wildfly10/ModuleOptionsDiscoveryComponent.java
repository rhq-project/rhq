/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.modules.plugins.wildfly10;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.wildfly10.ModuleOptionsComponent.Value;
import org.rhq.modules.plugins.wildfly10.json.Address;
import org.rhq.modules.plugins.wildfly10.json.ReadAttribute;
import org.rhq.modules.plugins.wildfly10.json.ReadChildrenNames;
import org.rhq.modules.plugins.wildfly10.json.ReadResource;
import org.rhq.modules.plugins.wildfly10.json.Result;

/**
 * Discovery class for Module Options nodes. 
 * 
 * @author Simeon Pinder
 */
public class ModuleOptionsDiscoveryComponent implements ResourceDiscoveryComponent<BaseComponent<?>> {
    private static final Log LOG = LogFactory.getLog(ModuleOptionsDiscoveryComponent.class);

    private static final Pattern COMMA_PATTERN = Pattern.compile(",");

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<BaseComponent<?>> context)
        throws Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        BaseComponent parentComponent = context.getParentResourceComponent();
        ASConnection connection = parentComponent.getASConnection();

        Configuration config = context.getDefaultPluginConfiguration();
        String confPath = config.getSimpleValue("path", "");
        if (confPath == null || confPath.isEmpty()) {
            LOG.error("Path plugin config is null for ResourceType [" + context.getResourceType().getName() + "].");
            return details;
        }

        //locate parent component identifier via path
        Configuration configParent = parentComponent.pluginConfiguration;
        String parentConfPath = configParent.getSimpleValue("path", "");

        //create relevant path and address details.
        String path = confPath;//Ex. subsystem=security
        //processing to retrieve parent for profile/domain mode.
        if ((parentConfPath != null) && (!parentConfPath.isEmpty())) {
            path = parentConfPath + "," + confPath;//Ex. profile=standalone-ha,subsystem=security
        }

        String name;//name=security
        Address address = new Address(path);

        //process the specific nodes
        //Then we need to find out which of subchildren of ModOpsComponent is used i)security-domain=*
        //ii)[Authentication*,etc] or iii)[ModOptions]

        //path should already be right
        if (path.endsWith("security-domain")) {//individual security domain entries
            //ex. path => /subsystem=security/security-domain=(entry name)
            //find all children and iterate over and update name appropriately
            Address typeAddress = new Address(path);
            String childType = "security-domain";
            Result result = connection.execute(new ReadChildrenNames(typeAddress, childType));

            if (result.isSuccess()) {

                @SuppressWarnings("unchecked")
                List<String> children = (List<String>) result.getResult();
                for (String child : children) {
                    //update the components for discovery
                    name = child;//ex. basic, databaseDomain
                    String currentChildPath = path + //ex. /subsystem=security,security-domain=jboss-web
                        "=" + child;
                    address = new Address(currentChildPath);
                    addDiscoveredResource(context, details, connection, currentChildPath, name, address);
                }
            }
        } else if (ifResourceIsSupportedModuleType(path)) {//is ModOptions map child
            //ex. path => /subsystem=security/security-domain=(entry name)/authentication=classic/login-modules
            //Ex. String attribute = "login-modules";
            String attribute = lookupAttributeType(path);
            //query all the module-options defined and discover them here
            //Ex. String typeAddress = "subsystem=security,security-domain=testDomain2,authentication=classic";
            String typeAddress = parentConfPath;
            ReadAttribute readModuleOptionType = new ReadAttribute(new Address(typeAddress), attribute);
            Result result = connection.execute(readModuleOptionType);
            if (result.isSuccess()) {
                List<Value> loadedLoginModuleTypes = ModuleOptionsComponent.populateSecurityDomainModuleOptions(result,
                    ModuleOptionsComponent.loadModuleOptionType(attribute));
                for (int moduleIndex = 0; moduleIndex < loadedLoginModuleTypes.size(); moduleIndex++) {
                    //Ex. name = "Login Module " + moduleIndex;
                    name = ModuleOptionsComponent.ModuleOptionType.readableNameMap.get(attribute) + " " + moduleIndex;
                    //Ex. subsystem=security,security-domain=testDomain2,authentication=classic,login-modules:0
                    String currentPath = path + ":" + moduleIndex;
                    //add the discovered resource
                    addDiscoveredResource(context, details, connection, currentPath, name, address);
                }
            }
        } else if (path.endsWith("module-options")) {//is ModOptions map child
            //ex. path => /subsystem=security/security-domain=(entry name)/acl=classic/login-modules*module-options
            //update name appropriately
            name = "Module Options";
            //add the discovered resource
            addDiscoveredResource(context, details, connection, path, name, address);
        } else {//[Authentication*,etc] children aka all others.
                //ex. path => /subsystem=security/security-domain=(entry name)/authentication=classic
                //update name appropriately
            name = context.getResourceType().getName();//Authentication (Classic).  Singletons.
            //add the discovered resource
            addDiscoveredResource(context, details, connection, path, name, address);
        }
        return details;
    }

    private String lookupAttributeType(String path) {
        String attribute = null;
        if ((path != null) && (!path.trim().isEmpty())) {
            //Ex. subsystem=security,security-domain=testDomain2,authentication=classic,login-modules
            String[] segments = COMMA_PATTERN.split(path);
            //contents of last segment should be the 'attribute' value of one of the ModuleOptionTypes
            if (segments.length > 1) {
                String last = segments[segments.length - 1];
                if (ModuleOptionsComponent.ModuleOptionType.typeMap.keySet().contains(last)) {
                    attribute = last;
                }
            }
        }
        return attribute;
    }

    private boolean ifResourceIsSupportedModuleType(String path) {
        boolean resourceIsSupported = false;
        if ((path != null) && (!path.trim().isEmpty())) {
            //Ex. subsystem=security,security-domain=testDomain2,authentication=classic,login-modules
            String[] segments = COMMA_PATTERN.split(path);
            //contents of last segment should be the 'attribute' value of one of the ModuleOptionTypes
            if (segments.length > 1) {
                String last = segments[segments.length - 1];
                if (ModuleOptionsComponent.ModuleOptionType.typeMap.keySet().contains(last)) {
                    resourceIsSupported = true;
                }
            }
        }
        return resourceIsSupported;
    }

    /** Adds discovered resource.
     * 
     * @param context
     * @param details
     * @param connection
     * @param path
     * @param name
     * @param address
     */
    private void addDiscoveredResource(ResourceDiscoveryContext context, Set<DiscoveredResourceDetails> details,
        ASConnection connection, String path, String name, Address address) {
        //ping the resource to determine if it's enabled and available.
        ReadResource op = new ReadResource(address);
        Result result = connection.execute(op);
        if (result.isSuccess()) {

            //include the config entry for the discovered node.
            Configuration config2 = context.getDefaultPluginConfiguration();
            //add path component to config as well.
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
}
