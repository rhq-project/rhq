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

import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.bradmcevoy.http.FolderResource;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.util.PageControl;

/**
 * @author Greg Hinkle
 */
public class RootFolder implements Resource, CollectionResource, PropFindableResource {

    List<ResourceFolder> roots;

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
        if (roots == null) {
            ResourceManagerLocal rm = LookupUtil.getResourceManager();

            List<ResourceComposite> foundRoots =
                    rm.findResourceComposites(
                            LookupUtil.getSubjectManager().getOverlord(),
                            ResourceCategory.PLATFORM,
                            null,
                            null,
                            null,
                            true,
                            PageControl.getUnlimitedInstance());

            roots = new ArrayList<ResourceFolder>();

            for (ResourceComposite comp : foundRoots) {
                roots.add(new ResourceFolder(comp.getResource()));
            }
        }
        return roots;
    }

    public String getUniqueId() {
        return "/";
    }

    public String getName() {
        return "/";
    }

    public Object authenticate(String s, String s1) {
        return "auth";
    }

    public boolean authorise(Request request, Request.Method method, Auth auth) {
        return true;
    }

    public String getRealm() {
        return "rhq";
    }

    public Date getModifiedDate() {
        return new Date();
    }


    public String checkRedirect(Request request) {
        return null;
    }



    public Date getCreateDate() {
        return new Date();
    }


    public Long getMaxAgeSeconds(Auth auth) {
        return new Long(0);
    }
}
