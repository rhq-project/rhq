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
import java.util.List;
import java.util.Properties;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

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
    public static final String EXTENSION = "extension";
    public static final String MODULE = "module";
    public static final String CORE_SERVICE_MGMT = "management";
    public static final String MGMT_INTERFACE = "management-interface";
    public static final String MGMT_INTERFACE_HTTP = "http-interface";

    public CoreJBossASClient(ModelControllerClient client) {
        super(client);
    }

    /**
     * Allows the caller to turn on or off complete access for the app server's admin console.
     *
     * @param enableFlag true if the admin console enabled and visible; false if you want to prohibit all access to the admin console
     * @throws Exception 
     */
    public void setEnableAdminConsole(boolean enableFlag) throws Exception {
        // /core-service=management/management-interface=http-interface/:write-attribute(name=console-enabled,value=false)
        final Address address = Address.root()
            .add(CORE_SERVICE, CORE_SERVICE_MGMT, MGMT_INTERFACE, MGMT_INTERFACE_HTTP);
        final ModelNode req = createWriteAttributeRequest("console-enabled", Boolean.toString(enableFlag), address);
        final ModelNode response = execute(req);
        if (!isSuccess(response)) {
            throw new FailureException(response);
        }
        return;
    }

    /**
     * Given a string with possible ${x} expressions in it, this will resolve that expression
     * using system property values that are set within the AS JVM itself. If the string
     * to resolve has no expressions, or has no expressions that are resolveable, the expression
     * string itself is returned as-is (this includes if <code>expression</code> is <code>null</code>).
     *
     * @param expression string containing zero, one or more ${x} expressions to be resolved
     * @return the expression with the expressions resolved using system properties of the AS JVM
     * @throws Exception if failed to resolve the expression.
     */
    public String resolveExpression(String expression) throws Exception {
        if (expression == null || expression.length() == 0) {
            return expression;
        }
        final ModelNode request = createRequest("resolve-expression", Address.root());
        request.get("expression").set(expression);
        final ModelNode response = execute(request);
        if (!isSuccess(response)) {
            throw new FailureException(response);
        }
        return getResults(response).asString();
    }

    /**
     * This returns the system properties that are set in the AS JVM. This is not the system properties
     * in the JVM of this client object - it is actually the system properties in the remote
     * JVM of the AS instance that the client is talking to.
     *
     * @return the AS JVM's system properties
     * @throws Exception
     */
    public Properties getSystemProperties() throws Exception {
        final String[] address = { CORE_SERVICE, PLATFORM_MBEAN, "type", "runtime" };
        final ModelNode op = createReadAttributeRequest(true, "system-properties", Address.root().add(address));
        final ModelNode results = execute(op);
        if (isSuccess(results)) {
            // extract the DMR representation into a java Properties object
            final Properties sysprops = new Properties();
            final ModelNode node = getResults(results);
            final List<Property> propertyList = node.asPropertyList();
            for (Property property : propertyList) {
                final String name = property.getName();
                final ModelNode value = property.getValue();
                if (name != null) {
                    sysprops.put(name, value != null ? value.asString() : "");
                }
            }
            return sysprops;
        } else {
            throw new FailureException(results, "Failed to get system properties");
        }
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

    public String getServerProductVersion() throws Exception {
        final String version = getStringAttribute("product-version", Address.root());
        return version;
    }

    public String getServerProductName() throws Exception {
        final String version = getStringAttribute("product-name", Address.root());
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

    public String getAppServerDataDir() throws Exception {
        final String[] address = { CORE_SERVICE, SERVER_ENVIRONMENT };
        final String dir = getStringAttribute(true, "data-dir", Address.root().add(address));
        return dir;
    }

    /**
     * Enabled or disables the default deployment scanner.
     * @param enabled the new status to be set
     * @throws Exception
     */
    public void setAppServerDefaultDeploymentScanEnabled(boolean enabled) throws Exception {
        final String[] addressArr = { SUBSYSTEM, DEPLOYMENT_SCANNER, SCANNER, "default" };
        final Address address = Address.root().add(addressArr);
        final ModelNode req = createWriteAttributeRequest("scan-enabled", Boolean.toString(enabled), address);
        final ModelNode response = execute(req);

        if (!isSuccess(response)) {
            throw new FailureException(response);
        }
        return;
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
     * Adds a new module extension to the core system.
     *
     * @param name the name of the new module extension
     * @throws Exception
     */
    public void addExtension(String name) throws Exception {
        // /extension=<name>/:add(module=<name>)
        final ModelNode request = createRequest(ADD, Address.root().add(EXTENSION, name));
        request.get(MODULE).set(name);
        final ModelNode response = execute(request);
        if (!isSuccess(response)) {
            throw new FailureException(response, "Failed to add new module extension [" + name + "]");
        }
        return;
    }

    /**
     * Returns true if the given extension is already in existence.
     *
     * @param name the name of the extension to check
     * @return true if the extension already exists; false if not
     * @throws Exception
     */
    public boolean isExtension(String name) throws Exception {
        return null != readResource(Address.root().add(EXTENSION, name));
    }

    /**
     * Adds a new subsystem to the core system.
     *
     * @param name the name of the new subsystem
     * @throws Exception
     */
    public void addSubsystem(String name) throws Exception {
        // /subsystem=<name>:add()
        final ModelNode request = createRequest(ADD, Address.root().add(SUBSYSTEM, name));
        final ModelNode response = execute(request);
        if (!isSuccess(response)) {
            throw new FailureException(response, "Failed to add new subsystem [" + name + "]");
        }
        return;
    }

    /**
     * Returns true if the given subsystem is already in existence.
     *
     * @param name the name of the subsystem to check
     * @return true if the subsystem already exists; false if not
     * @throws Exception
     */
    public boolean isSubsystem(String name) throws Exception {
        return null != readResource(Address.root().add(SUBSYSTEM, name));
    }

    /**
     * Invokes the management "reload" operation which will shut down all the app server services and
     * restart them again. This is required for certain configuration changes to take effect.
     * This does not shutdown the JVM itself.
     *
     * NOTE: once this method returns, the client is probably unusable since the server side
     * will probably shutdown the connection. You will need to throw away this object and rebuild
     * another one with a newly reconnected {@link #getModelControllerClient() client}.
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
     * NOTE: once this method returns, the client is probably unusable since the server side
     * will probably shutdown the connection. You will need to throw away this object and rebuild
     * another one with a newly reconnected {@link #getModelControllerClient() client}.
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
     * will be killed and if the client is co-located with the server JVM, this client will
     * also die.
     *
     * NOTE: even if this method returns, the client is unusable since the server side
     * will shutdown the connection. You will need to throw away this object and rebuild
     * another one with a newly reconnected {@link #getModelControllerClient() client}.
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
     * will be killed and if the client is co-located with the server JVM, this client will
     * also die.
     *
     * NOTE: even if this method returns, the client is unusable since the server side
     * will shutdown the connection. You will need to throw away this object and rebuild
     * another one with a newly reconnected {@link #getModelControllerClient() client}.
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
