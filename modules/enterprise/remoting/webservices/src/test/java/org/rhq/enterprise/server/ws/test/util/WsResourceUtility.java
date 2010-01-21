/*
* RHQ Management Platform
* Copyright (C) 2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.ws.test.util;

import java.util.List;

import org.rhq.enterprise.server.ws.ObjectFactory;
import org.rhq.enterprise.server.ws.Resource;
import org.rhq.enterprise.server.ws.ResourceCriteria;
import org.rhq.enterprise.server.ws.ResourceType;
import org.rhq.enterprise.server.ws.ResourceTypeCriteria;
import org.rhq.enterprise.server.ws.Subject;
import org.rhq.enterprise.server.ws.WebservicesRemote;

/**
 * Utility functions for working with resources via web service calls.
 *
 * @author Jason Dobies
 */
public class WsResourceUtility {

    private WebservicesRemote service;
    private WsSubjectUtility subjectUtil;
    private ObjectFactory objectFactory;

    /**
     * Initializes a new instance to use the given connection to a running server.
     *
     * @param service must be in a state where remote calls can be made (i.e. the server is running)
     */
    public WsResourceUtility(WebservicesRemote service) {
        this.service = service;
        this.subjectUtil = new WsSubjectUtility(service);
        this.objectFactory = new ObjectFactory();
    }

    /**
     * Creates and returns a new platform in the system with a randomly generated name. The platform will be
     * of type 'Linux'.
     * <p/>
     * Resources created in this way should be removed via {@link #deleteResource(int)} at the end of the test run.
     *
     * @return WS representation of the created platform
     * @throws Exception if the platform cannot be created successfully
     * @see #deleteResource(int)
     */
    public Resource randomPlatform() throws Exception {

        long randomSeed = System.currentTimeMillis();
        Subject admin = subjectUtil.admin();

        // Use the default Linux platform
        ResourceTypeCriteria typeCriteria = objectFactory.createResourceTypeCriteria();
        typeCriteria.setFilterName("Linux");
        List<ResourceType> types = service.findResourceTypesByCriteria(admin, typeCriteria);

        if (types.size() != 1) {
            throw new RuntimeException("Unexpected number of resource types returned for " +
                "type [Linux]. Expected [1], Found [" + types.size() + "]");
        }

        // Information about the resource being created
        String name = "Test Platform " + randomSeed;

        Resource resource = objectFactory.createResource();
        resource.setName(name);
        resource.setResourceKey(name);
        resource.setDescription("WsResourceUtility Created");
        resource.setVersion("1.0");
        resource.setResourceType(types.get(0));

        // Register the platform
        service.registerPlatform(admin, resource, -1);

        // Retrieve for return
        ResourceCriteria criteria = objectFactory.createResourceCriteria();
        criteria.setFilterName(name);
        List<Resource> resources = service.findResourcesByCriteria(admin, criteria);

        if (resources.size() != 1) {
            throw new RuntimeException("Unexpected number of resources returned for name [" +
                name + "]. Expected [1], Found [" + resources.size() + "]");
        }

        return resources.get(0);
    }

    /**
     * Deletes the indicated resource from the server.
     *
     * @param resourceId must refer to an existing resource in the server
     * @throws Exception if the resource cannot be deleted
     */
    public void deleteResource(int resourceId) throws Exception {
        Subject admin = subjectUtil.admin();
        service.deleteResource(admin, resourceId);
    }
}
