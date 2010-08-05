package org.rhq.core.pc.inventory;

/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.inventory.ChildResourceTypeDiscoveryFacet;
import org.rhq.core.util.exception.ThrowableUtil;

public class ChildResourceTypeDiscoveryRunner implements Callable<Set<ResourceType>>, Runnable {

    private Log log = LogFactory.getLog(ChildResourceTypeDiscoveryRunner.class);

    //Default Ctor
    public ChildResourceTypeDiscoveryRunner() {

    }

    public void run() {
        try {
            call();
        } catch (Exception e) {
            log.error("Could not get measurement report.", e);
        }
    }

    public Set<ResourceType> call() {

        if (log.isDebugEnabled()) {
            log.info("<ChildResourceTypeDiscoveryRunner>call() called");
        }

        //Set<ResourceTypes> for the ResourceTypes which shall be added later
        Set<ResourceType> resourceTypes = null;

        long start = System.currentTimeMillis();

        //get InventoryManager instance
        InventoryManager im = PluginContainer.getInstance().getInventoryManager();

        if (log.isDebugEnabled()) {
            log.info("InventoryManager instance created");
        }

        //Get current plattform
        Resource platform = im.getPlatform();
        if (log.isDebugEnabled()) {
            log.info("Platform returned with name: " + platform.getName());
        }

        // Next discover all other services and non-top-level servers
        Set<Resource> children = platform.getChildResources();
        if (log.isDebugEnabled()) {
            log.info("Platform " + platform.getName() + " has " + children.size() + " ChildResources");
        }

        if (children != null) {
            for (Resource child : children) {
                if (log.isDebugEnabled()) {
                    log.debug("Name of server: " + child.getName());
                    log.debug("Id of server: " + child.getId());
                    log.debug("Category of server: " + child.getResourceType().getCategory().toString());
                }

                //Check if really is of Category SERVER because our Plugin has to be of that category
                if (child.getResourceType().getCategory() == ResourceCategory.SERVER) {

                    if (log.isDebugEnabled()) {
                        log.info("Server " + child.getName() + "has passed the Server Category test succesfull");
                    }

                    ResourceContainer container = im.getResourceContainer(child.getId());

                    if (log.isDebugEnabled()) {
                        log.info("Server " + child.getName() + " is running in ResourceContainer "
                            + container.toString());
                    }

                    if (container.getResourceComponentState() != ResourceContainer.ResourceComponentState.STARTED
                        || container.getAvailability() == null
                        || container.getAvailability().getAvailabilityType() == AvailabilityType.DOWN) {
                        // Don't collect metrics for resources that are down
                        if (log.isDebugEnabled()) {
                            log.info("ChildType not discoverd for inactive resource component: "
                                + container.getResource());
                        }
                    } else {

                        try {

                            //Get Facet Component
                            ChildResourceTypeDiscoveryFacet discoveryComponent = ComponentUtil.getComponent(child
                                .getId(), ChildResourceTypeDiscoveryFacet.class, FacetLockType.READ, 30 * 1000, true,
                                true);

                            //get Set<ResourceType> --> all the ChildResourceTypes which shall be added dynamically
                            resourceTypes = discoverChildResourceTypes(discoveryComponent);

                            if (log.isDebugEnabled()) {
                                log.info("Container.getResource(): " + container.getResource().getName());
                                log.info("Container.getResource().getResourceType(): "
                                    + container.getResource().getResourceType().getName());

                            }

                            //all the ChildResourceTypes which are already part of the plugin
                            Set<ResourceType> currentChildTypes = container.getResource().getResourceType()
                                .getChildResourceTypes();

                            Set<ResourceType> newTypesToAdd = new HashSet<ResourceType>();

                            if (log.isDebugEnabled()) {
                                log.info("Size of HashSet<ResourceType> after initialisation: " + newTypesToAdd.size());
                            }

                            //Check all types with were added by the plugin
                            for (ResourceType newTypetoAdd : resourceTypes) {
                                //Check all ChildResourceTypes that already exist 
                                for (ResourceType alreadyExistingType : currentChildTypes) {
                                    //Check if name and plugin of the types are equal
                                    //Necessary because equal-named ChildResourceTypes can belong to different plugins
                                    if (newTypetoAdd.getName().equals(alreadyExistingType.getName())
                                        && newTypetoAdd.getPlugin().equals(alreadyExistingType.getPlugin())) {
                                        log.info("The ResourceType " + newTypetoAdd.getName()
                                            + " already exists for the Plugin " + newTypetoAdd.getPlugin());
                                    } else {
                                        log.info("The ResourceType " + newTypetoAdd.getName()
                                            + " does not exist for the Plugin " + newTypetoAdd.getPlugin() + " yet");

                                        //if ChildResourceType did not exist add the new ChildResourceType 
                                        //to the set which will be given to the InventoryManager to persist
                                        if (log.isDebugEnabled()) {
                                            log.info("new ChildResourceType " + newTypetoAdd.getName()
                                                + " added to Set<ResourceTypes>");
                                        }
                                        newTypesToAdd.add(newTypetoAdd);

                                    }

                                }
                            }

                            //Create a new ResourceType in the DB for the selected type
                            //call InventoryManager method only if Set<ResourceType> contains at least one element
                            if (newTypesToAdd.size() > 0) {
                                im.createNewResourceType(newTypesToAdd);
                            }

                        } catch (PluginContainerException pce) {
                            // This is expected when the ResourceComponent does not implement the ChildResourceTypeDiscoveryFacet
                            log.warn("Error submitting service scan: " + pce.getMessage());
                        } catch (Exception e) {
                            throw new RuntimeException("Error submitting service scan", e);
                        }
                    }
                }
            }

        } else {
            log.info("Set<ResourceType> was returned with value null");
        }

        return null;
    }

    /**
     *
     * @param discoveryComponent
     * @return
     */
    private Set<ResourceType> discoverChildResourceTypes(ChildResourceTypeDiscoveryFacet discoveryComponent) {

        Set<ResourceType> resourceTypes = null;
        try {
            long start = System.currentTimeMillis();
            resourceTypes = discoveryComponent.discoverChildResourceTypes();
            long duration = (System.currentTimeMillis() - start);

            if (duration > 2000) {
                log.info("[PERF] Discovery of childResourceTypes for [" + discoveryComponent + "] took [" + duration
                    + "ms]");
            }
        } catch (Throwable t) {

            log.warn("Failure to discover childResourceType data - cause: " + ThrowableUtil.getAllMessages(t));
        }

        return resourceTypes;
    }
}
