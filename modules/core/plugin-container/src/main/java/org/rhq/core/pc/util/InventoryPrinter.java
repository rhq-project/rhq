 /*
  * RHQ Management Platform
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
package org.rhq.core.pc.util;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ProcessScan;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.InventoryFile;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;

/**
 * @author John Mazzitelli
 * @author Ian Springer
 */
public class InventoryPrinter {
    public static void outputAllResourceTypes(PrintWriter exportWriter, boolean dumpXml, Set<ResourceType> rootTypes) {
        if (dumpXml) {
            exportWriter.printf("<?xml version=\"1.0\"?>\n");
            exportWriter.printf("<inventory-types>\n");
        }

        if (rootTypes != null) {
            for (ResourceType rootType : rootTypes) {
                outputResourceType(exportWriter, dumpXml, 0, rootType);
            }
        }

        if (dumpXml) {
            exportWriter.printf("</inventory-types>\n");
        }
    }

    private static void outputResourceType(PrintWriter exportWriter, boolean dumpXml, int descendantDepth,
        ResourceType resourceType) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < descendantDepth; i++) {
            indent.append("   ");
            if (dumpXml) {
                indent.append("   "); // add another three spaces since XML needs more indentation
            }
        }

        if (dumpXml) {
            exportWriter.printf("%s<resource-type>\n", indent);
            exportWriter.printf("%s   <id>%d</id>\n", indent, resourceType.getId());
            exportWriter.printf("%s   <name>%s</name>\n", indent, resourceType.getName());
            exportWriter.printf("%s   <category>%s</category>\n", indent, resourceType.getCategory());
            exportWriter.printf("%s   <plugin>%s</plugin>\n", indent, resourceType.getPlugin());
            exportWriter.printf("%s   <description>%s</description>\n", indent, resourceType.getDescription());
            exportWriter.printf("%s   <sub-category>%s</sub-category>\n", indent, resourceType.getSubCategory());

            exportWriter.printf("%s   <process-scans>\n", indent);
            if (resourceType.getProcessScans() != null) {
                for (ProcessScan processScan : resourceType.getProcessScans()) {
                    exportWriter.printf("%s      <process-scans>%s</process-scans>\n", indent, processScan);
                }
            }

            exportWriter.printf("%s   </process-scans>\n", indent);

            exportWriter.printf("%s   <metrics>\n", indent);
            if (resourceType.getMetricDefinitions() != null) {
                for (MeasurementDefinition def : resourceType.getMetricDefinitions()) {
                    exportWriter.printf("%s      <metric>%s</metric>\n", indent, def.getName());
                }
            }

            exportWriter.printf("%s   </metrics>\n", indent);

            exportWriter.printf("%s   <operations>\n", indent);
            if (resourceType.getOperationDefinitions() != null) {
                for (OperationDefinition def : resourceType.getOperationDefinitions()) {
                    exportWriter.printf("%s      <operation>%s</operation>\n", indent, def.getName());
                }
            }

            exportWriter.printf("%s   </operations>\n", indent);

            exportWriter.printf("%s   <children>\n", indent);
        } else {
            String name = resourceType.getName();
            String plugin = resourceType.getPlugin();
            List<String> parents = new ArrayList<String>();
            for (ResourceType parent : resourceType.getParentResourceTypes()) {
                parents.add(parent.getName() + '(' + parent.getPlugin() + ')');
            }

            exportWriter.printf("%s+ %s(%s), parents=%s\n", indent, name, plugin, parents);
        }

        for (ResourceType child : resourceType.getChildResourceTypes()) {
            outputResourceType(exportWriter, dumpXml, descendantDepth + 1, child);
        }

        if (dumpXml) {
            exportWriter.printf("%s   </children>\n", indent);
            exportWriter.printf("%s</resource-type>\n", indent);
        }

        return;
    }

    public static void outputInventory(PrintWriter exportWriter, boolean xml) {
        // we want to recurse children
        outputInventory(exportWriter, true, xml);
    }

    public static void outputInventory(PrintWriter exportWriter, boolean recurseChildren, boolean xml) {
        outputInventory(exportWriter, recurseChildren, xml, (ResourceContainer) null);
    }

    public static void outputInventory(PrintWriter exportWriter, boolean recurseChildren, boolean xml,
        ResourceContainer resourceContainer) {
        outputInventory(exportWriter, recurseChildren, xml, resourceContainer, 0, PluginContainer.getInstance()
            .getInventoryManager(), null, null);
        exportWriter.flush();
    }

    public static void outputInventory(PrintWriter exportWriter, boolean recurseChildren, boolean xml,
        InventoryFile file) {
        outputInventory(exportWriter, recurseChildren, xml, null, 0, PluginContainer.getInstance()
            .getInventoryManager(), file, null);
        exportWriter.flush();
    }

    private static void outputInventory(PrintWriter exportWriter, boolean recurseChildren, boolean dumpXml,
        ResourceContainer resourceContainer, int descendantDepth, InventoryManager inventoryManager,
        InventoryFile inventoryFile, InventoryPrinter.ResourceCounter resourceCounter) {
        StringBuffer indent = new StringBuffer();
        for (int i = 0; i < descendantDepth; i++) {
            indent.append("   ");
            if (dumpXml) {
                indent.append("   "); // add another three spaces since XML needs more indentation
            }
        }

        if (descendantDepth == 0) {
            if (resourceContainer == null) {
                if (inventoryFile == null) {
                    // get the in-memory inventory
                    resourceContainer = inventoryManager.getResourceContainer(inventoryManager.getPlatform());
                } else {
                    // get the inventory found in the inventory file
                    resourceContainer = getResourceContainer(inventoryFile.getPlatform().getId(), inventoryFile
                        .getResourceContainers());
                }
            }

            if (resourceCounter == null) {
                resourceCounter = new ResourceCounter();
            }

            if (dumpXml) {
                exportWriter.printf("<?xml version=\"1.0\"?>\n");
                exportWriter.printf("<inventory>\n");
            }
        }

        if (resourceContainer == null) {
            exportWriter.printf("!!!RESOURCE CONTAINER IS NULL!!!");
            return;
        }

        Resource resource = resourceContainer.getResource();
        if (resource == null) {
            exportWriter.printf("!!!RESOURCE IS NULL!!!");
            return;
        }
        resourceCounter.tallyResource(resource);

        Availability avail = resourceContainer.getAvailability();
        AvailabilityType availType = null;
        if (avail != null) {
            availType = avail.getAvailabilityType();
        }
        String availString = (availType == null) ? "UNKNOWN" : availType.toString();

        if (dumpXml) {
            exportWriter.printf("%s<resource>\n", indent);
            exportWriter.printf("%s   <id>%d</id>\n", indent, resource.getId());
            exportWriter.printf("%s   <key>%s</key>\n", indent, resource.getResourceKey());
            exportWriter.printf("%s   <name>%s</name>\n", indent, resource.getName());
            exportWriter.printf("%s   <version>%s</version>\n", indent, resource.getVersion());
            exportWriter.printf("%s   <uuid>%s</uuid>\n", indent, resource.getUuid());
            exportWriter.printf("%s   <description>%s</description>\n", indent, resource.getDescription());
            exportWriter
                .printf("%s   <inventory-status>%s</inventory-status>\n", indent, resource.getInventoryStatus());
            exportWriter.printf("%s   <type>%s</type>\n", indent, (resource.getResourceType() != null) ? resource
                .getResourceType().getName() : "null");
            exportWriter.printf("%s   <availabilityType>%s</availabilityType>\n", indent, availString);
            exportWriter.printf("%s   <category>%s</category>\n", indent,
                (resource.getResourceType() != null) ? resource.getResourceType().getCategory() : "null");
            exportWriter.printf("%s   <container>\n", indent);
            exportWriter.printf("%s      <availability>%s</availability>\n", indent, avail);
            exportWriter.printf("%s      <state>%s</state>\n", indent, resourceContainer.getResourceComponentState());
            exportWriter.printf("%s      <schedules>\n", indent);

            Set<MeasurementScheduleRequest> schedules = resourceContainer.getMeasurementSchedule();
            if (schedules != null) {
                for (MeasurementScheduleRequest schedule : schedules) {
                    exportWriter.printf("%s         <schedule>\n", indent);
                    exportWriter.printf("%s            <schedule-id>%d</schedule-id>\n", indent, schedule
                        .getScheduleId());
                    exportWriter.printf("%s            <name>%s</name>\n", indent, schedule.getName());
                    exportWriter.printf("%s            <enabled>%s</enabled>\n", indent, schedule.isEnabled());
                    exportWriter.printf("%s            <interval>%d</interval>\n", indent, schedule.getInterval());
                    exportWriter.printf("%s         </schedule>\n", indent);
                }
            }

            exportWriter.printf("%s      </schedules>\n", indent);
            exportWriter.printf("%s   </container>\n", indent);
            if (recurseChildren) {
                exportWriter.printf("%s   <children>\n", indent);
            }
        } else {
            Set<MeasurementScheduleRequest> schedules = resourceContainer.getMeasurementSchedule();
            int enabled = 0;
            if (schedules != null) {
                for (MeasurementScheduleRequest schedule : schedules) {
                    enabled += (schedule.isEnabled()) ? 1 : 0;
                }
            }
            exportWriter.printf("%s+ %s (sync=%s, state=%s, avail=%s, sched=%d/%d)\n", indent, resource,
                resourceContainer.getSynchronizationState(), resourceContainer.getResourceComponentState(),
                availString, enabled, (schedules != null) ? schedules.size() : 0);
        }

        if (recurseChildren) {
            Set<Resource> children = new HashSet(resource.getChildResources()); // wrap in new HashSet to avoid CCMEs
            for (Resource child : children) {
                ResourceContainer childContainer;

                if (inventoryFile == null) {
                    // get the child from the in-memory inventory
                    childContainer = inventoryManager.getResourceContainer(child);
                } else {
                    // get the child from the inventory found in the file
                    childContainer = getResourceContainer(child.getId(), inventoryFile.getResourceContainers());
                }

                // recursively call ourselves
                outputInventory(exportWriter, recurseChildren, dumpXml, childContainer, descendantDepth + 1,
                    inventoryManager, inventoryFile, resourceCounter);
            }
        }

        if (dumpXml) {
            if (recurseChildren) {
                exportWriter.printf("%s   </children>\n", indent);
            }

            exportWriter.printf("%s</resource>\n", indent);
        }

        if (descendantDepth == 0) {
            if (dumpXml) {
                exportWriter.printf("</inventory>\n");
            } else {
                exportWriter.printf("\nTotal Resources: %d (%d Platforms, %d Servers, %d Services)\n", resourceCounter
                    .getTotalCount(), resourceCounter.getPlatformCount(), resourceCounter.getServerCount(),
                    resourceCounter.getServiceCount());
            }
        }

        return;
    }

    /**
     * Finds the given resource in the map of containers - each container's resource is examined and the one whose
     * resource ID matches <code>resourceId</code> is returned.
     *
     * @param  resourceId
     * @param  containers
     *
     * @return the found container or <code>null</code> if there is none with the given resource ID
     */
    private static ResourceContainer getResourceContainer(Integer resourceId, Map<String, ResourceContainer> containers) {
        for (ResourceContainer container : containers.values()) {
            if (resourceId.equals(container.getResource().getId())) {
                return container;
            }
        }

        return null;
    }

    public static class ResourceCounter {
        private int totalCount;
        private int platformCount;
        private int serverCount;
        private int serviceCount;

        public void tallyResource(Resource resource) {
            if (resource == null) {
                return;
            }
            totalCount++;
            if (resource.getResourceType() == null) {
                return;
            }
            ResourceCategory category = resource.getResourceType().getCategory();
            switch (category) {
            case PLATFORM:
                platformCount++;
                break;
            case SERVER:
                serverCount++;
                break;
            case SERVICE:
                serviceCount++;
                break;
            }
        }

        public int getTotalCount() {
            return totalCount;
        }

        public int getPlatformCount() {
            return platformCount;
        }

        public int getServerCount() {
            return serverCount;
        }

        public int getServiceCount() {
            return serviceCount;
        }
    }
}