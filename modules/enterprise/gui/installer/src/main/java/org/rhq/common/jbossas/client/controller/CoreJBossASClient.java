/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.common.jbossas.client.controller;

import java.io.File;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 * Provides information to some core services.
 * 
 * @author John Mazzitelli
 */
public class CoreJBossASClient extends JBossASClient {

    public static final String CORE_SERVICE = "core-service";
    public static final String SERVER_ENVIRONMENT = "server-environment";
    public static final String PLATFORM_MBEAN = "platform-mbean";
    public static final String DEPLOYMENT_SCANNER = "deployment-scanner";
    public static final String SCANNER = "scanner";

    public CoreJBossASClient(ModelControllerClient client) {
        super(client);
    }

    public String getOperatingSystem() throws Exception {
        final String[] address = { CORE_SERVICE, PLATFORM_MBEAN, "type", "operating-system" };
        final String osName = getStringAttribute("name", Address.root().add(address));
        return osName;
    }

    public String getAppServerVersion() throws Exception {
        final String version = getStringAttribute("release-version", Address.root());
        return version;
    }

    public String getAppServerHomeDir() throws Exception {
        final String[] address = { CORE_SERVICE, SERVER_ENVIRONMENT };
        final String dir = getStringAttribute(true, "home-dir", Address.root().add(address));
        return dir;
    }

    public String getAppServerBaseDir() throws Exception {
        final String[] address = { CORE_SERVICE, SERVER_ENVIRONMENT };
        final String dir = getStringAttribute(true, "base-dir", Address.root().add(address));
        return dir;
    }

    public String getAppServerConfigDir() throws Exception {
        final String[] address = { CORE_SERVICE, SERVER_ENVIRONMENT };
        final String dir = getStringAttribute(true, "config-dir", Address.root().add(address));
        return dir;
    }

    public String getAppServerLogDir() throws Exception {
        final String[] address = { CORE_SERVICE, SERVER_ENVIRONMENT };
        final String dir = getStringAttribute(true, "log-dir", Address.root().add(address));
        return dir;
    }

    public String getAppServerTmpDir() throws Exception {
        final String[] address = { CORE_SERVICE, SERVER_ENVIRONMENT };
        final String dir = getStringAttribute(true, "temp-dir", Address.root().add(address));
        return dir;
    }

    /**
     * Returns the location where the default deployment scanner is pointing to.
     * This is where EARs, WARs and the like are deployed to.
     * @return the default deployments directory - null if there is no deployment scanner
     * @throws Exception
     */
    public String getAppServerDefaultDeploymentDir() throws Exception {
        final String[] addressArr = { SUBSYSTEM, DEPLOYMENT_SCANNER, SCANNER, "default" };
        final Address address = Address.root().add(addressArr);
        final ModelNode resourceAttributes = readResource(address);

        if (resourceAttributes == null) {
            return null; // there is no default scanner
        }

        final String path = resourceAttributes.get("path").asString();
        String relativeTo = null;
        if (resourceAttributes.hasDefined("relative-to")) {
            relativeTo = resourceAttributes.get("relative-to").asString();
        }

        // path = the actual filesystem path to be scanned. Treated as an absolute path,
        //        unless 'relative-to' is specified, in which case value is treated as relative to that path.
        // relative-to = Reference to a filesystem path defined in the "paths" section of the server
        //               configuration, or one of the system properties specified on startup.
        //               NOTE: here we will assume that if specified, it is a system property name.

        if (relativeTo != null) {
            String syspropValue = System.getProperty(relativeTo);
            if (syspropValue == null) {
                throw new IllegalStateException("Cannot support relative-to that isn't a sysprop: " + relativeTo);
            }
            relativeTo = syspropValue;
        }

        File dir = new File(relativeTo, path);
        return dir.getAbsolutePath();
    }

    /**
     * Set a runtime system property in the JVM that is managed by JBossAS.
     *
     * @param name
     * @param value
     * @throws Exception
     */
    public void setSystemProperty(String name, String value) throws Exception {
        ModelNode request = createRequest(ADD, Address.root().add(SYSTEM_PROPERTY, name));
        request.get(VALUE).set(value);
        ModelNode response = execute(request);
        if (!isSuccess(response)) {
            throw new FailureException(response, "Failed to set system property [" + name + "]");
        }
    }
}
