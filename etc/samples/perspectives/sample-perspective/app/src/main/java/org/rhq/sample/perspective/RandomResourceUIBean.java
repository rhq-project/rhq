/*
 * RHQ Management Platform
 * Copyright (C) 2009-2010 Red Hat, Inc.
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
package org.rhq.sample.perspective;

import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.server.perspective.AbstractPerspectiveUIBean;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;

import org.jboss.seam.annotations.Name;

/**
 * A Seam component that utilizes the RHQ remote API.
 * 
 * @author Ian Springer
 */
@Name("RandomResourceUIBean")
public class RandomResourceUIBean extends AbstractPerspectiveUIBean {
    private final Log log = LogFactory.getLog(this.getClass());

    private Resource randomResource;

    public Resource getRandomResource() throws Exception {
        if (this.randomResource == null) {
            this.randomResource = createRandomResource();
            log.debug("Retrieved random Resource " + this.randomResource);
        }
        return this.randomResource;
    }

    private Resource createRandomResource() throws Exception {
        RemoteClient remoteClient = this.perspectiveClient.getRemoteClient();
        Subject subject = this.perspectiveClient.getSubject();
        // ***NOTE***: The javassist.NotFoundException stack traces that are logged by this call can be ignored.
        ResourceManagerRemote resourceManager = remoteClient.getResourceManagerRemote();
        ResourceCriteria resourceCriteria = new ResourceCriteria();            
        PageList<Resource> allResources = resourceManager.findResourcesByCriteria(subject, resourceCriteria);
        Random randomGenerator = new Random();
        int randomIndex = randomGenerator.nextInt(allResources.size());
        return allResources.get(randomIndex);
    }    
}