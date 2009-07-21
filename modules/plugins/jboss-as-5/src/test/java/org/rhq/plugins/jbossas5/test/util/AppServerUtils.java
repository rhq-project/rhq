/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.plugins.jbossas5.test.util;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.deployers.spi.management.deploy.DeploymentProgress;
import org.jboss.deployers.spi.management.deploy.DeploymentStatus;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.plugins.jbossas5.ProfileServiceComponent;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;

/**
 * Various application server related utility methods used by the tests.
 * 
 * @author Lukas Krejci
 */
public class AppServerUtils {

    public static final String PLUGIN_NAME = "JBossAS5";
    public static final String APPLICATION_SERVER_RESOURCE_NAME = "JBossAS Server";

    public static final int DEPLOY_CONTENT_TIMEOUT = 30000; //30s

    private AppServerUtils() {
    }

    /**
     * Deploys an archive to the AS and starts it.
     * 
     * @param name the name of the archive
     * @param archiveFile the archive file
     * @param exploded whether to deploy exploded or not
     * @throws Exception
     */
    public static void deployFileToAS(String name, File archiveFile, boolean exploded) throws Exception {
        DeploymentManager deploymentManager = getDeploymentManager();

        DeploymentProgress progress = deploymentManager.distribute(name, archiveFile.toURI().toURL(), exploded);
        progress.run();

        DeploymentStatus status = progress.getDeploymentStatus();

        if (status.isFailed()) {
            throw new IllegalStateException("Failed to distribute " + archiveFile.getAbsolutePath() + " with message: "
                + status.getMessage());
        }

        String[] deploymentNames = progress.getDeploymentID().getRepositoryNames();

        progress = deploymentManager.start(deploymentNames);
        progress.run();

        status = progress.getDeploymentStatus();

        if (status.isFailed()) {
            throw new IllegalStateException("Failed to start " + archiveFile.getAbsolutePath() + " with message: "
                + status.getMessage());
        }

    }

    /**
     * Undeploys an archive of given name from the AS
     * 
     * @param archiveName
     * @throws Exception
     */
    public static void undeployFromAS(String archiveName) throws Exception {
        DeploymentManager deploymentManager = getDeploymentManager();

        DeploymentProgress progress = deploymentManager.remove(archiveName);
        progress.run();

        DeploymentStatus status = progress.getDeploymentStatus();

        if (status.isFailed()) {
            throw new IllegalStateException("Failed to undeploy " + archiveName + " with message: "
                + status.getMessage());
        }
    }

    /**
     * @return The application server resource from the plugin container's inventory.
     * 
     * @throws IllegalStateException if there is none or more than 1 app servers 
     * in the plugin container's inventory.
     */
    public static Resource getASResource() {
        PluginMetadataManager pluginMetadataManager = PluginContainer.getInstance().getPluginManager()
            .getMetadataManager();
        ResourceType appServerResourceType = pluginMetadataManager.getType(APPLICATION_SERVER_RESOURCE_NAME,
            PLUGIN_NAME);

        InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();

        Set<Resource> appServers = inventoryManager.getResourcesWithType(appServerResourceType);

        if (appServers.size() != 1) {
            throw new IllegalStateException("Expected to find exactly 1 AS5 server, but found " + appServers.size()
                + " instead.");
        }

        return appServers.iterator().next();
    }

    /**
     * Returns the application server component proxy with given interface.
     * <p>
     * The proxy is created using the AS component classloader and therefore cannot be directly
     * cast to the facetInterface, which is most probably loaded in a different classloader.
     * <p>
     * The return type is therefore an object and you have to use reflection to work with it.
     * 
     * @param <T> 
     * @param facetInterface the proxy interface class to hide the application server component behind
     * @return the proxy
     * @throws Exception
     */
    public static <T> Object getASComponentProxy(Class<T> facetInterface) throws Exception {
        ClassLoader currenContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Resource asResource = getASResource();
            ClassLoader cl = PluginContainer.getInstance().getPluginComponentFactory().getResourceClassloader(
                asResource);
            Class<?> resourceSpecificFacetInterface = Class.forName(facetInterface.getName(), true, cl);

            //use the resource specific classloader for the proxy creation
            Thread.currentThread().setContextClassLoader(cl);
            
            return ComponentUtil.getComponent(asResource.getId(), resourceSpecificFacetInterface, FacetLockType.WRITE,
                DEPLOY_CONTENT_TIMEOUT, true, true);
        } finally {
            Thread.currentThread().setContextClassLoader(currenContextClassLoader);
        }
    }

    public static <T> T getRemoteObject(String jndiName, Class<T> clazz) throws Exception {
        InitialContext initialContext = getAppServerInitialContext();
        return clazz.cast(initialContext.lookup(jndiName));
    }

    public static InitialContext getAppServerInitialContext() throws NamingException {
        ResourceContainer resourceContainer = PluginContainer.getInstance().getInventoryManager().getResourceContainer(
            getASResource());

        Configuration asConfiguration = resourceContainer.getResourceContext().getPluginConfiguration();

        Properties env = new Properties();
        env.setProperty(Context.PROVIDER_URL, asConfiguration.getSimpleValue("namingURL", null));

        env.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
        env.setProperty("jnp.timeout", "60000");
        env.setProperty("jnp.sotimeout", "60000");
        env.setProperty("jnp.disableDiscovery", "true");

        return new InitialContext(env);
    }

    /**
     * A helper method to invoke a method on an object using reflection.
     * 
     * @param methodName the name of the method to invoke
     * @param instance the instance to invoke the method upon
     * @param methodArgTypesAndValues the method argument types and values
     * @return the result of the method call
     * @throws Exception on error
     */
    public static Object invokeMethod(String methodName, Object instance, MethodArgDef... methodArgTypesAndValues)
        throws Exception {
        Class<?>[] argTypes = null;
        Object[] argValues = null;

        if (methodArgTypesAndValues != null) {
            argTypes = new Class<?>[methodArgTypesAndValues.length];
            argValues = new Object[methodArgTypesAndValues.length];

            for (int i = 0; i < methodArgTypesAndValues.length; ++i) {
                argTypes[i] = methodArgTypesAndValues[i].getType();
                argValues[i] = methodArgTypesAndValues[i].getValue();
            }
        }

        Method method = instance.getClass().getMethod(methodName, argTypes);

        return method.invoke(instance, argValues);
    }

    private static DeploymentManager getDeploymentManager() throws Exception {

        Object asComponent = getASComponentProxy(ProfileServiceComponent.class);

        Object connection = invokeMethod("getConnection", asComponent, (MethodArgDef[])null);
        //I wonder why this cast works... where does the DeploymentManager get loaded from in plugin and in here? 
        return (DeploymentManager) invokeMethod("getDeploymentManager", connection, (MethodArgDef[])null);
    }
}
