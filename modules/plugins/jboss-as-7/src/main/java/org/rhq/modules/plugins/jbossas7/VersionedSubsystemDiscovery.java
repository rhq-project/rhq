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

package org.rhq.modules.plugins.jbossas7;

import static org.rhq.modules.plugins.jbossas7.ASConnection.verbose;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
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
 * This subclass adds logic for discovering different versions of the same logical resource,
 * by stripping version info out of the path, removing it from the resourceName and setting it
 * as the resourceVersion.
 *
 * @author Heiko W. Rupp
 * @author Jay Shaughnessy
 */
public class VersionedSubsystemDiscovery extends SubsystemDiscovery {

    /* The matched format is name-VERSION.ext.  Version must minimally be in major.minor format.  Simpler versions,
     * like a single digit, are too possibly part of the actual name.  myapp-1.war and myapp-2.war could easily be
     * different apps (albeit poorly named).  But myapp-1.0.war and myapp-2.0.war are pretty clearly versions of
     * the same app.  The goal was to handle maven-style versioning.
     */
    static private final String PATTERN_DEFAULT = "^(.*?)-([0-9]+\\.[0-9].*)(\\..*)$";
    static private final String PATTERN_PROP = "rhq.as7.VersionedSubsystemDiscovery.pattern";
    static private final Matcher MATCHER;

    static {
        Matcher m = null;
        try {
            String override = System.getProperty(PATTERN_PROP);
            if (null != override) {
                Pattern p = Pattern.compile(override);
                m = p.matcher("");
                if (m.groupCount() != 3) {
                    String msg = "Pattern supplied by system property [" + PATTERN_PROP
                        + "] is invalid. Expected [3] matching groups but found [" + m.groupCount()
                        + "]. Will use default pattern [" + PATTERN_DEFAULT + "].";
                    m = null;
                    LogFactory.getLog(VersionedSubsystemDiscovery.class).error(msg);
                }
            }
        } catch (Exception e) {
            String msg = "Pattern supplied by system property [" + PATTERN_PROP
                + "] is invalid. Will use default pattern [" + PATTERN_DEFAULT + "].";
            m = null;
            LogFactory.getLog(VersionedSubsystemDiscovery.class).error(msg, e);
        }

        MATCHER = (null != m) ? m : Pattern.compile(PATTERN_DEFAULT).matcher("");
    }

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

        // If the subsystem has a built-in version string, parse it out such that
        // we can discover new versions of the same logical resource without creating
        // a new resource, but rather just updating the version.

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

                        MATCHER.reset(val);
                        String version = null;
                        String name = val;
                        if (MATCHER.matches()) {
                            name = MATCHER.group(1) + MATCHER.group(3);
                            version = MATCHER.group(2);
                        }

                        Configuration config2 = context.getDefaultPluginConfiguration();
                        String resKey;
                        PropertySimple pathProp;

                        if (path == null || path.isEmpty()) {
                            resKey = cpath + "=" + name;
                            pathProp = new PropertySimple("path", cpath + "=" + val);

                        } else {
                            if (path.startsWith(","))
                                path = path.substring(1);
                            resKey = path + "," + cpath + "=" + name;
                            pathProp = new PropertySimple("path", path + "," + cpath + "=" + val);
                        }

                        config2.put(pathProp);

                        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context.getResourceType(), // DataType
                            resKey, // Key
                            name, // Name
                            version, // Version
                            context.getResourceType().getDescription(), // subsystem.description
                            config2, null);
                        details.add(detail);
                    }
                }
            }
        } else {
            // Single subsystem
            MATCHER.reset(path);
            String version = null;
            String resKey = path;
            if (MATCHER.matches()) {
                resKey = MATCHER.group(1) + MATCHER.group(3);
                version = MATCHER.group(2);
            }

            path += "," + confPath;
            resKey += "," + confPath;
            if (path.startsWith(",")) {
                path = path.substring(1);
                resKey = resKey.substring(1);
            }
            Result result = connection.execute(new ReadResource(new Address(path)));
            if (result.isSuccess()) {

                String name = resKey.substring(resKey.lastIndexOf("=") + 1);
                Configuration config2 = context.getDefaultPluginConfiguration();
                PropertySimple pathProp = new PropertySimple("path", path);
                config2.put(pathProp);

                DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context.getResourceType(), // DataType
                    resKey, // Key
                    name, // Name
                    version, // Version
                    context.getResourceType().getDescription(), // Description
                    config2, null);
                details.add(detail);
            }
        }

        return details;
    }
}
