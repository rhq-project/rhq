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
package org.rhq.enterprise.server.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.clientapi.agent.inventory.CreateResourceResponse;
import org.rhq.core.clientapi.agent.inventory.DeleteResourceResponse;
import org.rhq.core.clientapi.server.inventory.ResourceFactoryServerService;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * JON server-side implementation of the <code>ResourceFactoryServerService</code>. This implmentation simply forwards
 * the requests to the appropriate session bean.
 *
 * @author Jason Dobies
 */
public class ResourceFactoryServerServiceImpl implements ResourceFactoryServerService {
    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(this.getClass());

    // ResourceFactoryServerService Implementation  --------------------------------------------

    public void completeCreateResource(CreateResourceResponse response) {
        log.info("Received create resource response: " + response);
        ResourceFactoryManagerLocal resourceFactoryManager = LookupUtil.getResourceFactoryManager();
        resourceFactoryManager.completeCreateResource(response);
    }

    public void completeDeleteResourceRequest(DeleteResourceResponse response) {
        log.info("Received delete resource response: " + response);
        ResourceFactoryManagerLocal resourceFactoryManager = LookupUtil.getResourceFactoryManager();
        resourceFactoryManager.completeDeleteResourceRequest(response);
    }
}