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
package org.rhq.plugins.jbossas5.factory;

import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.profileservice.spi.ProfileKey;
import org.jboss.profileservice.spi.ProfileService;

/**
 * Factory class to get the ProfileService and Profile Service related objects directly from the
 * ProfileService object.
 *
 * @author Mark Spritzler
 */
public class ProfileServiceFactory
{
    private static final Log LOG = LogFactory.getLog(ProfileServiceFactory.class);

    private static final String PROFILE_SERVICE_JNDI_NAME = "ProfileService";

    private static ProfileService profileService;

    private static ManagementView currentProfileView;
    
    private static final ProfileKey DEFAULT_PROFILE_KEY = new ProfileKey(ProfileKey.DEFAULT);

    /**
     * Returns the profile service from the JBoss server through JNDI
     *
     * @return ProfileService
     */
    public static ProfileService getProfileService()
    {
        if (profileService == null)
        {
            InitialContext initialContext;
            try
            {
                initialContext = new InitialContext();
            }
            catch (NamingException e)
            {
                LOG.error("Unable to get an InitialContext to JBossAS 5.", e);
                return null;
            }

            try
            {
                profileService = (ProfileService) initialContext.lookup(PROFILE_SERVICE_JNDI_NAME);
            }
            catch (Exception e)
            {
                LOG.error("Exception thrown when looking up ProfileService on JBoss AS 5", e);
            }
        }
        return profileService;
    }

    /**
     * Get the current profile's Management view. This will get the domains from the profile service
     * and return the first one in the list.
     *
     * @return ManagementView the management view of the first domain
     */
    public static ManagementView getCurrentProfileView()
    {
        if (currentProfileView == null)
        {
            currentProfileView = getProfileService().getViewManager();
            refreshCurrentProfileView();
        }
        return currentProfileView;
    }

    /**
     * Refresh the current profile's ManagementView so it contains all the latest data.
     * Use {@link #getCurrentProfileView()} to obtain the ManagementView.
     */
    public static void refreshCurrentProfileView()
    {
        try
        {
        	loadProfile(getCurrentProfileView());
        }
        catch (Exception e)
        {
            LOG.error("Could not load default profile from current management view.", e);
        }
    }

    public static DeploymentManager getDeploymentManager() throws Exception {
        DeploymentManager deploymentManager = getProfileService().getDeploymentManager();
        // Load and associate the given profile with the DeploymentManager for future operations. This is mandatory
        // in order for us to be able to successfully invoke the various DeploymentManager methods.
        loadProfile(deploymentManager);
        return deploymentManager;
    }

    private static void loadProfile(ManagementView managementView)
    {
    	try
    	{
    		LOG.debug("About to load profile via Management View...");
    		long startTime = System.currentTimeMillis();
    		managementView.load();
    	    long elapsedTime = System.currentTimeMillis() - startTime;
    		LOG.debug("Loaded profile via Management View in " + elapsedTime + " milliseconds.");
    	}
    	catch (Exception e)
    	{
    		LOG.error("Could not load profile via Management View.", e);
    	}    	
    }    
    
    private static void loadProfile(DeploymentManager deploymentManager)
    {
    	try
    	{
    		LOG.debug("Loading profile via Deployment Manager...");
    		long startTime = System.currentTimeMillis();
    		deploymentManager.loadProfile(DEFAULT_PROFILE_KEY);
    	    long elapsedTime = System.currentTimeMillis() - startTime;
    		LOG.debug("Loaded profile via Deployment Manager in " + elapsedTime + " milliseconds.");
    	}
    	catch (Exception e)
    	{
    		LOG.error("Could not find profile via Deployment Manager.", e);
    	}    	
    }    
    
    /**
     * Locate the given ComponentType with the given component name.
     *
     * @param type ComponentType of the component to get
     * @param name String name of the component
     * @return the matching ManagedComponent if found, null otherwise
     * @throws Exception on error
     */
    public static ManagedComponent getManagedComponent(ComponentType type, String name)
            throws Exception
    {
        ManagementView mgtView = getCurrentProfileView();
        return getManagedComponent(mgtView, type, name);
    }

    /**
     * Locate the given ComponentType with the given component name.
     *
     * @param managementView
     * @param type
     * @param name
     * @return the matching ManagedComponent if found, null otherwise
     * @throws Exception on error
     */
    public static ManagedComponent getManagedComponent(ManagementView managementView,
                                                       ComponentType type, String name)
            throws Exception
    {
        Set<ManagedComponent> managedComponents = managementView.getComponentsForType(type);
        if (managedComponents != null) {
            for (ManagedComponent managedComponent : managedComponents) {
                if (managedComponent.getName().equals(name))
                    return managedComponent;
            }
        }
        return null;
    }

    /**
     * 
     * @param name
     * @param componentType
     * @return
     */
    public static boolean isManagedComponent(String name, ComponentType componentType)
    {
        boolean isDeployed = false;
        if (name != null)
        {
            try
            {
                ManagedComponent component = getManagedComponent(componentType, name);
                if (component != null)
                    isDeployed = true;
            }
            catch (Exception e)
            {
                // Setting it to true to be safe than sorry, since there might be a component
                // already deployed in the AS. TODO (ips): I don't think I like this.
                isDeployed = true;
            }
        }
        return isDeployed;
    }
}
