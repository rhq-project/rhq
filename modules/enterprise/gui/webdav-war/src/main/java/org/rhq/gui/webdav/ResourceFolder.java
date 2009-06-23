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
package org.rhq.gui.webdav;

import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.FolderResource;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.PropPatchableResource;
import com.bradmcevoy.http.PropPatchHandler;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;

/**
 * @author Greg Hinkle
 */
public class ResourceFolder implements Resource, CollectionResource, PropFindableResource, PropPatchableResource {

    private List<Resource> children;

    private org.rhq.core.domain.resource.Resource resource;


    public ResourceFolder(org.rhq.core.domain.resource.Resource resource) {
        this.resource = resource;
    }

    public ResourceFolder(int resourceId) {
        ResourceManagerLocal rm = LookupUtil.getResourceManager();

        this.resource = rm.getResourceById(LookupUtil.getSubjectManager().getOverlord(), resourceId);

    }




  public Resource child(String s) {
        List<? extends Resource> resources = getChildren();
        for (Resource res : resources) {
            if (s.equals(res.getName())) {
                return res;
            }
        }
        return null;
    }

    public List<? extends Resource> getChildren() {
        if (children == null) {
            ResourceManagerLocal rm = LookupUtil.getResourceManager();
            List<Integer> childs = rm.getChildrenResourceIds(resource.getId(), InventoryStatus.COMMITTED);

            children = new ArrayList<Resource>();
            for (Integer childId : childs) {
                children.add(new ResourceFolder(childId));
            }

            AvailabilityResource availabilityResource = new AvailabilityResource(resource);
            children.add(availabilityResource);

            ConfigurationManagerLocal cm = LookupUtil.getConfigurationManager();
            Configuration config = cm.getActiveResourceConfiguration(resource.getId());
            if (config != null && !config.getProperties().isEmpty()) {
                children.add(new ConfigResource(resource, config));
            }


        }
        return children;
    }

    public String getUniqueId() {
        return String.valueOf(resource.getId());
    }

    public String getName() {
        return resource.getName();
    }

    public Object authenticate(String s, String s1) {
        return "auth";  
    }

    public boolean authorise(Request request, Request.Method method, Auth auth) {
        if (auth == null) {
            return false;
        }
        return true;  
    }

    public String getRealm() {
        return "rhq";  
    }

    public Date getModifiedDate() {
        return new Date(resource.getMtime());
    }

    public String checkRedirect(Request request) {
        return null;  
    }


    public Date getCreateDate() {
        return new Date(resource.getCtime());
    }

    public Long getMaxAgeSeconds(Auth auth) {
        return new Long(0);
    }

    public void setProperties(PropPatchHandler.Fields fields) {
        System.out.println("Got some fields: " + fields);
    }
}
