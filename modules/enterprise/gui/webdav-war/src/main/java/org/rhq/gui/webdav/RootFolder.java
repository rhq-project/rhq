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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This represents the top of the managed environment (i.e. all of the top-level platforms being managed).
 * 
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class RootFolder extends Authenticator implements AuthenticatedCollectionResource, PropFindableResource {

    private static final Log LOG = LogFactory.getLog(RootFolder.class);

    private List<ResourceFolder> roots;

    public RootFolder() {
        super(null);
    }

    public String getUniqueId() {
        return "/";
    }

    public String getName() {
        return "/";
    }

    public Date getModifiedDate() {
        return new Date();
    }

    public Date getCreateDate() {
        return new Date();
    }

    /**
     * This implementation of authorise simply assures that the user has authenticated himself.
     * If the user is logged in, <code>true</code> is returned; otherwise, <code>false</code> is returned.
     */
    public boolean authorise(Request request, Request.Method method, Auth auth) {
        boolean authorized = (auth != null) && (auth.getTag() != null);
        if (LOG.isDebugEnabled()) {
            LOG.debug("RootFolder auth=[" + ((auth != null) ? auth.getTag() : auth) + "], isAuthorized=" + authorized);
        }
        return authorized;
    }

    public String checkRedirect(Request request) {
        return null; // no-op
    }

    public Resource child(String childName) {
        List<? extends Resource> children = getChildren();
        for (Resource child : children) {
            if (childName.equals(child.getName())) {
                return child;
            }
        }
        return null;
    }

    public List<? extends Resource> getChildren() {
        return getChildren(getSubject());
    }

    public List<? extends Resource> getChildren(Subject subject) {
        if (this.roots == null) {
            ResourceManagerLocal rm = LookupUtil.getResourceManager();

            List<ResourceComposite> foundRoots = rm.findResourceComposites(subject, ResourceCategory.PLATFORM, null,
                null, null, true, PageControl.getUnlimitedInstance());

            if (foundRoots != null) {
                this.roots = new ArrayList<ResourceFolder>(foundRoots.size());
                for (ResourceComposite comp : foundRoots) {
                    this.roots.add(new ResourceFolder(subject, comp.getResource()));
                }
            } else {
                this.roots = new ArrayList<ResourceFolder>();
            }
        }
        return this.roots;
    }
}
