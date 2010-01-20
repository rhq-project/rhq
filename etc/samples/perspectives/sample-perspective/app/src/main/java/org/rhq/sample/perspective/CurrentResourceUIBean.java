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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;
import org.rhq.enterprise.server.perspective.AbstractPerspectiveUIBean;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.web.RequestParameter;

/**
 * A Seam component that utilizes the RHQ remote API to obtain the Resource with the id specified by the 'rhqResourceId'
 * HTTP request parameter.
 *
 * @author Ian Springer
 */
@Name("CurrentResourceUIBean")
public class CurrentResourceUIBean extends AbstractPerspectiveUIBean {
    private final Log log = LogFactory.getLog(this.getClass());

    @RequestParameter("rhqResourceId")
    private Integer resourceId;

    private Resource resource;

    public CurrentResourceUIBean() {
        return;
    }

    public Resource getResource() throws Exception {
        if (this.resource == null) {
            this.resource = createResource();
            log.info("Retrieved current Resource " + this.resource);
        }
        return this.resource;
    }

    private Resource createResource() throws Exception {
        if (this.resourceId == null) {
            throw new IllegalStateException("The 'rhqResourceId' HTTP request parameter is required by this page.");
        }        
        RemoteClient remoteClient = getRemoteClient();                 
        Subject subject = getSubject();

        // ***NOTE***: The javassist.NotFoundException stack traces that are logged by this call can be ignored.
        ResourceManagerRemote resourceManager = remoteClient.getResourceManagerRemote();
        ResourceCriteria resourceCriteria = new ResourceCriteria();
        resourceCriteria.addFilterId(this.resourceId);
        PageList<Resource> resources = resourceManager.findResourcesByCriteria(subject, resourceCriteria);
        if (resources.isEmpty()) {
            throw new IllegalStateException("No Resource exists with the id " + this.resourceId + ".");
        }
        remoteClient.disconnect();
        return resources.get(0);
    }
}