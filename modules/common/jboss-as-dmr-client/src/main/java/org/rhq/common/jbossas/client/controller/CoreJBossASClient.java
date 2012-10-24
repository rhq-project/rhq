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

        final File dir = new File(relativeTo, path);
        return dir.getAbsolutePath();
    }

    /**
     * Sets the interval of the default deployment scanner. If the value is
     * less than 1 millisecond, the scanner will scan only one time at server startup.
     * @param millis number of milliseconds to periodically scan the deployment directory
     * @throws Exception
     */
    public void setAppServerDefaultDeploymentScanInterval(long millis) throws Exception {
        final String[] addressArr = { SUBSYSTEM, DEPLOYMENT_SCANNER, SCANNER, "default" };
        final Address address = Address.root().add(addressArr);
        final ModelNode req = createWriteAttributeRequest("scan-interval", Long.toString(millis), address);
        final ModelNode response = execute(req);

        if (!isSuccess(response)) {
            throw new FailureException(response);
        }
        return;
    }

    /**
     * Sets the deployment timeout of the default deployment scanner. If a deployment
     * takes longer than this value, it will fail.
     * @param secs number of seconds the app server will wait for a deployment to finish
     * @throws Exception
     */
    public void setAppServerDefaultDeploymentTimeout(long secs) throws Exception {
        final String[] addressArr = { SUBSYSTEM, DEPLOYMENT_SCANNER, SCANNER, "default" };
        final Address address = Address.root().add(addressArr);
        final ModelNode req = createWriteAttributeRequest("deployment-timeout", Long.toString(secs), address);
        final ModelNode response = execute(req);

        if (!isSuccess(response)) {
            throw new FailureException(response);
        }
        return;
    }

    /**
     * Set a runtime system property in the JVM that is managed by JBossAS.
     *
     * @param name
     * @param value
     * @throws Exception
     */
    public void setSystemProperty(String name, String value) throws Exception {
        final ModelNode request = createRequest(ADD, Address.root().add(SYSTEM_PROPERTY, name));
        request.get(VALUE).set(value);
        final ModelNode response = execute(request);
        if (!isSuccess(response)) {
            throw new FailureException(response, "Failed to set system property [" + name + "]");
        }
    }

    /**
     * Invokes the management "reload" operation which will shut down all the app server services and
     * restart them again. This is required for certain configuration changes to take effect.
     * This does not shutdown the JVM itself.
     *
     * @throws Exception
     */
    public void reload() throws Exception {
        reload(false);
    }

    /**
     * Invokes the management "reload" operation which will shut down all the app server services and
     * restart them again potentially in admin-only mode.
     * This does not shutdown the JVM itself.
     *
     * @param adminOnly if <code>true</code>, reloads the server in admin-only mode
     *
     * @throws Exception
     */
    public void reload(boolean adminOnly) throws Exception {
        final ModelNode request = createRequest("reload", Address.root());
        request.get("admin-only").set(adminOnly);
        final ModelNode response = execute(request);
        if (!isSuccess(response)) {
            throw new FailureException(response);
        }
        return;
    }

    /**
     * Invokes the management "shutdown" operation with the restart option set to true
     * (see {@link #shutdown(boolean)}).
     * This means the app server JVM will shutdown but immediately be restarted.
     * Note that the caller may not be returned to since the JVM in which this call is made
     * will be killed.
     *
     * @throws Exception
     */
    public void restart() throws Exception {
        shutdown(true);
    }

    /**
     * Invokes the management "shutdown" operation that kills the JVM completely with System.exit.
     * If restart is set to true, the JVM will immediately be restarted again. If restart is false,
     * the JVM is killed and will stay down.
     * Note that in either case, the caller may not be returned to since the JVM in which this call is made
     * will be killed.
     *
     * @param restart if true, the JVM will be restarted
     * @throws Exception
     */
    public void shutdown(boolean restart) throws Exception {
        final ModelNode request = createRequest("shutdown", Address.root());
        request.get("restart").set(restart);
        final ModelNode response = execute(request);
        if (!isSuccess(response)) {
            throw new FailureException(response);
        }
        return;
    }
}
